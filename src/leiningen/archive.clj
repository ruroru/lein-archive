(ns leiningen.archive
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [leiningen.archive.tgz :as tgz]
    [leiningen.archive.zip :as zip]
    )
  (:import (java.io File)))


(defn- force-create-dir [path]
  (let [dir (io/file path)] (.mkdirs dir)))



(defn- get-file-info [source-path output-dir]
  (if (str/includes? source-path "*")
    (let [parent-dir (first (str/split source-path #"/(?!.*/)"))
          name-pattern (second (str/split source-path #"/(?!.*/)"))
          pattern (re-pattern (str/replace name-pattern "*" ".*"))]
      (map (fn [i] (list i output-dir))
           (filter (fn [f]
                     (let [file-name (.getName ^File f)]
                       (re-matches pattern (str file-name))))
                   (.listFiles (File. ^String parent-dir)))))
    (list (list (File. ^String source-path) output-dir))))


(defn archive
  [project & _]
  (let [files (apply concat (map
                              (fn [file-info] (get-file-info (:source-path file-info) (:output-path file-info)))
                              (:file-set (:archive project))))
        extension (case (:format (:archive project))
                    :tgz "tgz"
                    "zip")
        archive-path (str (System/getProperty "user.dir") "/" (:file-name (:archive project)
                                                                (format "target/%s-%s.%s" (:name project) (:version project) extension)))
        archive-parent (first (str/split archive-path #"/(?!.*/)"))]
    (force-create-dir archive-parent)
    (case (:format (:archive project))
      :tgz (tgz/pack files archive-path)
      (zip/pack files archive-path))
    (println (format "Created %s" (.getCanonicalPath (File. archive-path))))))

