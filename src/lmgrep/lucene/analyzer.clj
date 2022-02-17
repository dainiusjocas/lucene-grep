(ns lmgrep.lucene.analyzer
  (:require [clojure.string :as str]
            [lmgrep.lucene.custom-analyzer :as ca])
  (:import (org.apache.lucene.analysis Analyzer)))

(set! *warn-on-reflection* true)

(defn namify
  "Normalizes analysis component name."
  [component-name]
  (str/lower-case component-name))

(def token-filter-name->class
  (reduce-kv (fn [m k v] (assoc m (namify k) v))
             {} ca/token-filter-name->class))

(def tokenizer-name->class
  (reduce-kv (fn [m k v] (assoc m (namify k) v))
             {} ca/tokenizer-name->class))

(def char-filter-name->class
  (reduce-kv (fn [m k v] (assoc m (namify k) v))
             {} ca/char-filter-name->class))

(defn get-analyzer [analyzer-name custom-analyzers]
  (or (get custom-analyzers (namify (str/replace analyzer-name "Analyzer" "")))
      (throw
        (Exception.
          (format "%s '%s' is not available. Choose one of: %s"
                  Analyzer
                  analyzer-name
                  (sort (keys custom-analyzers)))))))

(defn ^Analyzer create
  "Either fetches a predefined analyzer or creates one from the config."
  ([opts] (create opts {}))
  ([{:keys [analyzer] :as opts} custom-analyzers]
   (try
     (if-let [analyzer-name (get analyzer :name)]
       (get-analyzer analyzer-name custom-analyzers)
       (ca/create (assoc opts :namify-fn namify)
                  char-filter-name->class
                  tokenizer-name->class
                  token-filter-name->class))
     (catch Exception e
       (when (System/getenv "DEBUG_MODE")
         (.printStackTrace e))
       (throw e)))))
