(ns leiningen.archive-test
  (:require [babashka.fs :as fs]
            [clojure.test :refer [deftest is use-fixtures]]
            [leiningen.archive :as archive])
  (:import (java.io BufferedOutputStream File FileInputStream FileOutputStream)
           (java.nio.file FileVisitResult Files LinkOption Paths SimpleFileVisitor)
           (java.util.zip GZIPInputStream)
           (net.lingala.zip4j ZipFile)
           (org.apache.commons.compress.archivers.tar TarArchiveInputStream)))

(def extract-dir "./target/extracted")

(defn- delete-recursively [directory]
  (let [path (Paths/get ^String directory (into-array String []))]
    (Files/walkFileTree
      path
      (proxy [SimpleFileVisitor] []
        (visitFile [file _]
          (Files/delete file)
          FileVisitResult/CONTINUE)
        (postVisitDirectory [dir _]
          (Files/delete dir)
          FileVisitResult/CONTINUE)))))

(defn extract-tar-gz
  [^String source-file ^String target-dir]
  (with-open [fis  (FileInputStream.  source-file)
              gis  (GZIPInputStream. fis)
              tis  (TarArchiveInputStream. gis)]
    (loop []
      (when-let [entry (.getNextEntry tis)]
        (let [out-file (File. target-dir (.getName entry))]
          (when (.isDirectory entry)
            (.mkdirs out-file))
          (when-not (.isDirectory entry)
            (doto (.getParentFile out-file)
              (.mkdirs))
            (with-open [fos (FileOutputStream. out-file)
                        bos (BufferedOutputStream. fos 4096)]
              (let [buffer (byte-array 4096)]
                (loop []
                  (let [read-bytes (.read tis buffer)]
                    (when (pos? read-bytes)
                      (.write bos buffer 0 read-bytes)
                      (recur))))))))
        (recur)))))

(use-fixtures :each (fn [f]
                      (when (fs/exists? extract-dir)
                        (delete-recursively extract-dir))

                      (f)))

(defn verify-files-exist [file-paths]
  (doseq [file file-paths]
    (let [file-path (Paths/get ^String file (into-array String []))]
      (is (Files/exists file-path (into-array LinkOption []))))))

(deftest test-zipping-files-in-current-dir
  (let [project {:name    "project" :version "1.2.3"
                 :archive {:file-name "target/target2/target3/archive.zip"
                           :file-set  [{:source-path "test/resources/test/file-*.jar" :output-path "/jar-files/"}
                                       {:source-path "test/resources/test/file.txt" :output-path "/text-files"}]}}]
    (archive/archive project)
    (let [zip-file ^ZipFile (ZipFile. "./target/target2/target3/archive.zip")]
      (.extractAll zip-file extract-dir)))
  (verify-files-exist (list "./target/extracted/jar-files/file-version.jar"
                            "./target/extracted/jar-files/file-version2.jar"
                            "./target/extracted//text-files/file.txt")))


(deftest not-providing-file-name-defaults-to-using-project-name-and-version
  (let [project {:name    "project" :version "1.2.3"
                 :archive {:file-set [{:source-path "test/resources/test/file-*.jar" :output-path "/jar-files/"}
                                      {:source-path "test/resources/test/file.txt" :output-path "/text-files"}]}}]
    (archive/archive project)
    (let [zip-file ^ZipFile (ZipFile. "./target/project-1.2.3.zip")]
      (.extractAll zip-file extract-dir)))
  (verify-files-exist (list "./target/extracted/jar-files/file-version.jar"
                            "./target/extracted/jar-files/file-version2.jar"
                            "./target/extracted//text-files/file.txt")))


(deftest test-targz-files-in-current-dir
  (let [project {:name    "project" :version "1.2.3"
                 :archive {:format :tgz
                           :file-name "target/archive.tgz"
                           :file-set  [{:source-path "test/resources/test/file-*.jar" :output-path "/jar-files/"}
                                       {:source-path "test/resources/test/file.txt" :output-path "/text-files"}]}}]
    (archive/archive project))
  (extract-tar-gz "target/archive.tgz" extract-dir)
  (verify-files-exist (list "./target/extracted/jar-files/file-version.jar"
                            "./target/extracted/jar-files/file-version2.jar"
                            "./target/extracted//text-files/file.txt")))


(deftest compress-tgz-without-file-name
  (let [project {:name    "project" :version "1.2.3"
                 :archive {:format :tgz

                           :file-set  [{:source-path "test/resources/test/file-*.jar" :output-path "/jar-files/"}
                                       {:source-path "test/resources/test/file.txt" :output-path "/text-files"}]}}]
    (archive/archive project))
  (extract-tar-gz "./target/project-1.2.3.tgz" extract-dir)
  (verify-files-exist (list "./target/extracted/jar-files/file-version.jar"
                            "./target/extracted/jar-files/file-version2.jar"
                            "./target/extracted//text-files/file.txt")))