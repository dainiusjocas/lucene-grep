.PHONY: pom.xml
pom.xml:
	clojure -Spom

.PHONY: uberjar
uberjar: pom.xml
	echo "$$(git describe --tags --abbrev=0)-SNAPSHOT" > resources/LMGREP_VERSION
	clojure -X:uberjar :jar target/lmgrep-uber.jar :main-class lmgrep.core

.PHONY: build
build: uberjar
	script/compile

.PHONY: build-linux-static
build-linux-static: uberjar
	LMGREP_STATIC=true script/compile

.PHONY: build-linux-static-musl
build-linux-static-musl: uberjar
	LMGREP_STATIC=true LMGREP_MUSL=true script/compile

docker_build = (docker build --build-arg $1 --build-arg $2 -f Dockerfile -t lmgrep-native-image .; \
				docker rm lmgrep-native-image-build || true; \
				docker create --name lmgrep-native-image-build lmgrep-native-image; \
				docker cp lmgrep-native-image-build:/usr/src/app/lmgrep lmgrep)

.PHONY: build-linux-with-docker
build-linux-with-docker:
	$(call docker_build, LMGREP_STATIC=false, LMGREP_MUSL=false)

.PHONY: build-linux-static-with-docker
build-linux-static-with-docker:
	$(call docker_build, LMGREP_STATIC=true, LMGREP_MUSL=false)

.PHONY: build-linux-static-musl-with-docker
build-linux-static-musl-with-docker:
	$(call docker_build, LMGREP_STATIC=true, LMGREP_MUSL=true)

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
