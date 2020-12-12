#!/bin/sh
# If we're not in the root of the Maven project, then find it using
# this script's name:
if ! [ -r pom.xml ]; then
    cd "$(dirname "$0")"/../../../
fi

# Start by cleaning clients
./src/test/script/cleanClients.sh

# Then clean environment
./src/test/script/cleanEnv.sh