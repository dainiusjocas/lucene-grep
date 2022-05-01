(ns lmgrep.matching
  (:require [jsonista.core :as json]
            [lmgrep.formatter :as formatter]
            [clojure.string :as str])
  (:import (lmgrep.lucene LuceneMonitorMatcher)
           (clojure.lang Indexed)))

(defn sum-score [highlights]
  (when-let [scores (seq (remove nil? (map :score highlights)))]
    (reduce + scores)))

(defn matcher-fn [highlighter-fn file-path options]
  (let [highlight-opts (select-keys options [:with-score :with-scored-highlights])
        with-details? (get options :with-details)
        format (get options :format)
        scored? (or (get options :with-score) (get options :with-scored-highlights))]
    (fn [line-nr line]
      (when-let [highlights (seq (highlighter-fn line highlight-opts))]
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

(defn batched-matcher-fn [highlighter-fn file-path options]
  (let [highlight-opts (select-keys options [:with-score :with-scored-highlights])
        with-details? (get options :with-details)
        format (get options :format)
        scored? (or (get options :with-score) (get options :with-scored-highlights))]
    (fn [lines]
      (when-let [highlights (seq (highlighter-fn lines highlight-opts))]
        (let [grouped (group-by :doc-id highlights)
              highlighted-lines (mapv (fn [[line-number highlights]]
                                        (let [details (cond-> {:line-number line-number
                                                               :line        (.nth ^Indexed lines line-number)}
                                                              file-path (assoc :file file-path)
                                                              (true? scored?) (assoc :score (sum-score highlights))
                                                              (true? with-details?) (assoc :highlights highlights))]
                                          (case format
                                            :edn (pr-str details)
                                            :json (json/write-value-as-string details)
                                            :string (formatter/string-output highlights details options)
                                            (formatter/string-output highlights details options))))
                                      grouped)]
          (str/join "\n" highlighted-lines))))))

(defn matcher-fn-2 [^LuceneMonitorMatcher highlighter-obj file-path options]
  (let [highlight-opts (select-keys options [:with-score :with-scored-highlights])
        with-details? (get options :with-details)
        format (get options :format)
        scored? (or (get options :with-score) (get options :with-scored-highlights))]
    (fn [line-nr line]
      (when-let [highlights (seq (.match highlighter-obj line highlight-opts))]
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
