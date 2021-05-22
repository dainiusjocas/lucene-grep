.PHONY: pom.xml
pom.xml:
	clojure -Spom

.PHONY: uberjar
uberjar: pom.xml
	clojure -X:uberjar :jar target/lmgrep-uber.jar :main-class lmgrep.core

.PHONY: build
build:
	script/compile

.PHONY: build-linux-static
build-linux-static:
	LMGREP_STATIC=true script/compile

.PHONY: build-linux-static-musl
build-linux-static-musl:
	LMGREP_STATIC=true LMGREP_MUSL=true script/compile

.PHONY: build-linux-static-with-docker
build-linux-static-with-docker:
	docker build \
		--build-arg LMGREP_STATIC=true \
		-f Dockerfile \
		-t lmgrep-native-image .
	docker rm lmgrep-native-image-build || true
	docker create --name lmgrep-native-image-build lmgrep-native-image
	docker cp lmgrep-native-image-build:/usr/src/app/lmgrep lmgrep

.PHONY: build-linux-static-musl-with-docker
build-linux-static-musl-with-docker:
	docker build \
		--build-arg LMGREP_STATIC=true \
		--build-arg LMGREP_MUSL=true \
		-f Dockerfile \
		-t lmgrep-native-image .
	docker rm lmgrep-native-image-build || true
	docker create --name lmgrep-native-image-build lmgrep-native-image
	docker cp lmgrep-native-image-build:/usr/src/app/lmgrep lmgrep

.PHONY: test
test:
	clojure -M:test

.PHONY: test-binary
test-binary:
	sh binary-test.sh

.PHONY: test-all
test-all: test build test-binary

.PHONY: lint
lint:
	clojure -M:clj-kondo

.PHONY: check-deps
check-deps:
	clojure -Sdeps '{:deps {antq/antq {:mvn/version "RELEASE"}}}' -M -m antq.core
