(ns lmgrep.matching
  (:require [jsonista.core :as json]
            [lmgrep.formatter :as formatter]))

(defn sum-score [highlights]
  (when-let [scores (seq (remove nil? (map :score highlights)))]
    (reduce + scores)))

(defrecord LineNrStr [nr str])

(defn matcher-fn [highlighter-fn file-path options]
  ;; This function can not return nil values
  (let [highlight-opts (select-keys options [:with-score :with-scored-highlights])
        with-details? (:with-details options)
        format (:format options)
        scored? (or (:with-score options) (:with-scored-highlights options))]
    (fn [^LineNrStr line-nr-and-line-str]
      (if-let [highlights (seq (highlighter-fn (.str line-nr-and-line-str) highlight-opts))]
        (let [details (cond-> {:line-number (inc (.nr line-nr-and-line-str))
                               :line        (.str line-nr-and-line-str)}
                              file-path (assoc :file file-path)
                              (true? scored?) (assoc :score (sum-score highlights))
                              (true? with-details?) (assoc :highlights highlights))]
          (case format
            :edn (pr-str details)
            :json (json/write-value-as-string details)
            :string (formatter/string-output highlights details options)
            (formatter/string-output highlights details options)))
        ""))))
