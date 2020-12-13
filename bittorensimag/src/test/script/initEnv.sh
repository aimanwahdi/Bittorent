#!/bin/sh

# If we're not in the root of the Maven project, then find it using
# this script's name:
if ! [ -r pom.xml ]; then
    cd "$(dirname "$0")"/../../../
fi

# Start of opentracker
../../opentracker/opentracker.debug -i 127.0.0.1 -p 6969 </dev/null &>/dev/null &
sleep 2

# server needs to be started at root of webui
cd ./src/test/webui-aria2/
# Start of webserver for aria2c
nohup node node-server.js &>../logs/webui-aria2.log &

cd ../../../

# On lance wireshark sur loopback en affichant bittorrent
echo "Lancement de wireshark"
wireshark -i lo -Y bittorrent -w ./src/test/logs/bittorensimag.pcapng -k -S -l </dev/null &>/dev/null &
sleep 1

# On lance les stats d'opentracker et la webui dans firefox
firefox "localhost:6969/stats" "localhost:8888" </dev/null &>/dev/null &