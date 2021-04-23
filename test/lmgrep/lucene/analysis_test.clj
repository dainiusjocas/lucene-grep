(ns lmgrep.lucene.analysis-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [lmgrep.lucene.analyzer :as analysis]
            [lmgrep.lucene.text-analysis :as ta]))

(deftest predefined-analyzers
  (let [text "The brown foxes"
        analyzer (analysis/create {:analyzer {:name "EnglishAnalyzer"}})]
    (is (= ["brown" "fox"] (ta/text->token-strings text analyzer))))
  (let [text "The brown foxes"
        analyzer (analysis/create {:analyzer {:name "CollationKeyAnalyzer"}})]
    (is (= ["The brown foxes"] (ta/text->token-strings text analyzer)))))

(deftest analysis-construction-from-components
  (let [text "The quick brown fox"
        analyzer (analysis/create
                   {:tokenizer {:name "standard"}
                    :token-filters [{:name "uppercase"}]})]
    (is (= ["THE" "QUICK" "BROWN" "FOX"]
           (ta/text->token-strings text analyzer)))

    (testing "specifying tokenizer is optional"
      (let [analyzer (analysis/create {:token-filters [{:name "uppercase"}]})]
        (is (= ["THE" "QUICK" "BROWN" "FOX"]
               (ta/text->token-strings text analyzer)))))

    (testing "specifying empty lists works as expected"
      (let [analyzer (analysis/create {:tokenizer {}
                                       :token-filters []
                                       :char-filters []})]
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
                                                    :args {"generateWordParts" 1
                                                           "generateNumberParts" 1
                                                           "preserveOriginal" 1
                                                           "splitOnCaseChange" 1}}]})]
    (is (= ["TestClass" "Test" "Class"] (ta/text->token-strings text analyzer)))))

(deftest lithuanian-snowball-stemmer-token-filter-factory
  (let [text "lietus lyja"
        analyzer (analysis/create {:token-filters [{:name "lithuanianSnowballStem"}]})]
    (is (= ["liet" "lyj"] (ta/text->token-strings text analyzer)))))

(deftest try-all-predefined-analyzers
  (let [text "cats and dogs"
        analyzer-names (keys analysis/predefined-analyzers)]
    (is (seq analyzer-names))
    (doseq [an analyzer-names]
      (try
        (let [analyzer (analysis/create {:analyzer {:name an}})]
          (is (seq (ta/text->token-strings text analyzer))))
        (catch Exception e
          (println (format "Failed analyzer: '%s'" an))
          (.printStackTrace e))))))

(deftest try-all-char-filters
  (let [text "cats and dogs"
        with-required-params #{"patternreplace"}
        char-filter-names (remove (fn [cfn] (contains? with-required-params cfn))
                                  (keys analysis/char-filter-name->class))]
    (is (seq char-filter-names))
    (doseq [char-filter-name char-filter-names]
      (try
        (let [analyzer (analysis/create {:char-filters [{:name char-filter-name}]})]
          (is (seq (ta/text->token-strings text analyzer))))
        (catch Exception e
          (println (format "Failed char filter name: '%s' class: '%s'"
                           char-filter-name
                           (get analysis/char-filter-name->class char-filter-name)))
          (throw e))))))

(deftest try-all-tokenizers
  (let [text "cats and dogs"
        components analysis/tokenizer-name->class
        with-required-params #{"simplepatternsplit" "simplepattern" "pattern"}
        tokenizer-names (remove (fn [tn] (contains? with-required-params tn))
                                (keys components))]
    (is (seq tokenizer-names))
    (doseq [tokenizer-name tokenizer-names]
      (try
        (let [analyzer (analysis/create {:tokenizer {:name tokenizer-name}})]
          (is (seq (ta/text->token-strings text analyzer))))
        (catch Exception e
          (println (format "Failed tokenizer name: '%s' class: '%s'"
                           tokenizer-name (get components tokenizer-name)))
          (throw e))))))

(deftest try-all-token-filters
  (let [text "cats and dogs"
        components analysis/token-filter-name->class
        with-required-params #{"synonym" "limittokencount" "delimitedpayload" "dictionarycompoundword"
                               "numericpayload" "hunspellstem" "edgengram" "patterncapturegroup"
                               "hyphenationcompoundword" "length" "type" "synonymgraph" "limittokenoffset"
                               "protectedterm" "limittokenposition" "patternreplace" "ngram" "codepointcount"}
        token-filter-name (remove (fn [tn] (contains? with-required-params tn))
                                  (keys components))]
    (is (seq token-filter-name))
    (doseq [tokenizer-name token-filter-name]
      (try
        (let [analyzer (analysis/create {:token-filters [{:name tokenizer-name}]})]
          (is (vector? (ta/text->token-strings text analyzer))))
        (catch Exception e
          (println (format "Failed token filter name: '%s' class: '%s'"
                           tokenizer-name (get components tokenizer-name)))
          (throw e))))))
