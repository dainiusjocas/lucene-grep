(ns script.env-profiles)

(defn profiles
  "Checks the relevant environment variables and returns a list of alias keywords."
  []
  (println "Collecting relevant profiles")
  (cond-> []
          (Boolean/valueOf (System/getenv "LMGREP_FEATURE_RAUDIKKO"))
          (conj :raudikko)
          (Boolean/valueOf (System/getenv "LMGREP_FEATURE_STEMPEL"))
          (conj :stempel)
          (Boolean/valueOf (System/getenv "LMGREP_FEATURE_BUNDLED_ANALYZERS"))
          (conj :bundled-analyzers)))
