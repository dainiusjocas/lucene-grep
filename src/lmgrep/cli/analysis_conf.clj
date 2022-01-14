(ns lmgrep.cli.analysis-conf
  (:require [clojure.tools.logging :as log]))

(def analysis-keys #{:case-sensitive?
                     :ascii-fold?
                     :stem?
                     :tokenizer
                     :stemmer
                     :word-delimiter-graph-filter})

(def default-text-analysis
  {:tokenizer {:name "standard"}
   :token-filters [{:name "lowercase"}
                   {:name "asciifolding"}
                   {:name "englishMinimalStem"}]})

(def ^String stemmer
  "Creates a stemmer object given the stemmer keyword.
  Default stemmer is English."
  {:arabic "arabicstem"
   :armenian "armenianSnowballStem"
   :basque "basqueSnowballStem"
   :catalan "catalanSnowballStem"
   :danish "danishSnowballStem"
   :dutch "dutchSnowballStem"
   :english "englishMinimalStem"
   :estonian "basqueSnowballStem"
   :finnish "finnishlightstem"
   :french "frenchLightStem"
   :german2 "germanlightstem"
   :german "germanstem"
   :hungarian "hungarianLightStem"
   :irish "irishSnowballStem"
   :italian "italianlightstem"
   :kp "kpSnowballStem"
   :lithuanian "lithuanianSnowballStem"
   :lovins "lovinsSnowballStem"
   :norwegian "norwegianminimalstem"
   :porter "porterstem"
   :portuguese "portugueselightstem"
   :romanian "romanianSnowballStem"
   :russian "russianlightstem"
   :spanish "spanishlightstem"
   :swedish "swedishlightstem"
   :turkish "turkishSnowballStem"})

(def tokenizer
  {:keyword {:name "keyword"}
   :letter {:name "letter"}
   :standard {:name "standard"}
   :unicode-whitespace {:name "whitespace" :args {:rule "unicode"}}
   :whitespace {:name "whitespace" :args {:rule "java"}}})

(defn wdgf->token-filter-args
  "wdgf stands for Word Delimiter Graph Filter
  https://lucene.apache.org/core/8_8_0/analyzers-common/constant-values.html#org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_WORD_PARTS"
  [wdgf]
  (when (pos-int? wdgf)
    (cond-> {}
            (not (zero? (bit-and wdgf 1))) (assoc "generateWordParts" 1)
            (not (zero? (bit-and wdgf 2))) (assoc "generateNumberParts" 1)
            (not (zero? (bit-and wdgf 4))) (assoc "catenateWords" 1)
            (not (zero? (bit-and wdgf 8))) (assoc "catenateNumbers" 1)
            (not (zero? (bit-and wdgf 16))) (assoc "catenateAll" 1)
            (not (zero? (bit-and wdgf 32))) (assoc "preserveOriginal" 1)
            (not (zero? (bit-and wdgf 64))) (assoc "splitOnCaseChange" 1)
            (not (zero? (bit-and wdgf 128))) (assoc "splitOnNumerics" 1)
            (not (zero? (bit-and wdgf 256))) (assoc "stemEnglishPossessive" 1)
            (not (zero? (bit-and wdgf 512))) (assoc "ignoreKeywords" 1))))


(defn override-token-filters [token-filters flags]
  (cond->> token-filters
           (true? (get flags :case-sensitive?))
           (remove (fn [tf] (= "lowercase" (name (get tf :name)))))
           (false? (get flags :ascii-fold?))
           (remove (fn [tf] (= "asciifolding" (name (get tf :name)))))
           (false? (get flags :stem?))
           (remove (fn [tf] (re-matches #".*[Ss]tem.*" (name (get tf :name)))))
           (keyword? (keyword (get flags :stemmer)))
           ((fn [token-filters]
              (conj (into [] (remove (fn [token-filter]
                                       (re-matches #".*[Ss]tem.*" (name (get token-filter :name))))
                                     token-filters))
                    {:name (let [stemmer-kw (keyword (get flags :stemmer))]
                             (get stemmer
                                  stemmer-kw
                                  (do
                                    (when stemmer-kw
                                      (log/debugf "Stemmer '%s' not found! EnglishStemmer is used." stemmer-kw))
                                    "englishMinimalStem")))})))
           (pos-int? (get flags :word-delimiter-graph-filter))
           (cons {:name "worddelimitergraph"
                  :args (wdgf->token-filter-args
                          (get flags :word-delimiter-graph-filter))})))

(defn override-acm [acm flags]
  (let [tokenizer (or (when-let [tokenizer-kw (get flags :tokenizer)]
                        (get tokenizer
                             tokenizer-kw
                             (do
                               (when tokenizer-kw
                                 (log/debugf "Tokenizer '%s' not found. StandardTokenizer is used." tokenizer-kw))
                               {:name "standard"})))
                      (:tokenizer acm))
        token-filters (override-token-filters (get acm :token-filters) flags)]
    (assoc acm
      :tokenizer tokenizer
      :token-filters token-filters)))

(defn prepare-analysis-configuration
  "When analysis key is not provided then construct analysis config
  by overriding default-text-analysis with provided analysis-flags if any.
  Given the default text analysis config appl"
  [default-text-analysis options]
  (if (empty? (get options :analysis))
    (let [analysis-flags (select-keys options analysis-keys)]
      (if (empty? analysis-flags)
        default-text-analysis
        (override-acm default-text-analysis analysis-flags)))
    (get options :analysis)))
