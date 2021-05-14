FROM findepi/graalvm:21.0.0-java11-native as BUILDER

ENV GRAALVM_HOME=/graalvm
ENV JAVA_HOME=/graalvm

ENV CLOJURE_VERSION=1.10.3.839

RUN apt-get update && apt-get install -y \
    build-essential \
    libz-dev \
    zlib1g \
    zlib1g-dev \
    curl \
    git \
    upx \
    musl \
    musl-dev \
    musl-tools \
    && rm -rf /var/lib/apt/lists/*

RUN curl -O https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh \
    && rm linux-install-$CLOJURE_VERSION.sh

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY deps.edn /usr/src/app/
RUN clojure -P -M:uberjar
COPY src/ /usr/src/app/src
COPY graalvm/ /usr/src/app/graalvm
COPY script/ /usr/src/app/script

RUN clojure -Spom
RUN clojure -X:uberjar :jar target/lmgrep-uber.jar :main-class lmgrep.core
RUN ./script/compile
