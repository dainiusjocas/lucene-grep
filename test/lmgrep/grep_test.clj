(ns lmgrep.grep-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.grep :as grep]
            [beagle.phrases :as phrases]))

(deftest highlighting-test
  (testing "coloring the output"
    (let [query-string "text"
          highlighter-fn (phrases/highlighter [{:text query-string}])
          text "prefix text suffix"]
      (is (= (str "prefix " \ "[1;31mtext" \ "[0m suffix")
             (grep/highlight-line text (highlighter-fn text) {}))))))
