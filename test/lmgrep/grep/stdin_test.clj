(ns lmgrep.grep.stdin-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [lmgrep.grep.stdin :as stdin]
            [lmgrep.lucene :as lucene]))

(deftest grepping-stdin
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "fox"
        options {:split true
                 :pre-tags ">" :post-tags "<"
                 :template "{{highlighted-line}}"
                 :preserve-order false}
        highlighter-fn (lucene/highlighter [{:query query}] options)]
    (is (= "The quick brown >fox< jumps over the lazy dog"
           (with-in-str text-from-stdin
                        (str/trim
                          (with-out-str
                            (stdin/grep highlighter-fn options))))))))
