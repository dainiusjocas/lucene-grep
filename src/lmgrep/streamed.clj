(ns lmgrep.streamed
  (:require [jsonista.core :as json]
            [lmgrep.lucene :as lucene]
            [lmgrep.analysis :as analysis]
            [lmgrep.matching :as matching])
  (:import (java.io BufferedReader BufferedWriter PrintWriter)))

(defn start
  "Listens on STDIN where every line should include both query and the text."
  [options]
  (let [custom-analyzers (analysis/prepare-analyzers (get options :analyzers-file) options)
        reader-buffer-size (get options :reader-buffer-size 8192)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        ^PrintWriter reader (BufferedReader. *in* reader-buffer-size)
        writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size))
        line-nr 1
        with-empty-lines (get options :with-empty-lines)]
    (with-open [^BufferedReader rdr reader]
      (loop [^String line (.readLine rdr)]
        (when-not (nil? line)
          (let [{:keys [query text]} (json/read-value line json/keyword-keys-object-mapper)
                highlighter-fn (lucene/highlighter [{:query query}] options custom-analyzers)
                matcher-fn (matching/matcher-fn highlighter-fn nil options)]
            (if-let [out-str (matcher-fn line-nr text)]
              (.println writer out-str)
              (when with-empty-lines
                (.println writer ""))))
          (recur (.readLine rdr)))))
    (.flush writer)))
