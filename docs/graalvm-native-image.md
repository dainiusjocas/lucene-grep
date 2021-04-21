# The tips and trick on how to setup the native-image compiler

Run the Application with the `-J-agentlib:native-image-agent=config-output-dir=` to get the reflection config.

```shell
 echo "dainius jocas"  | clojure -J-agentlib:native-image-agent=config-output-dir=graalvm -M -m lmgrep.core  --only-analyze
```

Collect only the classes needed for the compilation with all declared constructors.

```clojure
(spit "graalvm/lucene-reflect-config.json"
      (json/write-value-as-string
        (map (fn [entry]
               (assoc entry "allDeclaredConstructors" true))
             (filter
               (fn [e] (re-matches #"org.apache.*" (get e "name")))
               (json/read-value (slurp "graalvm/reflect-config.json"))))))
```
