FROM eclipse-temurin:17-jre-jammy

RUN apt-get update && \
    apt-get install -y git && \
    rm -rf /var/lib/apt/lists/*

COPY ssh_config /etc/ssh/ssh_config

COPY ewp-registry*.jar /app.jar

VOLUME /root
WORKDIR /root
ENTRYPOINT ["sh", "-c", "java -XX:-OmitStackTraceInFastThrow ${JAVA_OPTS} -jar /app.jar"]
EXPOSE 8080