#!/bin/bash

docker run --rm -it -v $(readlink -f registry_data):/root --net=host --entrypoint /root/entrypoint_register_keys.sh docker.usos.edu.pl:5000/ewp-registry-service
