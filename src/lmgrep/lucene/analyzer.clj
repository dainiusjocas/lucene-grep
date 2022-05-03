(ns lmgrep.lucene.analyzer
  (:require [clojure.string :as str]
            [lucene.custom.analyzer :as ca])
  (:import (org.apache.lucene.analysis Analyzer)))

(set! *warn-on-reflection* true)

(defn namify
  "Normalizes analysis component name."
  [component-name]
  (str/lower-case component-name))

(def token-filter-name->class
  (reduce-kv (fn [m k v] (assoc m (namify k) v))
             {} (ca/token-filter-factories)))

(def tokenizer-name->class
  (reduce-kv (fn [m k v] (assoc m (namify k) v))
             {} (ca/tokenizer-factories)))

(def char-filter-name->class
  (reduce-kv (fn [m k v] (assoc m (namify k) v))
             {} (ca/char-filter-factories)))

(defn get-analyzer [analyzer-name custom-analyzers]
  (or (get custom-analyzers (namify (str/replace analyzer-name "Analyzer" "")))
      (throw
        (Exception.
          (format "%s '%s' is not available. Choose one of: %s"
                  Analyzer
                  analyzer-name
                  (sort (keys custom-analyzers)))))))


(defn custom-analyzer->short-notation [conf]
  (let [old->new (fn [old] {(get old :name) (get old :args)})
        converted-tokenizer {(get-in conf [:tokenizer :name]) (get-in conf [:tokenizer :args])}
        converted-char-filters (mapv old->new (get conf :char-filters))
        converted-token-filters (mapv old->new (get conf :token-filters))]
    (assoc conf :tokenizer converted-tokenizer
                :char-filters converted-char-filters
                :token-filters converted-token-filters)))

(defn create
  "Either fetches a predefined analyzer or creates one from the config."
  (^Analyzer [opts] (create opts {}))
  (^Analyzer [{:keys [analyzer] :as opts} custom-analyzers]
   (try
     (if-let [analyzer-name (get analyzer :name)]
       (get-analyzer analyzer-name custom-analyzers)
       (ca/create (custom-analyzer->short-notation (assoc opts :namify-fn namify))
                  char-filter-name->class
                  tokenizer-name->class
                  token-filter-name->class))
     (catch Exception e
       (when (System/getenv "DEBUG_MODE")
         (.printStackTrace e))
       (throw e)))))
