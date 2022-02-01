FROM ghcr.io/graalvm/graalvm-ce:java17-21.3.0 as BUILDER

ENV GRAALVM_HOME=$JAVA_HOME

ENV CLOJURE_VERSION=1.10.3.1069

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

COPY --from=babashka/babashka:0.7.4 /usr/local/bin/bb /usr/local/bin/bb

RUN curl -O https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh \
    && rm linux-install-$CLOJURE_VERSION.sh

COPY deps.edn /usr/src/app/
COPY build.clj /usr/src/app/build.clj
COPY lucene-monitor-helpers /usr/src/app/lucene-monitor-helpers
COPY raudikko /usr/src/app/raudikko
COPY stempel /usr/src/app/stempel
COPY bundled-analyzers /usr/src/app/bundled-analyzers
COPY snowball-token-filters /usr/src/app/snowball-token-filters

ARG LMGREP_FEATURE_RAUDIKKO
ENV LMGREP_FEATURE_RAUDIKKO=${LMGREP_FEATURE_RAUDIKKO:-false}

ARG LMGREP_FEATURE_SNOWBALL
ENV LMGREP_FEATURE_SNOWBALL=${LMGREP_FEATURE_SNOWBALL:-true}

ARG LMGREP_FEATURE_STEMPEL
ENV LMGREP_FEATURE_STEMPEL=${LMGREP_FEATURE_STEMPEL:-true}

ARG LMGREP_FEATURE_BUNDLED_ANALYZERS
ENV LMGREP_FEATURE_BUNDLED_ANALYZERS=${LMGREP_FEATURE_BUNDLED_ANALYZERS:-true}

RUN clojure -P && clojure -P -M:uberjar
COPY src/ /usr/src/app/src
COPY test/ /usr/src/app/test
COPY graalvm/ /usr/src/app/graalvm
COPY resources/ /usr/src/app/resources
COPY bb.edn /usr/src/app/

RUN clojure -Spom
RUN clojure -T:build prep-deps
RUN bb generate-reflection-config
RUN clojure -T:build uberjar

# RUN ./script/compile
