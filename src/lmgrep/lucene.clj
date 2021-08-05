(ns lmgrep.lucene
  (:require [clojure.string :as s]
            [lmgrep.lucene.matching :as matching]
            [lmgrep.lucene.monitor :as monitor]))

(defn highlighter
  ([questionnaire] (highlighter questionnaire {}))
  ([questionnaire {:keys [type-name] :as options}]
   (let [default-type (if (s/blank? type-name) "QUERY" type-name)
         {:keys [monitor field-names]} (monitor/setup questionnaire default-type options)]
     (fn
       ([text] (matching/match-monitor text monitor field-names {}))
       ([text opts] (matching/match-monitor text monitor field-names opts))))))

(comment
  ((highlighter [{:query "text"}] {}) "foo text bar")

  ((highlighter [{:query "best class"
                  :case-sensitive? false
                  :word-delimiter-graph-filter (+ 1 2 32 64)}] {}) "foo text bar BestClass fooo name")

  ((highlighter [{:query "text bar"}]) "foo text bar one more time text with bar text" {:with-score true}))
