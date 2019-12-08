#!/bin/bash

if [[ -z "$1" ]]; then
    echo "Usage: $0 <manifest-https-url> [<manifest-https-url>...]"
    exit 1
fi

mkdir -p registry_data
cd registry_data
mkdir -p repo
cd repo
git init
cd ..
cp ../entrypoint_register_keys.sh .
cp ../local_application.properties application.properties
cp -r ../keys .
cp keys/cert.pem keys/certs_to_trust/registry_cert.pem
echo "" > manifest-sources.xml
echo "<manifest-sources>" >> manifest-sources.xml
for MANIFEST_URL in $@; do
    echo "<source><location>$MANIFEST_URL</location></source>" >> manifest-sources.xml
done
echo "</manifest-sources>" >> manifest-sources.xml
