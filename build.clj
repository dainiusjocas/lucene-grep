(ns build
  (:require [clojure.tools.cli.api :as c]))

(defn prep-deps [& _]
  (println "Preparing transitive dependencies")
  (c/prep (assoc (c/basis {:aliases [:raudikko]}) :force true))
  (c/prep (assoc (c/basis {:aliases [:snowball-token-filters]}) :force true)))
