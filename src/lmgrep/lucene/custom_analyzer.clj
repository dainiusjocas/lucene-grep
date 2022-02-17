(ns lmgrep.lucene.custom-analyzer
  (:import (java.util HashMap Map)
           (java.io File)
           (java.nio.file Path)
           (org.apache.lucene.analysis.custom CustomAnalyzer CustomAnalyzer$Builder)
           (org.apache.lucene.analysis Analyzer TokenizerFactory CharFilterFactory TokenFilterFactory)))

(defn stringify [m]
  (reduce-kv (fn [acc k v] (assoc acc (name k) (str v))) {} m))

(def tokenizer-name->class
  (reduce (fn [acc ^String tokenizer-name]
            (assoc acc tokenizer-name (TokenizerFactory/lookupClass tokenizer-name)))
          {} (TokenizerFactory/availableTokenizers)))

(def char-filter-name->class
  (reduce (fn [acc ^String char-filter-name]
            (assoc acc char-filter-name (CharFilterFactory/lookupClass char-filter-name)))
          {} (CharFilterFactory/availableCharFilters)))

(def token-filter-name->class
  (reduce (fn [acc ^String token-filter-name]
            (assoc acc token-filter-name (TokenFilterFactory/lookupClass token-filter-name)))
          {} (TokenFilterFactory/availableTokenFilters)))

(def DEFAULT_TOKENIZER_NAME "standard")

(defn ^Path config-dir->path [config-dir]
  (let [^String dir (or config-dir ".")]
    (.toPath (File. dir))))

(defn get-component-or-exception [factories name component-type namify-fn]
  (or (get factories (namify-fn name))
      (throw
        (Exception.
          (format "%s '%s' is not available. Choose one of: %s"
                  component-type
                  name
                  (sort (keys factories)))))))

(defn create
  (^Analyzer [opts]
   (create opts char-filter-name->class tokenizer-name->class token-filter-name->class))
  (^Analyzer [{:keys [config-dir char-filters tokenizer token-filters namify-fn]}
              char-filter-factories tokenizer-factories token-filter-factories]
   (let [namify-fn (or namify-fn identity)
         ^CustomAnalyzer$Builder builder (CustomAnalyzer/builder ^Path (config-dir->path config-dir))]
     (.withTokenizer builder
                     ^Class (get-component-or-exception tokenizer-factories
                                                        (get tokenizer :name DEFAULT_TOKENIZER_NAME)
                                                        "Tokenizer"
                                                        namify-fn)
                     ^Map (HashMap. ^Map (stringify (get tokenizer :args))))

     (doseq [{:keys [name args]} char-filters]
       (.addCharFilter builder
                       ^Class (get-component-or-exception char-filter-factories
                                                          name
                                                          "Char filter"
                                                          namify-fn)
                       ^Map (HashMap. ^Map (stringify args))))

     (doseq [{:keys [name args]} token-filters]
       (.addTokenFilter builder
                        ^Class (get-component-or-exception token-filter-factories
                                                           name
                                                           "Token filter"
                                                           namify-fn)
                        ^Map (HashMap. ^Map (stringify args))))

     (.build builder))))

(comment
  (lmgrep.lucene.custom-analyzer/create
    {:tokenizer {:name "standard"
                 :args {:maxTokenLength 4}}
     :char-filters [{:name "patternReplace"
                     :args {:pattern "joc"
                            :replacement "foo"}}]
     :token-filters [{:name "uppercase"}
                     {:name "reverseString"}]})

  (lmgrep.lucene.custom-analyzer/create
    {:tokenizer {:name "standard"}
     :char-filters [{:name "patternReplace"
                     :args {:pattern "foo"
                            :replacement "bar"}}]}))
