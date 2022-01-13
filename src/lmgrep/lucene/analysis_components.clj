(ns lmgrep.lucene.analysis-components)

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
  "wdgf stands for Word Delimiter Graph Filter"
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
