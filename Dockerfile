FROM ghcr.io/graalvm/graalvm-ce:ol8-java17-22.3.2 as BUILDER

ENV GRAALVM_HOME=$JAVA_HOME

ENV CLOJURE_VERSION=1.11.1.1347

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

COPY --from=babashka/babashka:1.3.181 /usr/local/bin/bb /usr/local/bin/bb

RUN curl -O https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh \
    && rm linux-install-$CLOJURE_VERSION.sh

COPY deps.edn /usr/src/app/
COPY build.clj /usr/src/app/build.clj
COPY modules /usr/src/app/modules

ARG LMGREP_FEATURE_RAUDIKKO
ENV LMGREP_FEATURE_RAUDIKKO=${LMGREP_FEATURE_RAUDIKKO:-true}

ARG LMGREP_FEATURE_SNOWBALL
ENV LMGREP_FEATURE_SNOWBALL=${LMGREP_FEATURE_SNOWBALL:-true}

ARG LMGREP_FEATURE_STEMPEL
ENV LMGREP_FEATURE_STEMPEL=${LMGREP_FEATURE_STEMPEL:-true}

ARG LMGREP_FEATURE_BUNDLED_ANALYZERS
ENV LMGREP_FEATURE_BUNDLED_ANALYZERS=${LMGREP_FEATURE_BUNDLED_ANALYZERS:-true}

RUN clojure -P -M:build
COPY src/ /usr/src/app/src
COPY test/ /usr/src/app/test
COPY graalvm/ /usr/src/app/graalvm
COPY resources/ /usr/src/app/resources
COPY bb.edn /usr/src/app/
COPY README.md /usr/src/app/

RUN clojure -Spom
RUN clojure -T:build prep-deps
RUN bb generate-reflection-config
RUN clojure -T:build uberjar

RUN ./script/compile
