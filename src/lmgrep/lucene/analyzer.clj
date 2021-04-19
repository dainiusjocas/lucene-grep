(ns lmgrep.lucene.analyzer
  (:require [clojure.string :as str])
  (:import (org.apache.lucene.analysis.custom CustomAnalyzer CustomAnalyzer$Builder)
           (java.util HashMap Map)
           (org.apache.lucene.analysis.util TokenizerFactory TokenFilterFactory CharFilterFactory)))

; https://lucene.apache.org/core/8_8_0/analyzers-common/constant-values.html#org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_WORD_PARTS

(set! *warn-on-reflection* true)

(defn stringify [m]
  (reduce (fn [acc [k v]] (assoc acc (name k) (str v))) {} m))

(def tokenizer-name->class
  (reduce (fn [acc ^String tokenizer-name]
            (assoc acc (str/lower-case tokenizer-name) (TokenizerFactory/lookupClass tokenizer-name)))
          {} (TokenizerFactory/availableTokenizers)))

(def char-filter-name->class
  (reduce (fn [acc ^String char-filter-name]
            (assoc acc (str/lower-case char-filter-name) (CharFilterFactory/lookupClass char-filter-name)))
          {} (CharFilterFactory/availableCharFilters)))

(def token-filter-name->class
  (reduce (fn [acc ^String token-filter-name]
            (assoc acc (str/lower-case token-filter-name) (TokenFilterFactory/lookupClass token-filter-name)))
          {} (TokenFilterFactory/availableTokenFilters)))

(def DEFAULT_TOKENIZER_NAME "standard")

; What about analyzers by name
(defn ^CustomAnalyzer create [{:keys [char-filters tokenizer token-filters]}]
  (try
    (let [^CustomAnalyzer$Builder cab (CustomAnalyzer/builder)]
      (.withTokenizer cab
                      ^Class
                      (get tokenizer-name->class (str/lower-case (get tokenizer :name DEFAULT_TOKENIZER_NAME)))
                      ^Map (HashMap. ^Map (stringify (get tokenizer :args))))

      (doseq [char-filter char-filters]
        (.addCharFilter cab
                        ^Class (get char-filter-name->class (str/lower-case (get char-filter :name)))
                        ^Map (HashMap. ^Map (stringify (get char-filter :args)))))

      (doseq [token-filter token-filters]
        (.addTokenFilter cab
                         ^Class (get token-filter-name->class (str/lower-case (get token-filter :name)))
                         ^Map (HashMap. ^Map (stringify (get token-filter :args)))))

      (.build cab))
    (catch Exception e
      (.println System/err (.getMessage e))
      (throw e))))

(comment
  (lmgrep.lucene.analyzer/create
    {:tokenizer {:name "standard"
                 :args {:maxTokenLength 4}}
     :char-filters [{:name "patternReplace"
                     :args {:pattern "joc"
                            :replacement "foo"}}]
     :token-filters [{:name "uppercase"}
                     {:name "reverseString"}]})

  (lmgrep.lucene.analyzer/create
    {:tokenizer {:name "standard"}
     :char-filters [{:name "patternReplace"
                     :args {:pattern "foo"
                            :replacement "bar"}}]}))
