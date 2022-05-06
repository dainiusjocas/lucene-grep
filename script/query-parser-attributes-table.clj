(require '[lmgrep.lucene.query-parser])
(require '[lmgrep.lucene.query-parser.conf])
(require '[clojure.pprint])
(require '[clojure.string])

;; Create all five query parsers
;; iterate and collect pairs of an attribute and whether it is applicable
;; one line per attribute to which query parser it applies
;; maybe collect java docs
;; Clojure.pprint/print-table

(let [query-parsers (sort [:classic :complex-phrase :surround :simple :standard])
      all-attributes (sort (mapcat (fn [[k v]] (keys v)) lmgrep.lucene.query-parser.conf/query-parser-class->attrs))
      qps (into {} (map (fn [query-parser-kw]
                          (let [qp (lmgrep.lucene.query-parser/create query-parser-kw {} "field-name" nil)]
                            {query-parser-kw (set (remove nil?
                                                          (flatten
                                                            (for [[klazz defaults] lmgrep.lucene.query-parser.conf/query-parser-class->attrs]
                                                              (when (instance? klazz qp)
                                                                (keys defaults))))))})) query-parsers))
      attrs qps
      header (concat [:option] query-parsers)
      rows (conj (map (fn [attr]
                        (reduce (fn [acc query-parser-kw]
                                  (assoc acc query-parser-kw (contains? (get attrs query-parser-kw) attr)))
                                {:option attr} query-parsers))
                      all-attributes))]
  (println (clojure.string/replace (with-out-str (clojure.pprint/print-table header rows))
                                   "+"
                                   "|")))

;; TODO: think if it is possible to update docs on every release

(def qps-kws #{:classic :complex-phrase :surround :simple :standard})

(def qps (map (fn [qp-kw] (lmgrep.lucene.query-parser/create qp-kw)) qps-kws))

(def klass->defaults
  (reduce-kv
    (fn [m k v] (assoc m k (reduce (fn [acc [k v]] (assoc acc k (:default v))) {} v)))
    {}
    lmgrep.lucene.query-parser.conf/query-parser-class->attrs))

(defn configure [query-parser]
  (reduce (fn [acc [klass defaults]]
            (if (instance? ^Class klass query-parser)
              (merge acc defaults)
              acc)) {} klass->defaults))

(def qp-kw->defaults
  (reduce (fn [acc qp-kw]
            (assoc acc qp-kw (configure (lmgrep.lucene.query-parser/create qp-kw))))
          {} qps-kws))

(let [query-parsers (sort [:classic :complex-phrase :surround :simple :standard])
      all-attributes (sort (mapcat (fn [[k v]] (keys v)) lmgrep.lucene.query-parser.conf/query-parser-class->attrs))
      qps (into {} (map (fn [query-parser-kw]
                          (let [qp (lmgrep.lucene.query-parser/create query-parser-kw {} "field-name" nil)]
                            {query-parser-kw (set (remove nil?
                                                          (flatten
                                                            (for [[klazz defaults] lmgrep.lucene.query-parser.conf/query-parser-class->attrs]
                                                              (when (instance? klazz qp)
                                                                (keys defaults))))))})) query-parsers))
      attrs qps
      header (concat [:option] query-parsers)
      rows (conj (map (fn [attr]
                        (reduce (fn [acc query-parser-kw]
                                  (assoc acc query-parser-kw
                                             (let [default-value (get-in qp-kw->defaults [query-parser-kw attr])]
                                               (if (nil? default-value)
                                                 ""
                                                 default-value))))
                                {:option attr} query-parsers))
                      all-attributes))]
  (println (clojure.string/replace (with-out-str (clojure.pprint/print-table header rows))
                                   "+"
                                   "|")))
