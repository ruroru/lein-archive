(ns leiningen.archive-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [leiningen.archive :as archive])
  (:import (java.io BufferedOutputStream File FileInputStream FileOutputStream InputStream)
           (java.nio.file FileVisitResult Files LinkOption Paths SimpleFileVisitor)
           (java.util.zip GZIPInputStream)
           (org.apache.commons.compress.archivers.tar TarArchiveInputStream)
           (org.apache.commons.compress.archivers.zip ZipArchiveInputStream)))

(def ^:private extract-dir "./target/extracted")

(defn- exists?
  [path]
  (-> (File. ^String path)
      .exists))

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
  (with-open [fis (FileInputStream. source-file)
              gis (GZIPInputStream. fis)
              tis (TarArchiveInputStream. gis)]
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
                      (when (exists? extract-dir)
                        (delete-recursively extract-dir))

                      (f)))

(defn verify-files-exist [file-paths]
  (doseq [file file-paths]
    (let [file-path (Paths/get ^String file (into-array String []))]
      (is (Files/exists file-path (into-array LinkOption []))))))

(deftest test-zipping-files-in-current-dir
  (let [project {:name    "project" :version "1.2.3"
                 :archive {:file-name "target/target2/target3/archive.zip"
                           :file-set  [{:source-path "test/resources/test/file-*.jar" :output-path "/"}
                                       {:source-path "test/resources/test/file.txt" :output-path "/"}]}}]
    (archive/archive project)
    (let [zip-path "./target/target2/target3/archive.zip"]
      (with-open [fis (FileInputStream. zip-path)
                  zis (ZipArchiveInputStream. fis)]
        (loop [entry (.getNextZipEntry zis)]
          (when entry
            (let [entry-file (File. ^String extract-dir (.getName entry))]
              (if (.isDirectory entry)
                (.mkdirs entry-file)
                (do
                  (.mkdirs (.getParentFile entry-file))
                  (with-open [fos (FileOutputStream. entry-file)]
                    (.transferTo ^InputStream zis fos))))
              (recur (.getNextZipEntry zis)))))))
    )
  (verify-files-exist (list "./target/extracted/file-version.jar"
                            "./target/extracted/file-version2.jar"
                            "./target/extracted//file.txt")))


(deftest not-providing-file-name-defaults-to-using-project-name-and-version
  (let [project {:name    "project" :version "1.2.3"
                 :archive {:file-set [{:source-path "test/resources/test/file-*.jar" :output-path "/jar-files/"}
                                      {:source-path "test/resources/test/file.txt" :output-path "/text-files"}]}}]
    (archive/archive project)
    (let [zip-path "./target/project-1.2.3.zip"]
      (with-open [fis (FileInputStream. zip-path)
                  zis (ZipArchiveInputStream. fis)]
        (loop [entry (.getNextZipEntry zis)]
          (when entry
            (let [entry-file (File. ^String extract-dir (.getName entry))]
              (if (.isDirectory entry)
                (.mkdirs entry-file)
                (do
                  (.mkdirs (.getParentFile entry-file))
                  (with-open [fos (FileOutputStream. entry-file)]
                    (.transferTo ^InputStream zis fos))))
              (recur (.getNextZipEntry zis))))))))
  (verify-files-exist (list "./target/extracted/jar-files/file-version.jar"
                            "./target/extracted/jar-files/file-version2.jar"
                            "./target/extracted//text-files/file.txt")))


(deftest test-targz-files-in-current-dir
  (let [project {:name    "project" :version "1.2.3"
                 :archive {:format    :tgz
                           :file-name "target/archive.tar.gz"
                           :file-set  [{:source-path "test/resources/test/file-*.jar" :output-path "/jar-files/"}
                                       {:source-path "test/resources/test/file.txt" :output-path "/text-files"}]}}]
    (archive/archive project))
  (extract-tar-gz "target/archive.tar.gz" extract-dir)
  (verify-files-exist (list "./target/extracted/jar-files/file-version.jar"
                            "./target/extracted/jar-files/file-version2.jar"
                            "./target/extracted//text-files/file.txt")))


(deftest compress-tgz-without-file-name
  (let [project {:name    "project" :version "1.2.4"
                 :archive {:format   :tgz

                           :file-set [{:source-path "test/resources/test/file-*.jar" :output-path "/jar-files/"}
                                      {:source-path "test/resources/test/file.txt" :output-path "/text-files"}]}}]

    (archive/archive project))
  (extract-tar-gz "./target/project-1.2.4.tar.gz" extract-dir)
  (verify-files-exist (list "./target/extracted/jar-files/file-version.jar"
                            "./target/extracted/jar-files/file-version2.jar"
                            "./target/extracted//text-files/file.txt")))

(deftest allow-adding-directories
  (let [project {:name    "project" :version "1.2.6"
                 :archive {:format   :tgz
                           :file-set [{:source-path "./test/resources/test/folder2" :output-path "/path"}]}}]
    (archive/archive project)
    (extract-tar-gz "./target/project-1.2.6.tar.gz" extract-dir)
    (verify-files-exist (list "./target/extracted/path/folder2/folder/file.txt"))))

(deftest allow-adding-directories-with-trailing-slash
  (let [project {:name    "project" :version "1.2.5"
                 :archive {:format   :tgz
                           :file-set [{:source-path "test/resources/test/folder2" :output-path "/path/"}]}}]
    (archive/archive project)
    (extract-tar-gz "./target/project-1.2.5.tar.gz" extract-dir)
    (verify-files-exist (list "./target/extracted/path/folder2/folder/file.txt"))))


(deftest adding-non-existing-file-does-not-crash
  (let [project {:name    "project" :version "1.2.7"
                 :archive {:format   :tgz
                           :file-set [{:source-path "test/resources/test/this-does-not-exist.txt" :output-path "/"}

                                      {:source-path "test/resources/test/folder2" :output-path "/"}]}}]
    (archive/archive project)
    (extract-tar-gz "./target/project-1.2.7.tar.gz" extract-dir)
    (verify-files-exist (list "./target/extracted/folder2/folder/file.txt"))))