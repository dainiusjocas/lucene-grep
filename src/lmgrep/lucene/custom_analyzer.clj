(ns lmgrep.lucene.custom-analyzer
  (:import (java.util HashMap Map)
           (java.io File)
           (java.nio.file Path)
           (org.apache.lucene.analysis.custom CustomAnalyzer CustomAnalyzer$Builder)
           (org.apache.lucene.analysis Analyzer CharFilterFactory TokenFilterFactory TokenizerFactory)))

(defn- stringify [m]
  (reduce-kv (fn [acc k v] (assoc acc (name k) (str v))) {} m))

(defn load-tokenizer-factories []
  (reduce (fn [acc ^String tokenizer-name]
            (assoc acc tokenizer-name (TokenizerFactory/lookupClass tokenizer-name)))
          {} (TokenizerFactory/availableTokenizers)))

(defn load-char-filter-factories []
  (reduce (fn [acc ^String char-filter-name]
            (assoc acc char-filter-name (CharFilterFactory/lookupClass char-filter-name)))
          {} (CharFilterFactory/availableCharFilters)))

(defn load-token-filter-factories []
  (reduce (fn [acc ^String token-filter-name]
            (assoc acc token-filter-name (TokenFilterFactory/lookupClass token-filter-name)))
          {} (TokenFilterFactory/availableTokenFilters)))

(def tokenizer-name->class (load-tokenizer-factories))
(def char-filter-name->class (load-char-filter-factories))
(def token-filter-name->class (load-token-filter-factories))

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
  "Constructs a Lucene Analyzer using the CustomAnalyzer builder.
   Under the hood it uses the factory classes TokenizerFactory, TokenFilterFactory, and CharFilterFactory.
   The factories are loaded with java.util.ServiceLoader.

   Factory description is of shape:
   `
   {:name String
    :args Map}
   `

   If needed factories can be passed as arguments in shape:
   `
   {STRING CLASS}
   `

   Example:
   `
   {:tokenizer {:name \"standard\", :args {:maxTokenLength 4}}
    :char-filters [{:name \"patternReplace\" :args {:pattern \"foo\", :replacement \"foo\"}}]
    :token-filters [{:name \"uppercase\"} {:name \"reverseString\"}]
    :config-dir \".\"}
   `

   `opts` can have specified:
     - config-dir: path to directory from which resources will be loaded, default '.'
     - char-filters: list of char filter descriptions
     - tokenizer: tokenizer description, default 'standard' tokenizer
     - token-filters: list of token filter descriptions
     - namify-fn: function that changes the string identifier of the service name, e.g. str/lowercase, default: identity"
  (^Analyzer [opts]
   (create opts char-filter-name->class tokenizer-name->class token-filter-name->class))
  (^Analyzer [{:keys [config-dir char-filters tokenizer token-filters namify-fn]}
              char-filter-factories tokenizer-factories token-filter-factories]
   (let [namify-fn (or namify-fn identity)
         ^CustomAnalyzer$Builder builder (CustomAnalyzer/builder ^Path (config-dir->path config-dir))]

     (assert (or (nil? char-filters) (sequential? char-filters))
             (format "Character filters should be a list, was '%s'" char-filters))
     (assert (or (nil? token-filters) (sequential? token-filters))
             (format "Token filters should be a list, was '%s'" token-filters))

     (assert (or (nil? tokenizer) (map? tokenizer))
             (format "Tokenizer must have 'name' and 'args', but was '%s'" tokenizer))
     (.withTokenizer builder
                     ^Class (get-component-or-exception tokenizer-factories
                                                        (get tokenizer :name DEFAULT_TOKENIZER_NAME)
                                                        "Tokenizer"
                                                        namify-fn)
                     ^Map (HashMap. ^Map (stringify (get tokenizer :args))))

     (doseq [{:keys [name args] :as char-filter} char-filters]
       (assert (or (nil? char-filter) (map? char-filter))
               (format "Character filter must have 'name' and 'args', but was '%s'" char-filter))
       (.addCharFilter builder
                       ^Class (get-component-or-exception char-filter-factories
                                                          name
                                                          "Char filter"
                                                          namify-fn)
                       ^Map (HashMap. ^Map (stringify args))))

     (doseq [{:keys [name args] :as token-filter} token-filters]
       (assert (or (nil? token-filter) (map? token-filter))
               (format "Token filter must have 'name' and 'args', but was '%s'" token-filter))
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
                     :args {:pattern "foo"
                            :replacement "foo"}}]
     :token-filters [{:name "uppercase"}
                     {:name "reverseString"}]}))
