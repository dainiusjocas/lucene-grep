FROM oracle/graalvm-ce:20.3.0-java11 as BUILDER

ENV GRAALVM_HOME=$JAVA_HOME

RUN gu install native-image \
    && curl -O https://download.clojure.org/install/linux-install-1.10.1.727.sh \
    && chmod +x linux-install-1.10.1.727.sh \
    && ./linux-install-1.10.1.727.sh \
    && rm linux-install-1.10.1.727.sh

RUN yum install -y git maven

RUN git clone https://github.com/gunnarmorling/search.morling.dev.git
RUN (cd search.morling.dev && sh mvnw install || true)

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY deps.edn /usr/src/app/
RUN clojure -P -M:native-linux-static
COPY src/ /usr/src/app/src

RUN clojure -M:native-linux-static
