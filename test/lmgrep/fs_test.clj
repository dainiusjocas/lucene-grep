(ns lmgrep.fs-test
  (:require [clojure.test :refer [deftest is]]
            [lmgrep.fs :as fs]))

(deftest filter-for-files
  (let [files ["src" "deps.edn"]]
    (is (= ["deps.edn"] (fs/filter-files files)))))

(deftest binary-file-shipping
  (when (contains? #{"Linux" "Mac OS X"} (System/getProperty "os.name"))
    ; NOTE: Windows doesn't support this feature
    ; All .png files in docs should be binary
    (is (= [] (fs/get-files "docs/**.png"
                            {:skip-binary-files true})))))
