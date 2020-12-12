#!/bin/sh
# If we're not in the root of the Maven project, then find it using
# this script's name:
if ! [ -r pom.xml ]; then
    cd "$(dirname "$0")"/../../../
fi

echo "Fermeture des clients"

fileFolder=$HOME/Downloads

# clean all aria2c data
killall aria2c
rm -rf $fileFolder/aria2c*
