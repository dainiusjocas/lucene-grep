(ns lmgrep.lucene.monitor
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [lucene.custom.query :as query]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.dictionary :as dictionary]
            [lmgrep.print :as print])
  (:import (org.apache.lucene.monitor MonitorConfiguration Monitor MonitorQuerySerializer Presearcher TermFilteredPresearcher MultipassTermFilteredPresearcher)
           (org.apache.lucene.analysis.miscellaneous PerFieldAnalyzerWrapper)
           (java.util ArrayList)
           (java.util.function Function)
           (java.nio.file Path)
           (org.apache.lucene.util BytesRef IOSupplier)
           (org.apache.lucene.store ByteBuffersDirectory)))

(set! *warn-on-reflection* true)

(def monitor-query-serializer
  (MonitorQuerySerializer/fromParser
    (reify Function
      (apply [_ str-value]
        (query/parse str-value :simple)))))

(defn disk-oriented-monitor-query-serializer [monitor-query-constructor-fn]
  (reify MonitorQuerySerializer
    (serialize [_ monitor-query]
      (BytesRef. ^CharSequence (.get (.getMetadata monitor-query) dictionary/CONF_KEY)))
    (deserialize [_ bytes-ref]
      (let [mq (json/read-value (io/reader (.bytes ^BytesRef bytes-ref)) json/keyword-keys-object-mapper)
            wo-meta-meta (update mq :meta dissoc dictionary/CONF_KEY)]
        (monitor-query-constructor-fn wo-meta-meta)))))

(def default-analyzer (analyzer/create {}))

(def DEFAULT_PRESEARCHER Presearcher/NO_FILTERING)

(def SCHEMA_FILE_NAME "/schema.json")

(def presearchers
  {:no-filtering            Presearcher/NO_FILTERING
   :term-filtered           (TermFilteredPresearcher.)
   :multipass-term-filtered (MultipassTermFilteredPresearcher. 2)})

(defn prepare-per-field-analyzers [field-name->analysis-conf custom-analyzers]
  (reduce-kv (fn [schema field-name analysis-conf]
               (assoc schema
                 (name field-name)
                 (dictionary/get-string-analyzer analysis-conf custom-analyzers)))
             {} field-name->analysis-conf))

(defn questionnaire->schema [questionnaire]
  (reduce (fn [acc d]
            (assoc acc (:field-name d) (:analysis-conf d)))
          {}
          questionnaire))

(defn prepare-schema
  "Schema from the queries file takes precedence over the one stored on disk."
  [questionnaire-with-analyzers options]
  (let [field-name->analysis-conf (questionnaire->schema questionnaire-with-analyzers)]
    (if-let [queries-index-dir (get options :queries-index-dir)]
     (let [schema-file (str queries-index-dir SCHEMA_FILE_NAME)
           field->analysis-conf (merge
                                  (when (fs/exists? schema-file)
                                    (json/read-value (slurp schema-file) json/keyword-keys-object-mapper))
                                  field-name->analysis-conf)]
       (when-not (fs/exists? queries-index-dir)
         (fs/create-dir queries-index-dir))
       (when-not (fs/exists? schema-file)
         (fs/create-file schema-file))
       (spit schema-file (json/write-value-as-string field->analysis-conf))
       field->analysis-conf)
     field-name->analysis-conf)))

(defn create [questionnaire-with-analyzers custom-analyzers options]
  (let [^MonitorConfiguration config (MonitorConfiguration.)
        presearcher (get presearchers (get options :presearcher) DEFAULT_PRESEARCHER)]
    (.setQueryUpdateBufferSize config (int (get options :query-update-buffer-size 100000)))
    (if-let [queries-index-dir (get options :queries-index-dir)]
      (.setIndexPath config
                     (Path/of queries-index-dir (into-array String []))
                     (disk-oriented-monitor-query-serializer
                       (dictionary/monitor-query-constructor custom-analyzers)))
      (.setDirectoryProvider config (reify IOSupplier
                                      (get [_] (ByteBuffersDirectory.)))
                             monitor-query-serializer))
    (let [schema (prepare-schema questionnaire-with-analyzers options)
          field-name->analyzer (prepare-per-field-analyzers schema custom-analyzers)
          per-field-analyzers (PerFieldAnalyzerWrapper. default-analyzer field-name->analyzer)]
      (Monitor. per-field-analyzers presearcher config))))

(defn defer-to-one-by-one-registration [^Monitor monitor monitor-queries]
  (doseq [mq monitor-queries]
    (try
      (.register monitor (doto (ArrayList.) (.add mq)))
      (catch Exception e
        (when (System/getenv "DEBUG_MODE")
          (print/throwable e))
        (print/to-err (format "Failed to register query %s with exception '%s'" mq (.getMessage e)))))))

(defn register-queries [^Monitor monitor monitor-queries]
  (try
    (.register monitor ^Iterable monitor-queries)
    (catch Exception e
      (when (System/getenv "DEBUG_MODE")
        (print/throwable e))
      (print/to-err (format "Failed to register queries with exception '%s'" (.getMessage e)))
      (defer-to-one-by-one-registration monitor monitor-queries))))

(defn setup
  "Setups the monitor with all the questionnaire entries."
  [questionnaire default-type options custom-analyzers]
  (let [questionnaire (dictionary/normalize questionnaire default-type options custom-analyzers)
        monitor (create questionnaire custom-analyzers options)]
    (register-queries monitor (dictionary/get-monitor-queries questionnaire))
    {:monitor     monitor
     :field-names (set (mapv :field-name questionnaire))}))
