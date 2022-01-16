(ns lmgrep.features)

;; excluded by default
(def raudikko? (= "true" (System/getenv "LMGREP_FEATURE_RAUDIKKO")))
(def snowball? (= "true" (System/getenv "LMGREP_FEATURE_SNOWBALL")))
(def stempel? (= "true" (System/getenv "LMGREP_FEATURE_STEMPEL")))
