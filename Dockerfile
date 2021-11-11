FROM ghcr.io/graalvm/graalvm-ce:java11-21.3.0 as BUILDER

ENV GRAALVM_HOME=$JAVA_HOME

ENV CLOJURE_VERSION=1.10.3.1013

ENV LMGREP_FEATURE_RAUDIKKO=true

ARG LMGREP_STATIC
ENV LMGREP_STATIC=$LMGREP_STATIC

ARG LMGREP_MUSL
ENV LMGREP_MUSL=$LMGREP_MUSL

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
ENV MUSL_DIR=/usr/src/app/.musl

COPY script/ /usr/src/app/script

RUN microdnf install wget git \
    && gu install native-image \
    && /usr/src/app/script/setup-musl

ENV PATH=$PATH:${MUSL_DIR}/x86_64-linux-musl-native/bin

COPY --from=babashka/babashka:0.6.4 /usr/local/bin/bb /usr/local/bin/bb

RUN curl -O https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh \
    && rm linux-install-$CLOJURE_VERSION.sh

COPY deps.edn /usr/src/app/
RUN clojure -P && clojure -P -M:uberjar
COPY src/ /usr/src/app/src
COPY docs/ /usr/src/app/docs
COPY test/ /usr/src/app/test
COPY graalvm/ /usr/src/app/graalvm
COPY resources/ /usr/src/app/resources
COPY bb.edn /usr/src/app/

RUN clojure -Spom
RUN bb generate-reflection-config
RUN clojure -X:uberjar :jar target/lmgrep-uber.jar :main-class lmgrep.core

RUN ./script/compile
