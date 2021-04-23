(ns lmgrep.lucene.dictionary
  (:require [clojure.core.reducers :as r]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.field-name :as field-name]
            [lmgrep.lucene.analysis-conf :as ac])
  (:import (org.apache.lucene.queryparser.classic QueryParser ParseException)
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

(defn query->monitor-query [dict-entry field-name monitor-analyzer]
  (try
    (MonitorQuery. ^String (get dict-entry :id)
                   ^Query (.parse (QueryParser. ^String field-name ^Analyzer monitor-analyzer)
                                  ^String (get dict-entry :query))
                   ^String (get dict-entry :query)
                   (prepare-metadata (get dict-entry :type) (get dict-entry :meta)))
    (catch ParseException e
      (.println System/err (format "Failed to parse query: '%s' with exception '%s'" dict-entry e)))
    (catch Exception e
      (.println System/err (format "Failed create query: '%s' with '%s'" dict-entry e)))))

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
         (r/map (fn [dictionary-entry]
                  (prepare-query-entry dictionary-entry default-type global-analysis-conf)))
         (r/foldcat))))

(defn get-monitor-queries
  "Returns a vector of MonitorQuery"
  [questionnaire]
  (->> questionnaire
       (r/map :monitor-query)
       (r/remove nil?)
       (into [])))
