(ns lmgrep.lucene.analysis-components
  (:require [clojure.tools.logging :as log]))

; TODO: convert to hashmap
(defn ^String stemmer
  "Creates a stemmer object given the stemmer keyword.
  Default stemmer is English."
  [stemmer-kw]
  (case stemmer-kw
    :arabic "arabicstem"
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
    :turkish "turkishSnowballStem"
    (do
      (when stemmer-kw
        (log/debugf "Stemmer '%s' not found! EnglishStemmer is used." stemmer-kw))
      "englishMinimalStem")))

; TODO: convert to hashmap
(defn tokenizer [tokenizer-kw]
  (case tokenizer-kw
    :keyword {:name "keyword"}
    :letter {:name "letter"}
    :standard {:name "standard"}
    :unicode-whitespace {:name "whitespace" :args {:rule "unicode"}}
    :whitespace {:name "whitespace" :args {:rule "java"}}
    (do
      (when tokenizer-kw
        (log/debugf "Tokenizer '%s' not found. StandardTokenizer is used." tokenizer-kw))
      {:name "standard"})))

(defn wdgf->token-filter-args [wdgf]
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
