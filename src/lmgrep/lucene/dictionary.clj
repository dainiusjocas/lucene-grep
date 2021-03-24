(ns lmgrep.lucene.dictionary
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [jsonista.core :as json]
            [lmgrep.lucene.text-analysis :as text-analysis])
  (:import (org.apache.lucene.queryparser.classic QueryParser ParseException)
           (org.apache.lucene.monitor MonitorQuery)
           (java.io File)))

(defn prepare-metadata [type meta]
  (reduce-kv (fn [m k v] (assoc m (name k) v)) {} (if type (assoc meta :_type type) meta)))

(defn dict-entry->monitor-queries [{:keys [id text meta type] :as dict-entry} default-analysis-conf idx]
  (try
    (let [query-id (or id (str idx))
          metadata (prepare-metadata type meta)
          field-name (text-analysis/get-field-name dict-entry default-analysis-conf)
          monitor-analyzer (text-analysis/get-string-analyzer dict-entry default-analysis-conf)]
      (MonitorQuery. query-id
                     (.parse (QueryParser. field-name monitor-analyzer) text)
                     text
                     metadata))
    (catch ParseException e
      (log/errorf "Failed to parse query: '%s' with exception '%s'" dict-entry e))
    (catch Exception e (log/errorf "Failed create query: '%s' with '%s'" dict-entry e))))

(defn dictionary->monitor-queries [dictionary default-analysis-conf]
  (remove nil?
          (map (fn [dict-entry idx]
                 (dict-entry->monitor-queries dict-entry default-analysis-conf idx))
               dictionary (range))))

(defn read-dictionary-from-file [file-path]
  (let [^File input-file (io/file file-path)]
    (if (.isFile input-file)
      (json/read-value (slurp input-file) json/keyword-keys-object-mapper)
      (throw (Exception. (format "File '%s' doesn't exist." file-path))))))

(def default-text-analysis
  {:case-sensitive?             false
   :ascii-fold?                 true
   :stem?                       true
   :tokenizer                   :standard
   :stemmer                     :english
   :word-delimiter-graph-filter 0})

(defn normalize-dictionary-entry [dictionary-entry]
  (-> dictionary-entry
      (assoc :text (:query dictionary-entry))
      (update :stemmer keyword)
      (update :tokenizer keyword)
      (dissoc :query)))

(defn prepare-dictionary-entry [dictionary-entry]
  (reduce (fn [acc k]
            (assoc acc k (text-analysis/three-way-merge k {} default-text-analysis acc)))
          (normalize-dictionary-entry dictionary-entry)
          (keys default-text-analysis)))

(defn prepare-dictionary [lucene-query-strings options]
  (let [analysis-options (merge default-text-analysis options)]
    (concat
      (map (fn [lucene-query-string]
             (assoc analysis-options :text lucene-query-string))
           lucene-query-strings)
      (when-let [queries-file-path (:queries-file options)]
        (map prepare-dictionary-entry (read-dictionary-from-file queries-file-path))))))
