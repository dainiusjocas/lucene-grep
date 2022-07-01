(ns lmgrep.matching
  (:require [jsonista.core :as json]
            [lmgrep.formatter :as formatter]
            [lmgrep.lucene :as lucene]
            [clojure.pprint])
  (:import (lmgrep.lucene LuceneMonitorMatcher)))

(defn sum-score [highlights]
  (when-let [scores (seq (remove nil? (map :score highlights)))]
    (reduce + scores)))

(defn matcher-fn [^LuceneMonitorMatcher highlighter-obj file-path options]
  (let [highlight-opts (select-keys options [:with-score :with-scored-highlights])
        with-details? (get options :with-details)
        format (get options :format)
        scored? (or (get options :with-score) (get options :with-scored-highlights))]
    (fn [line-nr line]
      (when-let [highlights (seq (lucene/match highlighter-obj line highlight-opts))]
        (clojure.pprint/pprint highlights)
        (let [details (cond-> {:line-number line-nr
                               :line        line}
                              file-path (assoc :file file-path)
                              (true? scored?) (assoc :score (sum-score highlights))
                              (true? with-details?) (assoc :highlights highlights))]
          (case format
            :edn (pr-str details)
            :json (json/write-value-as-string details)
            :string (formatter/string-output highlights details options)
            (formatter/string-output highlights details options)))))))
