(ns lmgrep.core
  (:require [lmgrep.cli :as cli]
            [lmgrep.grep :as grep])
  (:gen-class))

(defn print-summary-msg [summary]
  (println "Lucene Monitor based grep-like utility.")
  (println "Usage: lmgrep [OPTIONS] LUCENE_QUERY [FILES]")
  (println "Supported options:")
  (println summary))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]
         [lucene-query file-pattern & files] :arguments} (cli/handle-args args)]
    (when (seq errors)
      (println "Errors:" errors)
      (print-summary-msg summary)
      (System/exit 1))
    (when (or (:help options) (zero? (count arguments)))
      (print-summary-msg summary)
      (when-not (:help options)
        (System/exit 1)))
    (grep/grep lucene-query file-pattern files options))
  (System/exit 0))
