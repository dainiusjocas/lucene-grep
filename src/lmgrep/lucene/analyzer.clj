(ns lmgrep.lucene.analyzer
  (:require [clojure.string :as str])
  (:import (java.util HashMap Map ArrayList Locale)
           (java.text Collator)
           (org.apache.lucene.analysis.custom CustomAnalyzer CustomAnalyzer$Builder)
           (org.apache.lucene.analysis.util TokenizerFactory TokenFilterFactory CharFilterFactory)
           (org.apache.lucene.analysis.en EnglishAnalyzer LovinsSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis Analyzer CharArraySet)
           (org.apache.lucene.analysis.ar ArabicAnalyzer)
           (org.apache.lucene.analysis.standard StandardAnalyzer ClassicAnalyzer UAX29URLEmailAnalyzer)
           (org.apache.lucene.analysis.bg BulgarianAnalyzer)
           (org.apache.lucene.analysis.bn BengaliAnalyzer)
           (org.apache.lucene.analysis.br BrazilianAnalyzer)
           (org.apache.lucene.analysis.ca CatalanAnalyzer)
           (org.apache.lucene.analysis.core StopAnalyzer UnicodeWhitespaceAnalyzer KeywordAnalyzer SimpleAnalyzer WhitespaceAnalyzer)
           (org.apache.lucene.analysis.cjk CJKAnalyzer)
           (org.apache.lucene.analysis.ckb SoraniAnalyzer)
           (org.apache.lucene.analysis.cz CzechAnalyzer)
           (org.apache.lucene.analysis.da DanishAnalyzer DanishSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.de GermanAnalyzer)
           (org.apache.lucene.analysis.el GreekAnalyzer)
           (org.apache.lucene.analysis.es SpanishAnalyzer)
           (org.apache.lucene.analysis.et EstonianAnalyzer EstonianSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.eu BasqueAnalyzer BasqueSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.fa PersianAnalyzer)
           (org.apache.lucene.analysis.fi FinnishAnalyzer)
           (org.apache.lucene.analysis.fr FrenchAnalyzer)
           (org.apache.lucene.analysis.ga IrishAnalyzer IrishSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.gl GalicianAnalyzer)
           (org.apache.lucene.analysis.hi HindiAnalyzer)
           (org.apache.lucene.analysis.hu HungarianAnalyzer)
           (org.apache.lucene.analysis.hy ArmenianAnalyzer)
           (org.apache.lucene.analysis.id IndonesianAnalyzer)
           (org.apache.lucene.analysis.it ItalianAnalyzer)
           (org.apache.lucene.analysis.lt LithuanianAnalyzer LithuanianSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.lv LatvianAnalyzer)
           (org.apache.lucene.analysis.no NorwegianAnalyzer)
           (org.apache.lucene.analysis.pt PortugueseAnalyzer)
           (org.apache.lucene.analysis.ro RomanianAnalyzer RomanianSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.ru RussianAnalyzer)
           (org.apache.lucene.analysis.ru RussianAnalyzer)
           (org.apache.lucene.analysis.sv SwedishAnalyzer)
           (org.apache.lucene.analysis.th ThaiAnalyzer)
           (org.apache.lucene.analysis.tr TurkishAnalyzer TurkishSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.pl PolishAnalyzer)
           (org.apache.lucene.analysis.hy ArmenianSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.nl DutchAnalyzer DutchSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.ca CatalanSnowballStemTokenFilterFactory)
           (org.apache.lucene.analysis.nl KPSnowballStemTokenFilterFactory)
           (org.apache.lucene.collation CollationKeyAnalyzer)))

; https://lucene.apache.org/core/8_8_0/analyzers-common/constant-values.html#org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_WORD_PARTS

(set! *warn-on-reflection* true)

(defn namify
  "Normalizes analysis component name."
  [component-name]
  (str/lower-case component-name))

