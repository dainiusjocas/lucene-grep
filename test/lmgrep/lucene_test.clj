(ns lmgrep.lucene-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [lmgrep.lucene.dictionary :as dictionary]
            [lmgrep.lucene :as lucene]
            [lmgrep.grep :as grep]
            [lmgrep.lucene :as lucene]
            [lmgrep.lucene.text-analysis :as text-analysis]))

(deftest highlighter-details
  (testing "simple case"
    (let [text "foo text bar"
          query "text"]
      (is (= [{:text "text" :type "QUERY" :dict-entry-id "0"
               :meta {} :begin-offset 4 :end-offset 8}]
             ((lucene/highlighter [{:text query}] {}) text)))))

  (testing "word delimiter"
    (let [text "foo text bar BestClass fooo name"
          query "best class"
          dictionary [(merge
                        dictionary/default-text-analysis
                        {:text                        query
                         :word-delimiter-graph-filter (+ 1 2 32 64)})]]
      (is (= [{:begin-offset  13
               :dict-entry-id "0"
               :end-offset    22
               :meta          {}
               :text          "best class"
               :type          "QUERY"}
              {:begin-offset  13
               :dict-entry-id "0"
               :end-offset    22
               :meta          {}
               :text          "best class"
               :type          "QUERY"}]
             ((lucene/highlighter dictionary {}) text))))))

(deftest tokenizing-multiple-lines-from-stdin
  (let [text-from-stdin "foo bar \nbaz quux"
        analyzer (text-analysis/analyzer-constructor {})]
    (is (= "[\"foo\",\"bar\"]\n[\"baz\",\"quux\"]\n"
           (with-in-str text-from-stdin
                        (with-out-str
                          (grep/analyze-text nil analyzer)))))))

(deftest tokenizing-multiple-lines-from-file
  (let [file-path "test/resources/test.txt"
        analyzer (text-analysis/analyzer-constructor {})]
    (is (= 2 (count
               (str/split-lines
                 (str/trim
                   (with-out-str
                     (grep/analyze-text file-path analyzer)))))))))
