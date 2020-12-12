#!/bin/sh

if [ "$#" -ne 2 ]; then
  echo "Usage: multi-leechers loopBeggining loopEnd" >&2
  exit 1
fi

# If we're not in the root of the Maven project, then find it using
# this script's name:
if ! [ -r pom.xml ]; then
    cd "$(dirname "$0")"/../../../
fi
# Exemple de script de lancement de plusieurs clients transmissions

# Start by cleaning clients
./src/test/script/cleanClients.sh

# My directory structure
# ..
# ├── opentracker
# |	└── opentracker.debug
# |	..
# |	├── src
# |	│   └── test
# |	│       ├── exampleFiles
# |	│       │   └── Aigle.jpg
# |	│       ├── exampleTorrents
# |	│       │   └── Aigle.jpg.torrent
# |	│       └── script
# |	│           ├── clean.sh
# |	│           └── multi-transmission.sh

torrent=./src/test/exampleTorrents/Big_Buck_Bunny_1080p.avi.torrent
file=./src/test/exampleFiles/Big_Buck_Bunny_1080p.avi
fileFolder=$HOME/Downloads

#lancer plusieurs leechers en parallèle (ici 3)
leechers_number=$2

for i in $(seq $1 ${leechers_number});do
    port=$((2000 + $i))
	rpcPort=$((6800 + $i))
    echo "Starting leecher $i on port $port"

	downloadFolder=$fileFolder/aria2c$i
	mkdir $downloadFolder

	aria2c --enable-rpc --rpc-listen-all --rpc-listen-port=$rpcPort --listen-port $port -d $downloadFolder $torrent  &>/dev/null &
	sleep 1
done
