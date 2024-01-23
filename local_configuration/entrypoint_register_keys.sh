#!/bin/bash

CACERTS_STORE="/opt/java/openjdk/lib/security/cacerts"
for CERTIFICATE in /root/keys/certs_to_trust/*.pem; do
    echo "Added trusted certificate $CERTIFICATE"
    keytool -import -file "$CERTIFICATE" -keystore $CACERTS_STORE -alias "$CERTIFICATE" -storepass changeit -noprompt
done

exec java -XX:-OmitStackTraceInFastThrow ${JAVA_OPTS} -jar /app.jar
