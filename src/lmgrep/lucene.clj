(ns lmgrep.lucene
  (:require [clojure.string :as s]
            [lmgrep.lucene.matching :as matching]
            [lmgrep.lucene.monitor :as monitor])
  (:import (java.io Closeable)
           (org.apache.lucene.monitor Monitor)))

(defn highlighter
  ([questionnaire] (highlighter questionnaire {}))
  ([questionnaire options] (highlighter questionnaire options {}))
  ([questionnaire {:keys [type-name] :as options} custom-analyzers]
   (let [default-type (if (s/blank? type-name) "QUERY" type-name)
         {:keys [monitor field-names]} (monitor/setup questionnaire default-type options custom-analyzers)]
     (fn
       ([text] (matching/match-monitor text monitor field-names {}))
       ([text opts] (matching/match-monitor text monitor field-names opts))))))

(defprotocol IMatcher
  (match [this text] [this text opts]))

(deftype LuceneMonitorMatcher [^Monitor monitor field-names]
  IMatcher
  (match [_ text]
    (matching/match-monitor text monitor field-names {}))
  (match [_ text opts]
    (matching/match-monitor text monitor field-names opts))
  Closeable
  (close [_] (.close ^Monitor monitor)))

(defn highlighter-obj
  ([questionnaire] (highlighter-obj questionnaire {}))
  ([questionnaire options] (highlighter-obj questionnaire options {}))
  ([questionnaire {:keys [type-name] :as options} custom-analyzers]
   (let [default-type (if (s/blank? type-name) "QUERY" type-name)
         {:keys [monitor field-names]} (monitor/setup questionnaire default-type options custom-analyzers)]
     (->LuceneMonitorMatcher monitor field-names))))

(comment
  ((highlighter [{:query "text"}] {}) "foo text bar")
  ((highlighter [{:query "text bar"}]) "foo text bar one more time text with bar text" {:with-score true})

  (with-open [lm (highlighter-obj [{:query "text"}] {})]
    (.match lm "foo text bar")))
