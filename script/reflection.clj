(ns reflection
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def input-file "graalvm/reflect-config.json")
(def output-file "graalvm/lucene-reflect-config.json")

(defn gen []
  (let [my-pretty-printer (json/create-pretty-printer
                            (assoc json/default-pretty-print-options
                              :indent-arrays? true))]
    (spit output-file
          (json/generate-string
            (->> input-file
                 (io/reader)
                 (json/parse-stream)
                 (filter (fn [entry]
                           (or (re-matches #"org.apache.*" (get entry "name"))
                               (re-matches #"org.tartarus.*" (get entry "name")))))
                 (map (fn [entry]
                        (assoc entry "allDeclaredConstructors" true))))
            {:pretty my-pretty-printer}))))
