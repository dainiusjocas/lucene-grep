.PHONY: build
build:
	clojure -M:native

pom.xml:
	clojure -Spom

.PHONY: uberjar
uberjar: pom.xml
	clojure -X:uberjar :jar target/lmgrep-uber.jar :main-class lmgrep.core

.PHONY: test
test:
	clojure -M:test

.PHONY: lint
lint:
	clojure -M:clj-kondo
