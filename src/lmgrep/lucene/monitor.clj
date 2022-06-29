(ns lmgrep.lucene.monitor
  (:require [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.dictionary :as dictionary]
            [lucene.custom.query :as query]
            [jsonista.core :as json]
            [clojure.java.io :as io])
  (:import (org.apache.lucene.monitor MonitorConfiguration Monitor MonitorQuerySerializer Presearcher TermFilteredPresearcher MultipassTermFilteredPresearcher)
           (org.apache.lucene.analysis.miscellaneous PerFieldAnalyzerWrapper)
           (java.util ArrayList)
           (clojure.lang Indexed)
           (java.util.function Function)
           (java.nio.file Path)
           (org.apache.lucene.util BytesRef IOSupplier)
           (org.apache.lucene.store ByteBuffersDirectory)))

(set! *warn-on-reflection* true)

(def monitor-query-serializer
  (MonitorQuerySerializer/fromParser
    (reify Function
      (apply [_ str-value]
        (println "<<<" str-value)
        (query/parse str-value
                     :simple
                     {}
                     "text"
                     (analyzer/create
                       {:tokenizer {:name "standard"}}))))))

(defn alternative-monitor-query-serializer [monitor-query-constructor-fn]
  (reify MonitorQuerySerializer
    (serialize [_ monitor-query]
      (BytesRef. ^CharSequence (.get (.getMetadata monitor-query) dictionary/CONF_KEY)))
    (deserialize [_ bytes-ref]
      (let [mq (json/read-value (io/reader (.bytes ^BytesRef bytes-ref)) json/keyword-keys-object-mapper)
            wo-meta-meta (update mq :meta dissoc dictionary/CONF_KEY)]
        (monitor-query-constructor-fn wo-meta-meta)))))

(def default-analyzer (analyzer/create {}))

(def DEFAULT_PRESEARCHER Presearcher/NO_FILTERING)

(def presearchers
  {:no-filtering            Presearcher/NO_FILTERING
   :term-filtered           (TermFilteredPresearcher.)
   :multipass-term-filtered (MultipassTermFilteredPresearcher. 2)})

(defn create [field-names-w-analyzers custom-analyzers options]
  (let [^MonitorConfiguration config (MonitorConfiguration.)
        per-field-analyzers (PerFieldAnalyzerWrapper. default-analyzer field-names-w-analyzers)
        presearcher (get presearchers (get options :presearcher) DEFAULT_PRESEARCHER)
        monitor-query-constructor-fn (dictionary/monitor-query-constructor custom-analyzers)
        monitor-query-serializer (alternative-monitor-query-serializer monitor-query-constructor-fn)
        queries-index-dir (get options :queries-index-dir)]
    (if queries-index-dir
      (.setIndexPath config
                     (Path/of queries-index-dir (into-array String []))
                     monitor-query-serializer)
      (.setDirectoryProvider config (reify IOSupplier
                                      (get [_] (ByteBuffersDirectory.)))
                             monitor-query-serializer))
    (.setQueryUpdateBufferSize config (int (get options :query-update-buffer-size 100000)))
    (Monitor. per-field-analyzers presearcher config)))

(defn defer-to-one-by-one-registration [^Monitor monitor monitor-queries]
  (doseq [mq monitor-queries]
    (try
      (.register monitor (doto (ArrayList.) (.add mq)))
      (catch Exception e
        (when (System/getenv "DEBUG_MODE")
          (.printStackTrace e))
        (.println System/err (format "Failed to register query %s with exception '%s'" mq (.getMessage e)))))))

(defn register-queries [^Monitor monitor monitor-queries]
  (try
    (.register monitor ^Iterable monitor-queries)
    (catch Exception e
      (when (System/getenv "DEBUG_MODE")
        (.printStackTrace e))
      (.println System/err (format "Failed to register queries with exception '%s'" (.getMessage e)))
      (defer-to-one-by-one-registration monitor monitor-queries))))

(defn field-name-analyzer-mappings
  "Creates a map with field names as keys and Lucene analyzers as values.
  First, group dictionary entries by field name. Then from every group of dictionary entries
  take the first entry and get the analyzer."
  [questionnaire]
  (reduce (fn [acc [field-name dict]]
            (assoc acc field-name (get (.nth ^Indexed dict 0) :monitor-analyzer)))
          {}
          (group-by :field-name questionnaire)))

(defn setup
  "Setups the monitor with all the questionnaire entries."
  [questionnaire default-type options custom-analyzers]
  (let [questionnaire-with-analyzers (dictionary/normalize questionnaire default-type options custom-analyzers)
        mappings-from-field-names-to-analyzers (field-name-analyzer-mappings questionnaire-with-analyzers)

        monitor (create mappings-from-field-names-to-analyzers custom-analyzers options)]
    (register-queries monitor (dictionary/get-monitor-queries questionnaire-with-analyzers))
    {:monitor     monitor
     :field-names (keys mappings-from-field-names-to-analyzers)}))
