(ns lmgrep.fs-test
  (:require [clojure.test :refer [deftest is]]
            [lmgrep.fs :as fs]))

(deftest filter-for-files
  (let [files ["src" "deps.edn"]]
    (is (= ["deps.edn"] (fs/filter-files files)))))
