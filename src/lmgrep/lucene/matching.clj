(ns lmgrep.lucene.matching
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log])
  (:import (org.apache.lucene.monitor MonitorQuery Monitor
                                      HighlightsMatch HighlightsMatch$Hit
                                      ScoringMatch
                                      ScoringHighlightsMatch ScoringHighlightsMatch$Hit)
           (org.apache.lucene.document Document Field FieldType)
           (org.apache.lucene.index IndexOptions)))

(def ^FieldType field-type
  (doto (FieldType.)
    (.setTokenized true)
    (.setIndexOptions IndexOptions/DOCS_AND_FREQS)
    (.setStoreTermVectors true)
    (.setStoreTermVectorOffsets true)))

(defn match-text [^String text ^Monitor monitor field-names type-name]
  (let [doc (Document.)]
    (doseq [field-name field-names]
      (.add doc (Field. ^String field-name text field-type)))
    (mapcat (fn [^HighlightsMatch query-match]
              (let [^MonitorQuery query (.getQuery monitor (.getQueryId query-match))
                    meta (.getMetadata query)
                    base {:text          (.getQueryString query)
                          :type          (or (get meta "_type") type-name)
                          :dict-entry-id (.getQueryId query-match)
                          :meta          (into {} meta)}]
                (mapcat (fn [[_ ^HighlightsMatch$Hit hit]]
                          (doall (map (fn [^HighlightsMatch$Hit h]
                                        (assoc base :begin-offset (.-startOffset h)
                                                    :end-offset (.-endOffset h))) hit)))
                        (.getHits query-match))))
            (.getMatches (.match monitor doc (HighlightsMatch/MATCHER))))))

(defn match-with-scoring-highlights [^String text ^Monitor monitor field-names type-name]
  (let [doc (Document.)]
    (doseq [field-name field-names]
      (.add doc (Field. ^String field-name text field-type)))
    (mapcat (fn [^ScoringHighlightsMatch query-match]
              (let [^MonitorQuery query (.getQuery monitor (.getQueryId query-match))
                    meta (.getMetadata query)
                    base {:text          (.getQueryString query)
                          :type          (or (get meta "_type") type-name)
                          :dict-entry-id (.getQueryId query-match)
                          :meta          (into {} meta)
                          :score         (.getScore query-match)}]
                (mapcat (fn [[_ ^ScoringHighlightsMatch$Hit hit]]
                          (doall (map (fn [^ScoringHighlightsMatch$Hit h]
                                        (assoc base :begin-offset (.-startOffset h)
                                                    :end-offset (.-endOffset h))) hit)))
                        (.getHits query-match))))
            (.getMatches (.match monitor doc (ScoringHighlightsMatch/MATCHER))))))

(defn match-with-score [^String text ^Monitor monitor field-names type-name]
  (let [doc (Document.)]
    (doseq [field-name field-names]
      (.add doc (Field. ^String field-name text field-type)))
    (map (fn [^ScoringMatch query-match]
           (let [^MonitorQuery query (.getQuery monitor (.getQueryId query-match))
                 meta (.getMetadata query)]
             {:text          (.getQueryString query)
              :type          (or (get meta "_type") type-name)
              :dict-entry-id (.getQueryId query-match)
              :meta          (into {} meta)
              :score         (.getScore query-match)}))
         (.getMatches (.match monitor doc (ScoringMatch/DEFAULT_MATCHER))))))

(defn match-monitor [text monitor field-names type-name opts]
  (log/debugf "Match monitor with opts='%s'" opts)
  (if (s/blank? text)
    []
    (if (:with-scored-highlights opts)
      (match-with-scoring-highlights text monitor field-names type-name)
      (if (:with-score opts)
        (match-with-score text monitor field-names type-name)
        (match-text text monitor field-names type-name)))))
