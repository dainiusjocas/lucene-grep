(ns lmgrep.lucene.dictionary
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]
            [lmgrep.lucene.text-analysis :as text-analysis])
  (:import (org.apache.lucene.queryparser.classic QueryParser ParseException)
           (org.apache.lucene.monitor MonitorQuery)))

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
