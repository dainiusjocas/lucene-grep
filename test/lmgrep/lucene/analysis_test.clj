(ns lmgrep.lucene.analysis-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [lmgrep.lucene.analyzer :as analysis]
            [lmgrep.lucene.text-analysis :as ta]))

(deftest predefined-analyzers
  (let [text "The brown foxes"
        analyzer (analysis/create {:analyzer {:name "EnglishAnalyzer"}})]
    (is (= ["brown" "fox"] (ta/text->token-strings text analyzer)))))

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
                                                        :args {:words "test/resources/stops.txt"
                                                               :foo "bar"}}]})]
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
