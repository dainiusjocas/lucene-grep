.PHONY: build
build:
	clojure -M:native

.PHONY: test
test:
	clojure -M:test

.PHONY: lint
lint:
	clojure -M:clj-kondo
