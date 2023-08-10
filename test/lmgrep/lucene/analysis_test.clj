(ns lmgrep.lucene.analysis-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [jsonista.core :as json]
            [lmgrep.lucene.analyzer :as analysis]
            [lucene.custom.text-analysis :as ta]
            [lmgrep.predefined-analyzers :as predefined]))

(defn with-analyzers [opts]
  (analysis/create opts predefined/analyzers))

(def write-to-disk? (= "true" (System/getenv "LMGREP_WRITE_TEST_DATA")))

(deftest converter
  (let [conf {:tokenizer     {:name "standard"
                              :args {:maxTokenLength 4}}
              :char-filters  [{:name "patternReplace"
                               :args {:pattern     "foo"
                                      :replacement "foo"}}]
              :token-filters [{:name "uppercase"}
                              {:name "reverseString"}]}

        expected {:tokenizer     {"standard" {:maxTokenLength 4}}
                  :char-filters  [{"patternReplace" {:pattern     "foo"
                                                     :replacement "foo"}}]
                  :token-filters [{"uppercase" nil}
                                  {"reverseString" nil}]}]
    (is (= expected (analysis/custom-analyzer->short-notation conf)))))

(deftest analysis-construction-from-components
  (let [text "The quick brown fox"
        analyzer (analysis/create
                   {:tokenizer     {:name "standard"}
                    :token-filters [{:name "uppercase"}]})]
    (is (= ["THE" "QUICK" "BROWN" "FOX"]
           (ta/text->token-strings text analyzer)))

    (testing "specifying tokenizer is optional"
      (let [analyzer (analysis/create {:token-filters [{:name "uppercase"}]})]
        (is (= ["THE" "QUICK" "BROWN" "FOX"]
               (ta/text->token-strings text analyzer)))))

    (testing "specifying empty lists works as expected"
      (let [analyzer (analysis/create {:tokenizer     {}
                                       :token-filters []
                                       :char-filters  []})]
        (is (= ["The" "quick" "brown" "fox"]
               (ta/text->token-strings text analyzer)))))

    (testing "providing an empty conf returns proper analyzer"
      (let [analyzer (analysis/create {})]
        (is (= ["The" "quick" "brown" "fox"]
               (ta/text->token-strings text analyzer)))))

    (testing "if char filters work"
      (let [text "<title>foo</title>"
            analyzer (analysis/create {:char-filters [{:name "htmlStrip"}]})]
        (is (= ["foo"] (ta/text->token-strings text analyzer)))))

    (testing "stopwords token filter with default word list"
      (let [analyzer (analysis/create {:token-filters [{:name "lowercase"}
                                                       {:name "stop"}]})]
        (is (= ["quick" "brown" "fox"] (ta/text->token-strings text analyzer)))))

    (testing "stopwords token filter from a custom file"
      (let [text "my foo bar baz text"
            analyzer (analysis/create {:token-filters [{:name "lowercase"}
                                                       {:name "stop"
                                                        :args {:words "test/resources/stops.txt"}}]})]
        (is (= ["my" "text"] (ta/text->token-strings text analyzer)))))

    (testing "MappingCharFilter file resources"
      (let [text "my foo bar baz text"
            analyzer (analysis/create {:char-filters [{:name "mapping"
                                                       :args {:mapping "test/resources/mapping.txt"}}]})]
        (is (= ["my" "bar" "bar" "baz" "text"] (ta/text->token-strings text analyzer)))))

    (testing "order of token filters"
      (let [text "brown foxes"
            analyzer (analysis/create {:token-filters [{:name "uppercase"}
                                                       {:name "porterStem"}]})]
        (is (= ["BROWN" "FOXES"] (ta/text->token-strings text analyzer))))

      (let [text "brown foxes"
            analyzer (analysis/create {:token-filters [{:name "porterStem"}
                                                       {:name "uppercase"}]})]
        (is (= ["BROWN" "FOX"] (ta/text->token-strings text analyzer)))))

    (testing "case sensitivity of a component name"
      (let [text "brown foxes"
            porter-stemmer-name "porterStem"
            lowercased-ported-stemmer-name (str/lower-case porter-stemmer-name)
            analyzer-1 (analysis/create {:token-filters [{:name porter-stemmer-name}]})
            analyzer-2 (analysis/create {:token-filters [{:name lowercased-ported-stemmer-name}]})]
        (is (not= porter-stemmer-name lowercased-ported-stemmer-name))
        (is (= (ta/text->token-strings text analyzer-1) (ta/text->token-strings text analyzer-2)))))))

(deftest tokenizer-tests
  (let [text "The quick brown fox"
        analyzer (analysis/create {:tokenizer {:name "edgengram"
                                               :args {:maxGramSize 5}}})]
    (is (= ["T" "Th" "The" "The " "The q"] (ta/text->token-strings text analyzer)))))

(deftest word-delimiter-graph
  (let [text "TestClass"
        analyzer (analysis/create {:token-filters [{:name "worddelimitergraph"
                                                    :args {"generateWordParts"   1
                                                           "generateNumberParts" 1
                                                           "preserveOriginal"    1
                                                           "splitOnCaseChange"   1}}]})]
    (is (= ["TestClass" "Test" "Class"] (ta/text->token-strings text analyzer)))))

