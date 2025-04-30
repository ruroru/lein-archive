(ns leiningen.archive.tgz
  (:require [clojure.string :as str])
  (:import (java.io BufferedOutputStream File FileInputStream FileOutputStream)
           (org.apache.commons.compress.archivers.tar TarArchiveEntry TarArchiveOutputStream)
           (org.apache.commons.compress.compressors.gzip GzipCompressorOutputStream GzipParameters)))


(defn- create-entry-path [^String target-path ^String entry-name]
  (-> (if (.endsWith target-path entry-name)
        target-path
        (if (.endsWith target-path "/")
          (str target-path entry-name)
          (str target-path "/" entry-name)))
      (str/replace #"^/" "")))


(defn pack [source-files ^String archive-file-path]

  (with-open [file-output-stream (FileOutputStream. ^File (File. archive-file-path))]
    (let [gzip-params (GzipParameters.)]
      (.setCompressionLevel gzip-params 9)

      (with-open [gzip-output-stream (GzipCompressorOutputStream. file-output-stream gzip-params)]
        (with-open [output-stream (TarArchiveOutputStream. (BufferedOutputStream. gzip-output-stream))]
          (.setLongFileMode output-stream TarArchiveOutputStream/LONGFILE_GNU)
          (doseq [[^File input-file ^String output-directory] source-files]
            (let [entry-name (create-entry-path output-directory (.getName ^File input-file))
                  file-size (.length input-file)
                  tar-entry (TarArchiveEntry. ^String entry-name)]

              (.setSize tar-entry file-size)
              (.putArchiveEntry output-stream tar-entry)
              (with-open [file-input-stream (FileInputStream. ^File input-file)]
                (.transferTo file-input-stream output-stream))
              (.closeArchiveEntry output-stream))))))))