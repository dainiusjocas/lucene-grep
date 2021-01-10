(ns core
  (:require [beagle.phrases :as phrases])
  (:gen-class))

(defn highlighted-text [text]
  (str \ "[1;31m" text \"[0m"))

(defn match [query-string file-path]
  (let [dictionary [{:text            query-string
                     :id              "1"
                     :case-sensitive? false
                     :ascii-fold?     true
                     :stem?           true
                     :stemmer         (keyword "english")}]]
    (with-open [rdr (clojure.java.io/reader file-path)]
      (doseq [[line-str line-number] (map (fn [line-str line-number] [line-str line-number])
                                          (line-seq rdr) (range))]
        (let [highlighter-fn (phrases/highlighter dictionary)]
          (when-let [[first-highlight & _] (seq (highlighter-fn line-str))]
            (println
              (format "%s  %s%s%s"
                      line-number
                      (subs line-str 0 (:begin-offset first-highlight))
                      (highlighted-text (:text first-highlight))
                      (subs line-str (:end-offset first-highlight))))))))))

(defn -main [& args]
  (match (nth args 0) (nth args 1))
  (System/exit 0))
