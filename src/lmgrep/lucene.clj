(ns lmgrep.lucene
  (:require [clojure.string :as s]
            [lmgrep.lucene.matching :as matching]
            [lmgrep.lucene.monitor :as monitor])
  (:import (java.io Closeable)
           (org.apache.lucene.monitor Monitor)))

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
  (^LuceneMonitorMatcher [questionnaire] (highlighter-obj questionnaire {}))
  (^LuceneMonitorMatcher [questionnaire options] (highlighter-obj questionnaire options {}))
  (^LuceneMonitorMatcher [questionnaire {:keys [type-name] :as options} custom-analyzers]
   (let [default-type (if (s/blank? type-name) "QUERY" type-name)
         {:keys [monitor field-names]} (monitor/setup questionnaire default-type options custom-analyzers)]
     (->LuceneMonitorMatcher monitor field-names))))

(comment
  (with-open [lm (highlighter-obj [{:query "text"}] {})]
    (match lm "foo text bar")))

(defn highlight
  "Convenience function that creates a highlighter, matches the text,
  closes the highlighter, returns matches."
  [dictionary highlighter-opts text match-opts]
  (with-open [highlighter (highlighter-obj dictionary highlighter-opts)]
    (match highlighter text match-opts)))

(comment
  (dotimes [_ 10000] (highlight [{:query "text"}] {} "foo text bar" {})))
