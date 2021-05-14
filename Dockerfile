FROM ghcr.io/graalvm/graalvm-ce:21.1.0 as BUILDER

ENV GRAALVM_HOME=$JAVA_HOME

ENV CLOJURE_VERSION=1.10.3.839

ENV RESULT_DIR=/mymusl

RUN mkdir $RESULT_DIR \
    && curl https://musl.libc.org/releases/musl-1.2.2.tar.gz -o musl.tar.gz \
    && tar zxvf musl.tar.gz \
    && cd musl-1.2.2 \
    && ./configure --disable-shared --prefix=${RESULT_DIR}\
    && make \
    && make install \
    && curl https://zlib.net/zlib-1.2.11.tar.gz -o zlib-1.2.11.tar.gz \
    && tar zxvf zlib-1.2.11.tar.gz \
    && cd zlib-1.2.11 \
    && ./configure --static --prefix=${RESULT_DIR} \
    && make \
    && make install \
    && gu install native-image

ENV PATH=$PATH:${RESULT_DIR}/bin

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

RUN clojure -Spom
RUN clojure -X:uberjar :jar target/lmgrep-uber.jar :main-class lmgrep.core
RUN ./script/compile
