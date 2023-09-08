#!/bin/bash

# Run a simple environment validator.

python3 /validate.py

if [ $? -ne 0 ]
then
    sleep 1  # prevent clogging logs when run with --restart=always
    exit 1
fi

java -jar -XX:-OmitStackTraceInFastThrow -XX:+UseContainerSupport $* /app.jar
