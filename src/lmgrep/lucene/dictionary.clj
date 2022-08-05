(ns lmgrep.lucene.dictionary
  (:require [jsonista.core :as json]
            [lucene.custom.query :as q]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.field-name :as field-name])
  (:import (org.apache.lucene.queryparser.classic ParseException)
           (org.apache.lucene.monitor MonitorQuery)
           (org.apache.lucene.search Query)
           (org.apache.lucene.analysis Analyzer)
           (clojure.lang PersistentArrayMap)))

(defrecord Dict [field-name monitor-query analysis-conf])

(def ^Analyzer get-string-analyzer
  (memoize analyzer/create))

(def ^String get-field-name
  (memoize field-name/construct))

(defn prepare-meta
  "Metadata must be a map String->String"
  [meta]
  (if meta
    (reduce-kv (fn [m k v] (assoc m (name k) v)) {} meta)
    {}))

(def CONF_KEY "__MQ_CONF")
(def DEFAULT_FIELD_NAME_KEY "__DEFAULT_FIELD_NAME")

(defn monitor-query-constructor
  "Returns a function that can create a MonitorQuery object"
  [custom-analyzers]
  (fn monitor-query ^MonitorQuery
    [{:keys [id query meta default-field-name query-parser-name query-parser-conf analyzer] :as mq}]
    (try
      (let [monitor-analyzer (get-string-analyzer analyzer custom-analyzers)]
        (MonitorQuery. ^String id
                       ^Query (q/parse query query-parser-name query-parser-conf default-field-name monitor-analyzer)
                       ^String query
                       (assoc (prepare-meta meta)
                         CONF_KEY (json/write-value-as-string mq)
                         DEFAULT_FIELD_NAME_KEY default-field-name)))
      (catch ParseException e
        (when (System/getenv "DEBUG_MODE")
          (.println System/err (format "Failed to parse query: '%s' with exception '%s'" mq e))
          (.printStackTrace e))
        (throw e))
      (catch Exception e
        (when (System/getenv "DEBUG_MODE")
          (.println System/err (format "Failed create query: '%s' with '%s'" mq e))
          (.printStackTrace e))
        (throw e)))))

(defn in-memory-query-constructor [custom-analyzers]
  (fn monitor-query
    [{:keys [id query meta default-field-name query-parser-name query-parser-conf analyzer] :as mq}]
    (try
      (let [monitor-analyzer (get-string-analyzer analyzer custom-analyzers)]
        (MonitorQuery. ^String id
                       ^Query (q/parse query query-parser-name query-parser-conf default-field-name monitor-analyzer)
                       ^String query
                       (prepare-meta meta)))
      (catch ParseException e
        (when (System/getenv "DEBUG_MODE")
          (.println System/err (format "Failed to parse query: '%s' with exception '%s'" mq e))
          (.printStackTrace e))
        (throw e))
      (catch Exception e
        (when (System/getenv "DEBUG_MODE")
          (.println System/err (format "Failed create query: '%s' with '%s'" mq e))
          (.printStackTrace e))
        (throw e)))))

(defn stable-id [m]
  (str (Math/abs ^int (.hashCode ^PersistentArrayMap m))))

(defn prepare-query-entry
  [questionnaire-entry default-type global-analysis-conf monitor-query-constructor-fn]
  (let [id (or (:id questionnaire-entry) (stable-id questionnaire-entry))
        analysis-conf (if (empty? (get questionnaire-entry :analysis))
                        global-analysis-conf
                        (assoc (get questionnaire-entry :analysis)
                          :config-dir (get global-analysis-conf :config-dir)))
        ; parameter for the query parser
        default-field-name (get-field-name analysis-conf)
        monitor-query (monitor-query-constructor-fn
                        {:id                 id
                         :query              (get questionnaire-entry :query)
                         :meta               (assoc (get questionnaire-entry :meta) "_type" default-type)
                         :default-field-name default-field-name
                         :query-parser-name  (keyword (get questionnaire-entry :query-parser))
                         :query-parser-conf  (get questionnaire-entry :query-parser-conf)
                         :analyzer           analysis-conf})]
    (Dict.
      default-field-name
      monitor-query
      analysis-conf)))

(defn handle-query-parser-settings [questionnaire-entry options]
  (if (get questionnaire-entry :query-parser)
    questionnaire-entry
    (if (get questionnaire-entry :query-parser-conf)
      (assoc questionnaire-entry
        :query-parser (get options :query-parser))
      (assoc questionnaire-entry
        :query-parser (get options :query-parser)
        :query-parser-conf (get options :query-parser-conf)))))

(defn normalize
  "With global analysis configuration for each query:
  - add ID if missing;
  - construct field name;
  - construct analyzer;
  - construct Lucene MonitorQuery object."
  [questionnaire default-type options custom-analyzers]
  (let [global-analysis-conf (assoc (get options :analysis)
                               :config-dir (get options :config-dir))
        monitor-query-constructor-fn (if (:queries-index-dir options)
                                       (monitor-query-constructor custom-analyzers)
                                       (in-memory-query-constructor custom-analyzers))]
    (mapv (fn [questionnaire-entry]
            (prepare-query-entry (handle-query-parser-settings questionnaire-entry options)
                                 default-type
                                 global-analysis-conf
                                 monitor-query-constructor-fn)) questionnaire)))

(defn get-monitor-queries
  "Returns a vector of MonitorQuery"
  [questionnaire] (mapv :monitor-query questionnaire))
