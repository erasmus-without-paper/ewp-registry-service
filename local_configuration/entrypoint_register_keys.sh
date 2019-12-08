#!/bin/bash

CACERTS_STORE="/usr/local/openjdk-8/lib/security/cacerts"
for CERTIFICATE in /root/keys/certs_to_trust/*.pem; do
    echo "Added trusted certificate $CERTIFICATE"
    keytool -import -file "$CERTIFICATE" -keystore $CACERTS_STORE -alias "$CERTIFICATE" -storepass changeit -noprompt
done

exec /start-wrapper.sh
