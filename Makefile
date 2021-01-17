.PHONY: build
build:
	clojure -M:native

.PHONY: build-linux-static
build-linux-static:
	clojure -M:native-linux-static

.PHONY: build-linux-static-with-docker
build-linux-static-with-docker:
	docker build -f Dockerfile -t lmgrep-native-image .
	docker rm lmgrep-native-image-build || true
	docker create --name lmgrep-native-image-build lmgrep-native-image
	docker cp lmgrep-native-image-build:/usr/src/app/lmgrep lmgrep

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
