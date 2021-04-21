(ns lmgrep.lucene.field-name
  (:require [clojure.string :as str]))

(defn component-name-str [component]
  (str (get component :name) "-" (hash (get component :args))))

(defn construct
  "If analyzer is specified then analyzer determines the name of the field"
  [analysis-conf]
  (let [analyzer-name (when-let [analyzer (get analysis-conf :analyzer)]
                        (component-name-str analyzer))
        tokenizer-name (str (or (when-let [tokenizer (get analysis-conf :tokenizer)]
                                  (component-name-str tokenizer))
                                "standard")
                            "-tokenizer")
        char-filters (str/join "." (mapv component-name-str (get analysis-conf :char-filters)))
        token-filters (str/join "." (mapv component-name-str (get analysis-conf :token-filters)))
        suffix (str/join "." (remove str/blank? [char-filters tokenizer-name token-filters]))]
    (str "text" "." (or analyzer-name suffix))))
