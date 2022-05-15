(ns lmgrep.no-split-test
  (:require [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [lmgrep.lucene :as lucene]
            [lmgrep.no-split :as no-split]))

(deftest one-file-at-a-time
  (let [files ["test/resources/test.txt"]
        highlighter-fn (lucene/highlighter [{:query "dog"}] {} {})]
    (is (seq
          (json/read-value
            (with-out-str (no-split/grep files highlighter-fn {:format :json}))
            json/keyword-keys-object-mapper)))))
