FROM ghcr.io/graalvm/graalvm-ce:21.2.0 as BUILDER

ENV GRAALVM_HOME=$JAVA_HOME

ENV CLOJURE_VERSION=1.10.3.839

ENV MUSL_DIR=${HOME}/.musl
ENV MUSL_VERSION=1.2.2
ENV ZLIB_VERSION=1.2.11

RUN mkdir $MUSL_DIR \
    && curl https://musl.libc.org/releases/musl-${MUSL_VERSION}.tar.gz -o musl-${MUSL_VERSION}.tar.gz \
    && tar zxvf musl-${MUSL_VERSION}.tar.gz \
    && cd musl-${MUSL_VERSION} \
    && ./configure --disable-shared --prefix=${MUSL_DIR} \
    && make \
    && make install \
    && curl https://zlib.net/zlib-${ZLIB_VERSION}.tar.gz -o zlib-${ZLIB_VERSION}.tar.gz \
    && tar zxvf zlib-${ZLIB_VERSION}.tar.gz \
    && cd zlib-${ZLIB_VERSION} \
    && ./configure --static --prefix=${MUSL_DIR} \
    && make \
    && make install \
    && gu install native-image

ENV PATH=$PATH:${MUSL_DIR}/bin

RUN curl -O https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh \
    && rm linux-install-$CLOJURE_VERSION.sh

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY deps.edn /usr/src/app/
RUN clojure -P && clojure -P -M:uberjar
COPY src/ /usr/src/app/src
COPY graalvm/ /usr/src/app/graalvm
COPY script/ /usr/src/app/script
COPY resources/ /usr/src/app/resources

RUN clojure -Spom
RUN clojure -X:uberjar :jar target/lmgrep-uber.jar :main-class lmgrep.core

ARG LMGREP_STATIC
ENV LMGREP_STATIC=$LMGREP_STATIC

ARG LMGREP_MUSL
ENV LMGREP_MUSL=$LMGREP_MUSL

RUN ./script/compile
