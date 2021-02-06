(ns lmgrep.lucene.monitor
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [jsonista.core :as json]
            [lmgrep.lucene.text-analysis :as text-analysis])
  (:import (org.apache.lucene.monitor MonitorConfiguration Monitor MonitorQuerySerializer MonitorQuery)
           (org.apache.lucene.analysis.miscellaneous PerFieldAnalyzerWrapper)
           (org.apache.lucene.util BytesRef)
           (org.apache.lucene.search MatchAllDocsQuery)
           (java.util ArrayList)))

(def monitor-query-serializer
  (reify MonitorQuerySerializer
    (serialize [_ query]
      (BytesRef.
        (json/write-value-as-string
          {"query-id" (.getId query)
           "query"    (.getQueryString query)
           "metadata" (.getMetadata query)})))
    (deserialize [_ binary-value]
      (let [dq (json/read-value (io/reader (.bytes ^BytesRef binary-value)))]
        (MonitorQuery. (get dq "query-id")
                       (MatchAllDocsQuery.)
                       (get dq "query")
                       (get dq "metadata"))))))

(defn create [field-names-w-analyzers]
  (let [^MonitorConfiguration config (MonitorConfiguration.)
        per-field-analyzers (PerFieldAnalyzerWrapper.
                              (text-analysis/get-string-analyzer {} {}) field-names-w-analyzers)]
    (.setIndexPath config nil monitor-query-serializer)
    (Monitor. per-field-analyzers config)))

(defn defer-to-one-by-one-registration [^Monitor monitor monitor-queries]
  (doseq [mq monitor-queries]
    (try
      (.register monitor (doto (ArrayList.) (.add mq)))
      (catch Exception e
        (log/errorf "Failed to register query: '%s'" mq)
        (.printStackTrace e)))))

(defn register-queries [^Monitor monitor monitor-queries]
  (try
    (.register monitor ^Iterable monitor-queries)
    (catch Exception _
      (defer-to-one-by-one-registration monitor monitor-queries))))

(defn field-name-analyzer-mappings
  "Creates a map with field names as keys and Lucene analyzers as values.
  Both field name and analyzer are decided based on the dictionary entry configuration.
  First group dictionary entries by field name. Then from every group of dictionary entries
  take the first entry and create an analyzer based on analysis configuration."
  [dictionary default-analysis-conf]
  (->> dictionary
       (group-by (fn [dictionary-entry]
                   (text-analysis/get-field-name dictionary-entry default-analysis-conf)))
       (reduce (fn [acc [field-name dict]]
                 (assoc acc field-name (text-analysis/get-string-analyzer (first dict) default-analysis-conf)))
               {})))

(defn prepare [monitor dict-entries default-analysis-conf dictionary->monitor-queries-fn]
  (register-queries monitor (dictionary->monitor-queries-fn dict-entries default-analysis-conf)))

(defn setup
  "Setups the monitor with all the dictionary entries."
  [dictionary default-analysis-conf dict-entry->monitor-queries-fn]
  (let [mappings-from-field-names-to-analyzers (field-name-analyzer-mappings dictionary default-analysis-conf)
        monitor (create mappings-from-field-names-to-analyzers)]
    (prepare monitor dictionary default-analysis-conf dict-entry->monitor-queries-fn)
    {:monitor     monitor
     :field-names (keys mappings-from-field-names-to-analyzers)}))
