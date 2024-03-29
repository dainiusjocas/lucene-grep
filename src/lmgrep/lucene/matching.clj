(ns lmgrep.lucene.matching
  (:require [clojure.string :as s]
            [lmgrep.lucene.dictionary :as dictionary])
  (:import (org.apache.lucene.monitor MonitorQuery Monitor
                                      HighlightsMatch HighlightsMatch$Hit
                                      ScoringMatch
                                      ScoringHighlightsMatch ScoringHighlightsMatch$Hit MatcherFactory)
           (org.apache.lucene.document Document Field FieldType)
           (org.apache.lucene.index IndexOptions)
           (java.util Map$Entry Set Iterator)))

(def ^FieldType field-type
  (doto (FieldType.)
    (.setTokenized true)
    (.setIndexOptions IndexOptions/DOCS_AND_FREQS)
    (.setStoreTermVectors true)
    (.setStoreTermVectorOffsets true)))

(defn ^:private highlights-match->highlights
  [^HighlightsMatch query-match ^Monitor monitor]
  (let [^MonitorQuery query (.getQuery monitor (.getQueryId query-match))
        meta (.getMetadata query)
        base {:query         (.getQueryString query)
              :type          (get meta "_type")
              :dict-entry-id (.getQueryId query-match)
              :meta          (dissoc (into {} meta) "_type" dictionary/CONF_KEY dictionary/DEFAULT_FIELD_NAME_KEY)}
        hits-iterator (.iterator (.entrySet (.getHits query-match)))
        highlights (transient [])]
    (while (.hasNext hits-iterator)
      (let [^Map$Entry map-entry (.next hits-iterator)      ; key of the map entry is a field name
            ^Set highlights-match-hits (.getValue map-entry)
            iterator (.iterator highlights-match-hits)]
        (while (.hasNext iterator)
          (conj! highlights (let [^HighlightsMatch$Hit hit (.next iterator)]
                              (assoc (assoc base :begin-offset (.-startOffset hit))
                                :end-offset (.-endOffset hit)))))))
    (persistent! highlights)))

(defn ^:private scoring-highlights-match->highlights
  [^ScoringHighlightsMatch query-match ^Monitor monitor]
  (let [^MonitorQuery query (.getQuery monitor (.getQueryId query-match))
        meta (.getMetadata query)
        base {:query         (.getQueryString query)
              :type          (get meta "_type")
              :dict-entry-id (.getQueryId query-match)
              :meta          (dissoc (into {} meta) "_type" dictionary/CONF_KEY dictionary/DEFAULT_FIELD_NAME_KEY)
              :score         (.getScore query-match)}
        hits-iterator (.iterator (.entrySet (.getHits query-match)))
        highlights (transient [])]
    (while (.hasNext hits-iterator)
      (let [^Map$Entry map-entry (.next hits-iterator)      ; key of the map entry is a field name
            ^Set highlights-match-hits (.getValue map-entry)
            iterator (.iterator highlights-match-hits)]
        (while (.hasNext iterator)
          (conj! highlights (let [^ScoringHighlightsMatch$Hit hit (.next iterator)]
                              (assoc (assoc base :begin-offset (.-startOffset hit))
                                :end-offset (.-endOffset hit)))))))
    (persistent! highlights)))

(defn ^:private match-and-collect
  [^String text ^Monitor monitor field-names ^MatcherFactory matcher-factory collector-fn]
  (let [combined-highlights (transient [])
        doc (Document.)]
    (doseq [field-name field-names]
      (.add doc (Field. ^String field-name text field-type)))
    (let [^Iterator miter (.iterator (.getMatches (.match monitor doc matcher-factory)))]
      (while (.hasNext miter)
        (reduce conj! combined-highlights (collector-fn (.next miter) monitor)))
      (persistent! combined-highlights))))

(defn match-text [^String text ^Monitor monitor field-names]
  (match-and-collect ^String text ^Monitor monitor field-names
                     (HighlightsMatch/MATCHER) highlights-match->highlights))

(defn match-with-scoring-highlights [^String text ^Monitor monitor field-names]
  (match-and-collect ^String text ^Monitor monitor field-names
                     (ScoringHighlightsMatch/MATCHER) scoring-highlights-match->highlights))

(defn match-with-score [^String text ^Monitor monitor field-names]
  (let [doc (Document.)]
    (doseq [field-name field-names]
      (.add doc (Field. ^String field-name text field-type)))
    (mapv (fn [^ScoringMatch query-match]
            (let [^MonitorQuery query (.getQuery monitor (.getQueryId query-match))
                  meta (.getMetadata query)]
              {:query         (.getQueryString query)
               :type          (get meta "_type")
               :dict-entry-id (.getQueryId query-match)
               :meta          (dissoc (into {} meta) "_type" dictionary/CONF_KEY dictionary/DEFAULT_FIELD_NAME_KEY)
               :score         (.getScore query-match)}))
          (.getMatches (.match monitor doc (ScoringMatch/DEFAULT_MATCHER))))))

(defn match-monitor [text monitor field-names opts]
  (if (s/blank? text)
    []
    (if (:with-scored-highlights opts)
      (match-with-scoring-highlights text monitor field-names)
      (if (:with-score opts)
        (match-with-score text monitor field-names)
        (match-text text monitor field-names)))))
