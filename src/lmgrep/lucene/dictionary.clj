(ns lmgrep.lucene.dictionary
  (:require [clojure.core.reducers :as r]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.field-name :as field-name]
            [lmgrep.lucene.query-parser :as query-parser]
            [lmgrep.lucene.analysis-conf :as ac])
  (:import (org.apache.lucene.queryparser.classic ParseException)
           (org.apache.lucene.monitor MonitorQuery)
           (org.apache.lucene.search Query)
           (org.apache.lucene.analysis Analyzer)))

(defn prepare-metadata
  "Metadata must be a map String->String"
  [type meta]
  (let [str->str (if meta
                   (reduce-kv (fn [m k v] (assoc m (name k) v)) {} meta)
                   {})]
    (assoc str->str "_type" type)))

(defn query->monitor-query [questionnaire-entry field-name monitor-analyzer]
  (try
    (MonitorQuery. ^String (get questionnaire-entry :id)
                   ^Query (query-parser/construct-query questionnaire-entry field-name monitor-analyzer)
                   ^String (get questionnaire-entry :query)
                   (prepare-metadata (get questionnaire-entry :type) (get questionnaire-entry :meta)))
    (catch ParseException e
      (when (System/getenv "DEBUG_MODE")
        (.println System/err (format "Failed to parse query: '%s' with exception '%s'" questionnaire-entry e))
        (.printStackTrace e))
      (throw e))
    (catch Exception e
      (when (System/getenv "DEBUG_MODE")
        (.println System/err (format "Failed create query: '%s' with '%s'" questionnaire-entry e))
        (.printStackTrace e))
      (throw e))))

(defrecord Dict [field-name monitor-analyzer monitor-query])

(def ^Analyzer get-string-analyzer
  (memoize analyzer/create))

(def ^String get-field-name
  (memoize field-name/construct))

(defn ensure-type [questionnaire-entry default-type]
  (update questionnaire-entry :type (fn [type] (if type type default-type))))

(defn prepare-query-entry
  [questionnaire-entry default-type global-analysis-conf]
  (let [analysis-conf (ac/prepare-analysis-configuration global-analysis-conf questionnaire-entry)
        field-name (get-field-name analysis-conf)
        monitor-analyzer (get-string-analyzer analysis-conf)]
    (Dict.
      field-name
      monitor-analyzer
      (query->monitor-query (ensure-type questionnaire-entry default-type) field-name monitor-analyzer))))

(defn indexed
  "Iterate over all entries and add sequence number as ID if ID is missing."
  [questionnaire]
  (loop [index 0
         entries questionnaire
         acc []]
    (if entries
      (recur (inc index)
             (next entries)
             (conj acc (update (nth entries 0) :id #(or % (str index)))))
      acc)))

(defn normalize
  "With global analysis configuration for each query:
  - add ID if missing;
  - construct field name;
  - construct analyzer;
  - construct Lucene MonitorQuery object."
  [questionnaire default-type options]
  (let [global-analysis-conf (ac/prepare-analysis-configuration ac/default-text-analysis options)]
    (->> questionnaire
         indexed
         (r/map (fn [questionnaire-entry]
                  (prepare-query-entry questionnaire-entry default-type global-analysis-conf)))
         (r/foldcat))))

(defn get-monitor-queries
  "Returns a vector of MonitorQuery"
  [questionnaire]
  (->> questionnaire
       (r/map :monitor-query)
       (r/remove nil?)
       (into [])))
