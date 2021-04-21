(ns lmgrep.lucene.analysis-conf
  (:require [lmgrep.lucene.analysis-components :as ac]))

(def analysis-keys #{:case-sensitive?
                     :ascii-fold?
                     :stem?
                     :tokenizer
                     :stemmer
                     :word-delimiter-graph-filter})

(def default-text-analysis
  {:tokenizer nil
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
           ((fn [tfs]
              (conj (into [] (remove (fn [tf] (re-matches #".*[Ss]tem.*" (name (get tf :name))))
                                     tfs))
                    {:name (ac/stemmer (keyword (get flags :stemmer)))})))
           (pos-int? (get flags :word-delimiter-graph-filter))
           (cons {:name "worddelimitergraph"
                  :args (ac/wdgf->token-filter-args
                          (get flags :word-delimiter-graph-filter))})))

(defn override-acm [acm flags]
  (let [tokenizer (or (when-let [tokenizer-kw (get flags :tokenizer)]
                        (ac/tokenizer tokenizer-kw))
                      (:tokenizer acm))
        token-filters (override-token-filters (get acm :token-filters) flags)]
    (assoc acm
      :tokenizer tokenizer
      :token-filters token-filters)))

(defn prepare-analysis-configuration [default-text-analysis options]
  (if (empty? (get options :analysis))
    (let [analysis-flags (select-keys options analysis-keys)]
      (if (empty? analysis-flags)
        default-text-analysis
        (override-acm default-text-analysis analysis-flags)))
    (get options :analysis)))
