(ns lmgrep.lucene.custom-analyzer-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.lucene.custom-analyzer :as a])
  (:import (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.analysis.custom CustomAnalyzer)
           (org.apache.lucene.analysis.standard StandardTokenizerFactory)
           (org.apache.lucene.analysis.core KeywordTokenizerFactory LowerCaseFilterFactory)
           (org.apache.lucene.analysis.charfilter HTMLStripCharFilterFactory)
           (org.apache.lucene.analysis.miscellaneous ASCIIFoldingFilterFactory)))

(deftest analyzer-building
  (testing "tokenizer params handling"
    (let [analyzer (a/create {:tokenizer {:name "standard"}})]
      (is (instance? Analyzer analyzer))
      (is (instance? CustomAnalyzer analyzer))
      (is (= StandardTokenizerFactory
             (class (.getTokenizerFactory ^CustomAnalyzer analyzer)))))

    (let [analyzer (a/create {:tokenizer {:name "keyword"}})]
      (is (= KeywordTokenizerFactory
             (class (.getTokenizerFactory ^CustomAnalyzer analyzer))))))

  (testing "char filter params handling"
    (let [analyzer (a/create {:char-filters [{:name "htmlStrip"}]})]
      (is (= 1 (count (.getCharFilterFactories ^CustomAnalyzer analyzer))))
      (is (= HTMLStripCharFilterFactory
             (class (first (.getCharFilterFactories ^CustomAnalyzer analyzer))))))

    (let [analyzer (a/create {:char-filters [{:name "htmlStrip"} {:name "htmlStrip"}]})]
      (is (= 2 (count (.getCharFilterFactories ^CustomAnalyzer analyzer))))
      (is (= [HTMLStripCharFilterFactory HTMLStripCharFilterFactory]
             (map #(class %) (.getCharFilterFactories ^CustomAnalyzer analyzer))))))

  (testing "token filter params handling"
    (let [analyzer (a/create {:token-filters [{:name "asciiFolding"}]})]
      (is (= 1 (count (.getTokenFilterFactories ^CustomAnalyzer analyzer))))
      (is (= [ASCIIFoldingFilterFactory]
             (map #(class %) (.getTokenFilterFactories ^CustomAnalyzer analyzer)))))

    (let [analyzer (a/create {:token-filters [{:name "asciiFolding"} {:name "lowercase"}]})]
      (is (= 2 (count (.getTokenFilterFactories ^CustomAnalyzer analyzer))))
      (is (= [ASCIIFoldingFilterFactory LowerCaseFilterFactory]
             (map #(class %) (.getTokenFilterFactories ^CustomAnalyzer analyzer))))))

  (testing "gap param handling"
    (let [^CustomAnalyzer analyzer (a/create {:offset-gap 12
                                              :position-increment-gap 3})]
      (is (= 12 (.getOffsetGap analyzer "")))
      (is (= 3 (.getPositionIncrementGap analyzer ""))))))
