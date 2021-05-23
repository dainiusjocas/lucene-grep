(ns lmgrep.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [lmgrep.cli :as cli]
            [lmgrep.grep :as grep])
  (:gen-class))

(def version (str/trim (slurp (io/resource "LMGREP_VERSION"))))

(defn print-summary-msg [summary]
  (println (format "Lucene-grep %s" version))
  (println "Usage: lmgrep [OPTIONS] LUCENE_QUERY [FILES]")
  (println "Supported options:")
  (println summary))

(defn zero-queries? [arguments options]
  (and (zero? (count arguments))
       (empty? (:query options))
       (nil? (:queries-file options))))

(defn -main [& args]
  (try
    (let [{:keys [options arguments errors summary]
           [lucene-query file-pattern & files :as positional-arguments] :arguments} (cli/handle-args args)]
      (when (seq errors)
        (println "Errors:" errors)
        (print-summary-msg summary)
        (System/exit 1))
      (if (:only-analyze options)
        (grep/analyze-lines (first positional-arguments) (rest positional-arguments) options)
        (do
          (when (or (:help options) (zero-queries? arguments options))
            (print-summary-msg summary)
            (if-not (:help options)
              (System/exit 1)
              (System/exit 0)))
          (if-let [lucene-queries (seq (:query options))]
            (grep/grep lucene-queries (first positional-arguments) (rest positional-arguments) options)
            (if (:queries-file options)
              (grep/grep [] (first positional-arguments) (rest positional-arguments) options)
              (grep/grep [lucene-query] file-pattern files options))))))
    (catch Exception e
      (when (System/getenv "DEBUG_MODE")
        (.printStackTrace e))
      (.println System/err (.getMessage e))
      (System/exit 1)))
  (System/exit 0))
