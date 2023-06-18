(ns lmgrep.features)

;; excluded by default
(def raudikko? (or (= "true" (System/getenv "LMGREP_FEATURE_RAUDIKKO"))
                   (= "true" (System/getProperty "LMGREP_FEATURE_RAUDIKKO"))))
(def snowball? (or (= "true" (System/getenv "LMGREP_FEATURE_SNOWBALL"))
                   (= "true" (System/getProperty "LMGREP_FEATURE_SNOWBALL"))))
(def stempel? (or (= "true" (System/getenv "LMGREP_FEATURE_STEMPEL"))
                  (= "true" (System/getProperty "LMGREP_FEATURE_STEMPEL"))))
(def bundled? (or (= "true" (System/getenv "LMGREP_FEATURE_BUNDLED_ANALYZERS"))
                  (= "true" (System/getProperty "LMGREP_FEATURE_BUNDLED_ANALYZERS"))))