(deftest try-all-predefined-analyzers
  (let [text "cats and dogs"
        analyzer-names (keys predefined/analyzers)]
    (is (seq analyzer-names))
    (doseq [an analyzer-names]
      (try
        (let [analyzer (with-analyzers {:analyzer {:name an}})]
          (is (seq (ta/text->token-strings text analyzer)))
          (when write-to-disk?
            (let [file-name (format "test/resources/binary/analyzers/%s.json" an)]
              (io/make-parents file-name)
              (spit file-name (json/write-value-as-string {:analyzer {:name an}})))))
        (catch Exception e
          (println (format "Failed analyzer: '%s'" an))
          (.printStackTrace e))))))

(deftest try-all-char-filters
  (let [text "cats and dogs"
        char-filter-names (keys analysis/char-filter-name->class)
        args {"patternreplace" {"pattern" "foo"}}]
    (is (seq char-filter-names))
    (doseq [char-filter-name char-filter-names]
      (try
        (let [conf {:char-filters [(if-let [a (get args char-filter-name)]
                                     {:name char-filter-name :args a}
                                     {:name char-filter-name})]}
              analyzer (analysis/create conf)]
          (is (seq (ta/text->token-strings text analyzer))
              (when write-to-disk?
                (let [file-name (format "test/resources/binary/charfilters/%s.json" char-filter-name)]
                  (io/make-parents file-name)
                  (spit file-name (json/write-value-as-string conf))))))
        (catch Exception e
          (println (format "Failed char filter name: '%s' class: '%s'"
                           char-filter-name
                           (get analysis/char-filter-name->class char-filter-name)))
          (throw e))))))

(deftest try-all-tokenizers
  (let [text "cats and dogs"
        components analysis/tokenizer-name->class
        tokenizer-names (keys components)
        args {"simplepatternsplit" {"pattern" " "}
              "simplepattern"      {"pattern" " "}
              "pattern"            {"pattern" " "}
              "opennlp"            {"sentenceModel" "test/resources/opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin"
                                    "tokenizerModel" "test/resources/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin"}}]
    (is (seq tokenizer-names))
    (doseq [tokenizer-name tokenizer-names]
      (try
        (let [conf {:tokenizer (if-let [a (get args tokenizer-name)]
                                 {:name tokenizer-name :args a}
                                 {:name tokenizer-name})}
              analyzer (analysis/create conf)]
          (is (seq (ta/text->token-strings text analyzer)))
          (when write-to-disk?
            (let [file-name (format "test/resources/binary/tokenizers/%s.json" tokenizer-name)]
              (io/make-parents file-name)
              (spit file-name (json/write-value-as-string conf)))))
        (catch Exception e
          (println (format "Failed tokenizer name: '%s' class: '%s'"
                           tokenizer-name (get components tokenizer-name)))
          (throw e))))))

(deftest try-all-token-filters
  (let [text "cats and dogs"
        components analysis/token-filter-name->class
        token-filter-names (keys components)
        args {"limittokencount"         {"maxTokenCount" 5}
              "delimitedpayload"        {"encoder" "float"}
              "limittokenoffset"        {"maxStartOffset" 5}
              "length"                  {"min" 1 "max" 5}
              "type"                    {"types" "test/resources/stops.txt"}
              "ngram"                   {"minGramSize" 1
                                         "maxGramSize" 5}
              "protectedterm"           {"protected" "test/resources/stops.txt"}
              "edgengram"               {"minGramSize" 1
                                         "maxGramSize" 5}
              "limittokenposition"      {"maxTokenPosition" 2}
              "codepointcount"          {"min" 1 "max" 5}
              "numericpayload"          {"payload"   24
                                         "typeMatch" "word"}
              "patternreplace"          {"pattern" " "}
              "patterncapturegroup"     {"pattern" " "}
              "patterntyping"           {"patternFile" "test/resources/pattern.txt"}
              "dictionarycompoundword"  {"dictionary" "test/resources/stops.txt"}
              "synonymgraph"            {"synonyms" "test/resources/mapping.txt"}
              "synonym"                 {"synonyms" "test/resources/mapping.txt"}
              "hyphenationcompoundword" {"hyphenator" "test/resources/hyphenation_hyphenator.xml"}
              "hunspellstem"            {"dictionary" "test/resources/hunspell_dict.dic"
                                         "affix"      "test/resources/hunspell_dict.aff"}
              "dropifflagged"           {"dropFlags" "2"}
              "word2vecsynonym"         {"model" "test/resources/word2vec-model.zip"}
              "opennlppos"              {"posTaggerModel" "test/resources/opennlp-en-ud-ewt-pos-1.0-1.9.3.bin"}
              "opennlplemmatizer"       {"dictionary" "test/resources/en-lemmatizer.dict"}
              "opennlpchunker"          {"chunkerModel" "test/resources/en-chunker.bin"}}]
    (is (seq token-filter-names))
    (doseq [token-filter-name token-filter-names]
      (try
        (let [analyzer-conf {:token-filters [(if-let [a (get args token-filter-name)]
                                               {:name token-filter-name :args a}
                                               {:name token-filter-name})]}
              analyzer (analysis/create analyzer-conf)]
          (is (vector? (ta/text->token-strings text analyzer)))
          (when write-to-disk?
            (let [file-name (format "test/resources/binary/tokenfilters/%s.json" token-filter-name)]
              (io/make-parents file-name)
              (spit file-name (json/write-value-as-string analyzer-conf)))))
        (catch Exception e
          (println (format "Failed token filter name: '%s' class: '%s'"
                           token-filter-name (get components token-filter-name)))
          (throw e))))))
