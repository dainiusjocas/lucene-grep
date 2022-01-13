(ns lmgrep.lucene.analysis-conf
  (:require [clojure.tools.logging :as log]
            [lmgrep.lucene.analysis-components :as ac]))

(def analysis-keys #{:case-sensitive?
                     :ascii-fold?
                     :stem?
                     :tokenizer
                     :stemmer
                     :word-delimiter-graph-filter})

(def default-text-analysis
  {:tokenizer {:name "standard"}
   :token-filters [{:name "lowercase"}
                   {:name "asciifolding"}
                   {:name "englishMinimalStem"}]})

(defn override-token-filters [token-filters flags]
  (cond->> token-filters
           (true? (get flags :case-sensitive?))
           (remove (fn [tf] (= "lowercase" (name (get tf :name)))))
           (false? (get flags :ascii-fold?))
           (remove (fn [tf] (= "asciifolding" (name (get tf :name)))))
           (false? (get flags :stem?))
           (remove (fn [tf] (re-matches #".*[Ss]tem.*" (name (get tf :name)))))
           (keyword? (keyword (get flags :stemmer)))
           ((fn [token-filters]
              (conj (into [] (remove (fn [token-filter]
                                       (re-matches #".*[Ss]tem.*" (name (get token-filter :name))))
                                     token-filters))
                    {:name (let [stemmer-kw (keyword (get flags :stemmer))]
                             (get ac/stemmer
                                  stemmer-kw
                                  (do
                                    (when stemmer-kw
                                      (log/debugf "Stemmer '%s' not found! EnglishStemmer is used." stemmer-kw))
                                    "englishMinimalStem")))})))
           (pos-int? (get flags :word-delimiter-graph-filter))
           (cons {:name "worddelimitergraph"
                  :args (ac/wdgf->token-filter-args
                          (get flags :word-delimiter-graph-filter))})))

(defn override-acm [acm flags]
  (let [tokenizer (or (when-let [tokenizer-kw (get flags :tokenizer)]
                        (get ac/tokenizer
                             tokenizer-kw
                             (do
                               (when tokenizer-kw
                                 (log/debugf "Tokenizer '%s' not found. StandardTokenizer is used." tokenizer-kw))
                               {:name "standard"})))
                      (:tokenizer acm))
        token-filters (override-token-filters (get acm :token-filters) flags)]
    (assoc acm
      :tokenizer tokenizer
      :token-filters token-filters)))

(defn prepare-analysis-configuration
  "When analysis key is not provided then construct analysis config
  by overriding default-text-analysis with provided analysis-flags if any.
  Given the default text analysis config appl"
  [default-text-analysis options]
  (if (empty? (get options :analysis))
    (let [analysis-flags (select-keys options analysis-keys)]
      (if (empty? analysis-flags)
        default-text-analysis
        (override-acm default-text-analysis analysis-flags)))
    (get options :analysis)))
