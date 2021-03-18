(ns lmgrep.grep-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [lmgrep.formatter :as formatter]
            [lmgrep.grep :as grep]
            [lmgrep.lucene :as lucene]))

(deftest highlighting-test
  (testing "coloring the output"
    (let [query-string "text"
          highlighter-fn (lucene/highlighter [{:text query-string}])
          text "prefix text suffix"]
      (is (= (str "prefix " \ "[1;31mtext" \ "[0m suffix")
             (formatter/highlight-line text (highlighter-fn text) {}))))))

(deftest grepping-file
  (let [file "test/resources/test.txt"
        query "fox"
        options {:split true :pre-tags ">" :post-tags "<" :template "{{highlighted-line}}"}]
    (is (= "The quick brown >fox< jumps over the lazy dog"
           (str/trim
             (with-out-str
               (grep/grep [query] file nil options)))))))

(deftest grepping-stdin
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "fox"
        options {:split true :pre-tags ">" :post-tags "<" :template "{{highlighted-line}}"}]
    (is (= "The quick brown >fox< jumps over the lazy dog"
           (with-in-str text-from-stdin
                        (str/trim
                          (with-out-str
                            (grep/grep [query] nil nil options))))))))

(deftest grepping-multiple-queries
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        queries ["fox" "dog"]
        options {:split true :pre-tags ">" :post-tags "<" :template "{{highlighted-line}}"}]
    (is (= "The quick brown >fox< jumps over the lazy >dog<"
           (with-in-str text-from-stdin
                        (str/trim
                          (with-out-str
                            (grep/grep queries nil nil options))))))))

(deftest grepping-multiple-queries-from-file
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        queries []
        options {:split true
                 :pre-tags ">"
                 :post-tags "<"
                 :template "{{highlighted-line}}"
                 :queries-file "test/resources/queries.json"}]
    (is (= "The quick brown >fox< jumps over the lazy >dog<"
           (with-in-str text-from-stdin
                        (str/trim
                          (with-out-str
                            (grep/grep queries nil nil options))))))))

(deftest grepping-when-no-match-with-flag-to-println-empty-line
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "foo"
        options {:split true
                 :pre-tags ">"
                 :post-tags "<"
                 :with-empty-lines true
                 :template "{{highlighted-line}}"}]
    (is (= "\n" (with-in-str text-from-stdin
                             (with-out-str
                               (grep/grep [query] nil nil options)))))))
