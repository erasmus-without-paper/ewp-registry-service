#!/bin/bash

docker run --rm -it -v $(readlink -f registry_data):/root --net=host --entrypoint /root/entrypoint_register_keys.sh ghcr.io/erasmus-without-paper/ewp-registry-service/ewp-registry-service:latest
