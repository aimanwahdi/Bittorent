#!/bin/sh

if [ "$#" -ne 2 ]; then
  echo "Usage: multi-seeders loopBeggining loopEnd" >&2
  exit 1
fi

# If we're not in the root of the Maven project, then find it using
# this script's name:
if ! [ -r pom.xml ]; then
    cd "$(dirname "$0")"/../../../
fi
# Exemple de script de lancement de plusieurs clients transmissions

# Start by cleaning clients
#./src/test/script/cleanClients.sh

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

#lancer plusieurs seeders en parallèle (ici 3)
seeder_number=$2

for i in $(seq $1 ${seeder_number});do
    port=$((2000 + $i))
	rpcPort=$((6800 + $i))
    echo "Starting seeder $i on port $port"

	downloadFolder=$fileFolder/aria2c_$i
	mkdir $downloadFolder

	cp $file $downloadFolder

	aria2c --check-integrity=true --disable-ipv6=true --enable-dht=false --enable-dht6=false --enable-peer-exchange=false --enable-rpc --rpc-listen-all --rpc-listen-port=$rpcPort --seed-ratio=0.0 --listen-port $port -V -d $downloadFolder $torrent &>./src/test/logs/aria2c_$i.log &
	sleep 1
done
