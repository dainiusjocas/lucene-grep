# The tips and trick on how to setup the native-image compiler

Run the Application with the `-J-agentlib:native-image-agent=config-output-dir=` to get the reflection config.

```shell
 echo "cats and dogs"  | clojure -J-agentlib:native-image-agent=config-output-dir=graalvm -M -m lmgrep.core  --only-analyze
```

Collect only the classes needed for the compilation with all declared constructors.

```clojure
(require '[jsonista.core :as json])

(spit "graalvm/lucene-reflect-config.json"
      (json/write-value-as-string
        (map (fn [entry]
               (assoc entry "allDeclaredConstructors" true))
             (filter
               (fn [e] (or (re-matches #"org.apache.*" (get e "name"))
                           (re-matches #"org.tartarus.*" (get e "name"))))
               (json/read-value (slurp "graalvm/reflect-config.json"))))))
```
