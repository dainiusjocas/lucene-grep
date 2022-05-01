(ns lmgrep.lucene.match-multi
  (:import (org.apache.lucene.monitor MonitorQuery Monitor
                                      HighlightsMatch HighlightsMatch$Hit
                                      MultiMatchingQueries)
           (org.apache.lucene.document Document Field FieldType)
           (org.apache.lucene.index IndexOptions)
           (java.util Set Collection Map$Entry)))

(set! *warn-on-reflection* true)

(def ^FieldType field-type
  (doto (FieldType.)
    (.setTokenized true)
    (.setIndexOptions IndexOptions/DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)
    (.setStoreTermVectors true)
    (.setStoreTermVectorOffsets true)))

(defn create-docs [texts field-names]
  (let [docs (mapv (fn [^String text]
                     (let [doc (Document.)]
                       (doseq [^String field-name field-names]
                         (.add doc (Field. field-name text field-type)))
                       doc)) texts)]
    (into-array Document docs)))

(defn handle-highlights-match [doc-id ^HighlightsMatch query-match ^Monitor monitor]
  (let [^MonitorQuery query (.getQuery monitor (.getQueryId query-match))
        meta (.getMetadata query)
        base {:doc-id        doc-id
              :query         (.getQueryString query)
              :type          (get meta "_type")
              :dict-entry-id (.getQueryId query-match)
              :meta          (dissoc (into {} meta) "_type")}
        hits-iterator (.iterator (.entrySet (.getHits query-match)))
        highlights (transient [])]
    (while (.hasNext hits-iterator)
      (let [^Map$Entry map-entry (.next hits-iterator)      ; key of the map entry is a field name
            ^Set highlights-match-hits (.getValue map-entry)
            iterator (.iterator highlights-match-hits)]
        (while (.hasNext iterator)
          (conj! highlights (let [^HighlightsMatch$Hit hit (.next iterator)]
                              (assoc base :begin-offset (.-startOffset hit)
                                          :end-offset (.-endOffset hit)))))))
    (persistent! highlights)))

(defn process [^long doc-id ^Collection matches ^Monitor monitor]
  (loop [matches matches
         acc (transient [])]
    (if (first matches)
      (recur (rest matches)
             (reduce conj! acc (handle-highlights-match doc-id (first matches) monitor)))
      (persistent! acc))))

(defn multi-match [texts ^Monitor monitor field-names]
  (let [#^"[Lorg.apache.lucene.document.Document;" docs (create-docs texts field-names)
        docs-count (alength docs)
        ^MultiMatchingQueries multi-match-queries (.match monitor docs (HighlightsMatch/MATCHER))]
    (loop [doc-id (long 0)
           acc (transient [])]
      (if (< doc-id docs-count)
        (recur (inc doc-id)
               (reduce conj! acc (process doc-id (.getMatches multi-match-queries doc-id) monitor)))
        (persistent! acc)))))
