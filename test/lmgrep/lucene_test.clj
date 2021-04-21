(ns lmgrep.lucene-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [lmgrep.grep :as grep]
            [lmgrep.lucene :as lucene]
            [lmgrep.formatter :as formatter]
            [lmgrep.lucene.analyzer :as analyzer]))

(deftest highlighting-test
  (testing "coloring the output"
    (let [query-string "text"
          highlighter-fn (lucene/highlighter [{:query query-string}])
          text "prefix text suffix"]
      (is (= (str "prefix " \ "[1;31mtext" \ "[0m suffix")
             (formatter/highlight-line text (highlighter-fn text) {}))))))

(deftest highlighter-details
  (testing "simple case"
    (let [text "foo text bar"
          query "text"]
      (is (= [{:query "text" :type "QUERY" :dict-entry-id "0"
               :meta {"foo" "bar"} :begin-offset 4 :end-offset 8}]
             ((lucene/highlighter [{:query query :id "0" :meta {:foo "bar"}}]) text))))))

(deftest word-delimiter-highlights
  (testing "word delimiter"
    (let [text "foo text bar BestClass fooo name"
          query "best class"
          dictionary [{:query                       query
                       :id                          "0"
                       :case-sensitive?             false
                       :ascii-fold?                 true
                       :stem?                       true
                       :tokenizer                   :standard
                       :stemmer                     :english
                       :word-delimiter-graph-filter (+ 1 2 32 64)}]]
      (is (= [{:begin-offset  13
               :dict-entry-id "0"
               :end-offset    17
               :meta          {}
               :query         "best class"
               :type          "QUERY"}
              {:begin-offset  17
               :dict-entry-id "0"
               :end-offset    22
               :meta          {}
               :query         "best class"
               :type          "QUERY"}]
             ((lucene/highlighter dictionary {}) text))))))

(deftest only-analysis
  (testing "file input"
    (let [file-path "test/resources/test.txt"
          options {}]
      (is (= 2 (count
                 (str/split-lines
                   (str/trim
                     (with-out-str
                       (grep/analyze-lines file-path nil options)))))))))
  (testing "multiple lines from stdin"
    (let [text-from-stdin "foo bar \nbaz quux"
          analyzer {}]
      (is (= "[\"foo\",\"bar\"]\n[\"baz\",\"quux\"]\n"
             (with-in-str text-from-stdin
                          (with-out-str
                            (grep/analyze-lines nil nil analyzer))))))))
