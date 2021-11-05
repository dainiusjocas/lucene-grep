(require '[lmgrep.lucene.query-parser])
(require '[clojure.pprint])
(require '[clojure.string])
;; Create all five query parsers
;; iterate and collect pairs of an attribute and whether it is applicable
;; one line per attribute to which query parser it applies
;; maybe collect java docs
;; Clojure.pprint/print-table

(let [query-parsers (sort [:classic :complex-phrase :surround :simple :standard])
      all-attributes (sort (mapcat (fn [[k v]] (keys v)) lmgrep.lucene.query-parser/query-parser-class->attrs))
      qps (into {} (map (fn [query-parser-kw]
                          (let [qp (lmgrep.lucene.query-parser/create query-parser-kw {} "field-name" nil)]
                            {query-parser-kw (set (remove nil?
                                                          (flatten
                                                            (for [[klazz defaults] lmgrep.lucene.query-parser/query-parser-class->attrs]
                                                              (when (instance? klazz qp)
                                                                (keys defaults))))))})) query-parsers))]
  (let [attrs qps
        header (concat [:attribute] query-parsers)
        rows (conj (map (fn [attr]
                          (reduce (fn [acc query-parser-kw]
                                    (assoc acc query-parser-kw (contains? (get attrs query-parser-kw) attr)))
                                  {:attribute attr} query-parsers))
                        all-attributes))]
    (println (clojure.string/replace (with-out-str (clojure.pprint/print-table header rows))
                                     "+"
                                     "|"))))

