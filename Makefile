.PHONY: pom.xml
pom.xml:
	clojure -Spom

.PHONY: deps-prep
deps-prep:
	clojure -T:build prep-deps

.PHONY: uberjar
uberjar: deps-prep pom.xml
	echo "$$(git describe --tags --abbrev=0)-SNAPSHOT" > resources/LMGREP_VERSION
	clojure -T:build uberjar

.PHONY: build
build: uberjar
	bb generate-reflection-config
	script/compile

.PHONY: build-linux-static
build-linux-static: uberjar
	LMGREP_STATIC=true script/compile

.PHONY: build-linux-static-musl
build-linux-static-musl: uberjar
	./script/setup-musl
	PATH=$$PATH:$$(pwd)/.musl/x86_64-linux-musl-native/bin LMGREP_STATIC=true LMGREP_MUSL=true script/compile

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
test-binary: build
	sh binary-test.sh

test-all: export LMGREP_FEATURE_RAUDIKKO = true

.PHONY: test-all
test-all: test test-binary

.PHONY: lint
lint:
	clojure -M:clj-kondo

.PHONY: check-deps
check-deps:
	clojure -Sdeps '{:deps {antq/antq {:mvn/version "RELEASE"}}}' -M -m antq.core