(def predefined-analyzers
  (reduce (fn [acc [analyzer-name analyzer]]
            (assoc acc (namify (str/replace analyzer-name "Analyzer" "")) analyzer))
          {}
          {"ArabicAnalyzer"            (ArabicAnalyzer.)
           "BulgarianAnalyzer"         (BulgarianAnalyzer.)
           "BengaliAnalyzer"           (BengaliAnalyzer.)
           "BrazilianAnalyzer"         (BrazilianAnalyzer.)
           "CatalanAnalyzer"           (CatalanAnalyzer.)
           "CJKAnalyzer"               (CJKAnalyzer.)
           "SoraniAnalyzer"            (SoraniAnalyzer.)
           "StopAnalyzer"              (StopAnalyzer. (CharArraySet. (ArrayList.) true))
           "CzechAnalyzer"             (CzechAnalyzer.)
           "DanishAnalyzer"            (DanishAnalyzer.)
           "DutchAnalyzer"             (DutchAnalyzer.)
           "GermanAnalyzer"            (GermanAnalyzer.)
           "GreekAnalyzer"             (GreekAnalyzer.)
           "SpanishAnalyzer"           (SpanishAnalyzer.)
           "EstonianAnalyzer"          (EstonianAnalyzer.)
           "BasqueAnalyzer"            (BasqueAnalyzer.)
           "PersianAnalyzer"           (PersianAnalyzer.)
           "FinnishAnalyzer"           (FinnishAnalyzer.)
           "FrenchAnalyzer"            (FrenchAnalyzer.)
           "IrishAnalyzer"             (IrishAnalyzer.)
           "GalicianAnalyzer"          (GalicianAnalyzer.)
           "HindiAnalyzer"             (HindiAnalyzer.)
           "HungarianAnalyzer"         (HungarianAnalyzer.)
           "ArmenianAnalyzer"          (ArmenianAnalyzer.)
           "IndonesianAnalyzer"        (IndonesianAnalyzer.)
           "ItalianAnalyzer"           (ItalianAnalyzer.)
           "LithuanianAnalyzer"        (LithuanianAnalyzer.)
           "LatvianAnalyzer"           (LatvianAnalyzer.)
           "NorwegianAnalyzer"         (NorwegianAnalyzer.)
           "PortugueseAnalyzer"        (PortugueseAnalyzer.)
           "RomanianAnalyzer"          (RomanianAnalyzer.)
           "RussianAnalyzer"           (RussianAnalyzer.)
           "ClassicAnalyzer"           (ClassicAnalyzer.)
           "UAX29URLEmailAnalyzer"     (UAX29URLEmailAnalyzer.)
           "SwedishAnalyzer"           (SwedishAnalyzer.)
           "ThaiAnalyzer"              (ThaiAnalyzer.)
           "TurkishAnalyzer"           (TurkishAnalyzer.)
           "EnglishAnalyzer"           (EnglishAnalyzer.)
           ;; add 13 MB to the binary
           "PolishAnalyzer"            (PolishAnalyzer.)
           "StandardAnalyzer"          (StandardAnalyzer.)
           "UnicodeWhitespaceAnalyzer" (UnicodeWhitespaceAnalyzer.)
           "WhitespaceAnalyzer"        (WhitespaceAnalyzer.)
           "KeywordAnalyzer"           (KeywordAnalyzer.)
           "SimpleAnalyzer"            (SimpleAnalyzer.)
           "CollationKeyAnalyzer"      (CollationKeyAnalyzer. (Collator/getInstance Locale/ENGLISH))}))

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

(def token-filter-name->class
  (assoc (reduce (fn [acc ^String token-filter-name]
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
    (namify "lovinsSnowballStem") LovinsSnowballStemTokenFilterFactory))

(def DEFAULT_TOKENIZER_NAME "standard")

(defn ^Analyzer create
  "Either fetches a predefined analyzer or creates one from the config."
  [{:keys [char-filters tokenizer token-filters analyzer]}]
  (or
    (get predefined-analyzers (when-let [n (get analyzer :name)] (namify (str/replace n "Analyzer" ""))))
    (try
      (let [^CustomAnalyzer$Builder cab (CustomAnalyzer/builder)]
        (.withTokenizer cab
                        ^Class
                        (get tokenizer-name->class (namify (get tokenizer :name DEFAULT_TOKENIZER_NAME)))
                        ^Map (HashMap. ^Map (stringify (get tokenizer :args))))

        (doseq [char-filter char-filters]
          (.addCharFilter cab
                          ^Class (get char-filter-name->class (namify (get char-filter :name)))
                          ^Map (HashMap. ^Map (stringify (get char-filter :args)))))

        (doseq [token-filter token-filters]
          (.addTokenFilter cab
                           ^Class (get token-filter-name->class (namify (get token-filter :name)))
                           ^Map (HashMap. ^Map (stringify (get token-filter :args)))))

        (.build cab))
      (catch Exception e
        (.println System/err (.getMessage e))
        (throw e)))))

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
