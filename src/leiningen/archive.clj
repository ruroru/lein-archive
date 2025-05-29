(ns leiningen.archive
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [leiningen.archive.tgz :as tgz]
    [leiningen.archive.zip :as zip])
  (:import (java.io File)))


(defn- force-create-dir [path]
  (let [dir (io/file path)] (.mkdirs dir)))


(defn- list-relative-files
  [dir-path expected-path]
  (let [base-dir (io/file dir-path)]
    (when (.isDirectory base-dir)
      (->> (file-seq base-dir)
           (filter #(.isFile ^File %))
           (map (fn [file]
                  (let [full-path (-> (.getCanonicalFile ^File file)
                                      .toPath
                                      .normalize)
                        input-dir-path (-> (File. ^String dir-path)
                                           .getParentFile
                                           .getCanonicalFile
                                           .toPath
                                           .normalize)
                        relativized-path (-> input-dir-path
                                             (.relativize full-path)
                                             .normalize)]

                    (list file
                          (let [
                                transformed-relativized-str (-> relativized-path
                                                                .toString
                                                                (.replace File/separator "/"))]
                            (str expected-path
                                 (if (.endsWith ^String expected-path File/separator)
                                   ""
                                   "/")
                                 transformed-relativized-str))))))))))


(defn- handle-wildcard [source-path output-dir]
  (let [source-file (File. ^String source-path)
        parent-file (-> source-file
                        .getParentFile)
        pattern (re-pattern (str/replace (.getName ^File source-file) "*" ".*"))]
    (map (fn [i] (list i output-dir))
         (filter (fn [f]
                   (let [file-name (.getName ^File f)]
                     (re-matches pattern (str file-name))))
                 (.listFiles parent-file)))))


(defn- get-file-info [{:keys [source-path output-path]}]
  (if (str/includes? source-path "*")
    (handle-wildcard source-path output-path)
    (let [file (File. ^String source-path)]
      (case [(.exists ^File file) (.isDirectory ^File file)]
        [true true] (list-relative-files source-path output-path)
        [true false] (list (list (File. ^String source-path) output-path))
        (do
          (println (format "%s does not exist, it will not be added." source-path))
          (list))))))


(defn archive
  [project & _]
  (let [files (apply concat (map get-file-info (:file-set (:archive project))))
        extension (case (:format (:archive project))
                    :tgz "tar.gz"
                    "zip")
        archive-path (str (System/getProperty "user.dir") "/" (:file-name (:archive project)
                                                                (format "target/%s-%s.%s" (:name project) (:version project) extension)))
        archive-parent (-> (File. archive-path)
                           .getParentFile
                           .getCanonicalPath)]

    (force-create-dir archive-parent)
    (case (:format (:archive project))
      :tgz (tgz/pack files archive-path)
      (zip/pack files archive-path))
    (println (format "Created %s" (.getCanonicalPath (File. archive-path))))))
