(ns leiningen.archive.zip
  (:require [clojure.string :as str])
  (:import (java.io File FileInputStream FileOutputStream)
           (java.util.zip ZipEntry ZipOutputStream)))

(defn- create-zip-entry-path [^String target-path ^String entry-name]
  (-> (if (.endsWith target-path entry-name)
        target-path
        (if (.endsWith target-path "/")
          (str target-path entry-name)
          (str target-path "/" entry-name)))
      (str/replace #"^/" "")))

(defn pack [source-files ^String archive-file-path]
  (with-open [zip-output-stream (ZipOutputStream. (FileOutputStream. ^File (File. archive-file-path)))]
    (doseq [[input-file output-directory] source-files]
      (let [zip-entry-path (create-zip-entry-path output-directory (.getName ^File input-file))
            zip-entry ^ZipEntry (ZipEntry. ^String zip-entry-path)]
        (.setMethod zip-entry 8)

        (.putNextEntry zip-output-stream zip-entry)
        (with-open [file-input-stream (FileInputStream. ^File input-file)]
          (.transferTo file-input-stream zip-output-stream))))))