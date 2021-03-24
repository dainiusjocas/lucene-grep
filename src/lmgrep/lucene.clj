(ns lmgrep.lucene
  (:require [clojure.string :as s]
            [lmgrep.lucene.monitor :as monitor]
            [lmgrep.lucene.matching :as matching]
            [lmgrep.lucene.dictionary :as dictionary]))

(defn highlighter
  ([dictionary] (highlighter dictionary {}))
  ([dictionary {:keys [type-name tokenizer]}]
   (let [type-name (if (s/blank? type-name) "QUERY" type-name)
         {:keys [monitor field-names]} (monitor/setup dictionary
                                                      {:tokenizer tokenizer}
                                                      dictionary/dictionary->monitor-queries)]
     (fn
       ([text] (matching/match-monitor text monitor field-names type-name {}))
       ([text opts] (matching/match-monitor text monitor field-names type-name opts))))))

(comment
  ((highlighter [{:text "text"}] {}) "foo text bar")

  ((highlighter [{:text "best class"
                  :case-sensitive? false
                  :word-delimiter-graph-filter (+ 1 2 32 64)}] {}) "foo text bar BestClass fooo name")

  ((highlighter [{:text "text bar"}]) "foo text bar one more time text with bar text" {:with-score true}))
