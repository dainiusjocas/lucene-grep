(ns lmgrep.lucene.analyzer
  (:require [clojure.string :as str]
            [lmgrep.features :as features]
            [lmgrep.lucene.predefined-analyzers :as lucene.predefined])
  (:import (java.util HashMap Map)
           (org.apache.lucene.analysis.custom CustomAnalyzer CustomAnalyzer$Builder)
           (org.apache.lucene.analysis.util TokenizerFactory TokenFilterFactory CharFilterFactory)
           (org.apache.lucene.analysis.en LovinsSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.analysis.da DanishSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.et EstonianSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.eu BasqueSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.ga IrishSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.lt LithuanianSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.ro RomanianSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.tr TurkishSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.hy ArmenianSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.nl DutchSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.ca CatalanSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.nl KPSnowballStemTokenFilterFactory)))

; https://lucene.apache.org/core/8_8_0/analyzers-common/constant-values.html#org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_WORD_PARTS

(set! *warn-on-reflection* true)

(defn namify
  "Normalizes analysis component name."
  [component-name]
  (str/lower-case component-name))

(defn stringify [m]
  (reduce (fn [acc [k v]] (assoc acc (name k) (str v))) {} m))

(def tokenizer-name->class
  (reduce (fn [acc ^String tokenizer-name]
            (assoc acc (namify tokenizer-name) (TokenizerFactory/lookupClass tokenizer-name)))
          {} (TokenizerFactory/availableTokenizers)))

(def char-filter-name->class
  (reduce (fn [acc ^String char-filter-name]
            (assoc acc (namify char-filter-name) (CharFilterFactory/lookupClass char-filter-name)))
          {} (CharFilterFactory/availableCharFilters)))

(def default-token-filters
  (dissoc (assoc (reduce (fn [acc ^String token-filter-name]
                           (assoc acc (namify token-filter-name) (TokenFilterFactory/lookupClass token-filter-name)))
                         {} (TokenFilterFactory/availableTokenFilters))
            (namify "lithuanianSnowballStem") LithuanianSnowballStemTokenFilterFactory
            (namify "armenianSnowballStem") ArmenianSnowballStemTokenFilterFactory
            (namify "basqueSnowballStem") BasqueSnowballStemTokenFilterFactory
            (namify "catalanSnowballStem") CatalanSnowballStemTokenFilterFactory
            (namify "danishSnowballStem") DanishSnowballStemTokenFilterFactory
            (namify "dutchSnowballStem") DutchSnowballStemTokenFilterFactory
            (namify "basqueSnowballStem") EstonianSnowballStemTokenFilterFactory
            (namify "irishSnowballStem") IrishSnowballStemTokenFilterFactory
            (namify "kpSnowballStem") KPSnowballStemTokenFilterFactory
            (namify "turkishSnowballStem") TurkishSnowballStemTokenFilterFactory
            (namify "romanianSnowballStem") RomanianSnowballStemTokenFilterFactory
            (namify "lovinsSnowballStem") LovinsSnowballStemTokenFilterFactory)
          "synonym"                                        ; because deprecated and requires a patch
          ))

(def token-filter-name->class
  (cond-> default-token-filters
          features/raudikko? (assoc (namify "raudikko")
                                    (import 'org.apache.lucene.analysis.fi.RaudikkoTokenFilterFactory))))

(def DEFAULT_TOKENIZER_NAME "standard")

;(defn ^Analyzer get-analyzer
;  ([analyzer-conf]
;   (get-analyzer analyzer-conf lucene.predefined/analyzers))
;  ([analyzer-conf default-analyzers]
;   (get default-analyzers (get analyzer-conf :name))))

(defn custom-analyzer [{:keys [char-filters tokenizer token-filters] :as opts}]
  (let [^CustomAnalyzer$Builder cab (CustomAnalyzer/builder)]
    (when (nil? (get tokenizer-name->class (namify (get tokenizer :name DEFAULT_TOKENIZER_NAME))))
      (throw (Exception. (format "Tokenizer '%s' is not available. Choose one of: %s"
                                 (get tokenizer :name)
                                 (sort (keys tokenizer-name->class))))))
    (.withTokenizer cab
                    ^Class
                    (get tokenizer-name->class (namify (get tokenizer :name DEFAULT_TOKENIZER_NAME)))
                    ^Map (HashMap. ^Map (stringify (get tokenizer :args))))

    (doseq [char-filter char-filters]
      (when (nil? (get char-filter-name->class (namify (get char-filter :name))))
        (throw (Exception. (format "Char filter '%s' is not available. Choose one of: %s"
                                   (get char-filter :name)
                                   (sort (keys char-filter-name->class))))))
      (.addCharFilter cab
                      ^Class (get char-filter-name->class (namify (get char-filter :name)))
                      ^Map (HashMap. ^Map (stringify (get char-filter :args)))))

    (doseq [token-filter token-filters]
      (when (nil? (get token-filter-name->class (namify (get token-filter :name))))
        (throw (Exception. (format "Token Filter '%s' is not available. Choose one of: %s"
                                   (get token-filter :name)
                                   (sort (keys token-filter-name->class))))))
      (.addTokenFilter cab
                       ^Class (get token-filter-name->class (namify (get token-filter :name)))
                       ^Map (HashMap. ^Map (stringify (get token-filter :args)))))

    (.build cab)))

(defn ^Analyzer create
  "Either fetches a predefined analyzer or creates one from the config."
  [{:keys [analyzer] :as opts}]
  (try
    (or
      (when-let [n (get analyzer :name)]
        (when (nil? (get lucene.predefined/analyzers (namify (str/replace n "Analyzer" ""))))
          (throw (Exception. (format "Analyzer '%s' is not available. Choose one of: %s"
                                     (get analyzer :name)
                                     (sort (keys lucene.predefined/analyzers))))))
        (get lucene.predefined/analyzers (namify (str/replace n "Analyzer" ""))))
      (custom-analyzer opts))
    (catch Exception e
      (when (System/getenv "DEBUG_MODE")
        (.printStackTrace e))
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
