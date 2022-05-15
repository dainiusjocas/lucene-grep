(ns lmgrep.no-split
  (:require [lmgrep.matching :as matching])
  (:import (java.nio.file Files Path)
           (java.io File BufferedWriter PrintWriter)))

(defn grep
  [file-paths-to-analyze highlighter-fn options]
  (when (empty? file-paths-to-analyze)
    (.println System/err "Standard input is not supported!"))
  (let [with-empty-lines (get options :with-empty-lines)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size)
                                          ^Boolean (empty? file-paths-to-analyze))]
    (doseq [^String path file-paths-to-analyze]
      (let [^String content (Files/readString ^Path (.toPath (File. path)))
            matcher-fn (matching/matcher-fn highlighter-fn path options)
            out-str (matcher-fn 1 content)]
        (if out-str
          (.println writer out-str)
          (when with-empty-lines
            (.println writer)))))
    (.flush writer)))

(comment
  (require '[lmgrep.lucene :as lucene])

  (lmgrep.no-split/grep ["test/resources/test.txt"]
                        (lucene/highlighter [{:query "dog"}] {} {})
                        {:format :json}))
