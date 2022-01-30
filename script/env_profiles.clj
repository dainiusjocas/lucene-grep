(ns script.env-profiles
  (:require [clojure.edn :as e]))

(defn profiles
  "Checks the relevant environment variables and returns a list of alias keywords."
  []
  (let [env->alias (->> (:aliases (e/read-string (slurp "deps.edn")))
                        (map (fn [[k v]] [k (:env v)]))
                        (remove (fn [[_ v]] (nil? v)))
                        (map (fn [[k v]] [v k]))
                        (into {}))]
    (reduce (fn [acc [env-var alias]]
              (if (Boolean/valueOf (System/getenv env-var))
                (conj acc alias)
                acc))
            []
            env->alias)))
