#!/bin/sh
# If we're not in the root of the Maven project, then find it using
# this script's name:
if ! [ -r pom.xml ]; then
    cd "$(dirname "$0")"/../../../
fi

echo "Nettoyage du dossier de sortie"

rm -rf ./src/test/outputFolder/*
