FROM findepi/graalvm:21.0.0-java11-native as BUILDER

ENV GRAALVM_HOME=/graalvm
ENV JAVA_HOME=/graalvm

ENV CLOJURE_VERSION=1.10.2.774

RUN apt-get install -y curl git

RUN curl -O https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh \
    && rm linux-install-$CLOJURE_VERSION.sh

RUN git clone https://github.com/gunnarmorling/search.morling.dev.git
RUN (cd search.morling.dev && sh mvnw install || true)

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY deps.edn /usr/src/app/
RUN clojure -P -M:native-linux-static
COPY src/ /usr/src/app/src

RUN clojure -M:native-linux-static
