(ns lmgrep.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [jsonista.core :as json]
            [lmgrep.cli :as cli]
            [lmgrep.lucene.analyzer :as analyzers]
            [lmgrep.grep :as grep]
            [lmgrep.only-analyze :as analyze]
            [lmgrep.predefined-analyzers :as predefined]
            [lmgrep.streamed :as streamed])
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
       (nil? (:queries-file options))
       (nil? (:queries-index-dir options))))

(def available-analysis-components
  {:analyzers     (sort (keys predefined/analyzers))
   :char-filters  (sort (keys analyzers/char-filter-name->class))
   :tokenizers    (sort (keys analyzers/tokenizer-name->class))
   :token-filters (sort (keys analyzers/token-filter-name->class))})

(defn -main [& args]
  (try
    (let [{:keys [options arguments errors summary]
           [lucene-query file-pattern & files :as positional-arguments] :arguments} (cli/handle-args args)]
      (when (seq errors)
        (println "Errors:" errors)
        (print-summary-msg summary)
        (System/exit 1))
      (when (:show-analysis-components options)
        (println
          (json/write-value-as-string
            available-analysis-components))
        (System/exit 0))
      (when (get options :streamed)
        (streamed/grep options)
        (System/exit 0))
      (if (:only-analyze options)
        (analyze/analyze-lines (first positional-arguments) (rest positional-arguments) options)
        (do
          (when (or (:help options) (zero-queries? arguments options))
            (print-summary-msg summary)
            (if-not (:help options)
              (System/exit 1)
              (System/exit 0)))
          (if-let [lucene-queries (seq (:query options))]
            (grep/grep lucene-queries (first positional-arguments) (rest positional-arguments) options)
            (if (or (:queries-file options) (:queries-index-dir options))
              (grep/grep [] (first positional-arguments) (rest positional-arguments) options)
              (grep/grep [lucene-query] file-pattern files options))))))
    (catch Exception e
      (when (System/getenv "DEBUG_MODE")
        (.printStackTrace e))
      (.println System/err (.getMessage e))
      (System/exit 1)))
  (System/exit 0))
