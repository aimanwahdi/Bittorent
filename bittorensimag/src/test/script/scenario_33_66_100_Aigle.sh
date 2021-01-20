#!/bin/sh

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

torrent=./src/test/exampleTorrents/Aigle.jpg.torrent
file=./src/test/exampleFiles/Aigle.jpg
fileBegin=./src/test/Aigle033/Aigle.jpg
fileEnd=./src/test/Aigle066/Aigle.jpg
fileFolder=$HOME/Downloads

for i in $(seq 1 3);do
    echo "Creating folder for seeder $i"
	mkdir $fileFolder/aria2c_$i
done

# Clean to begin as leecher 0%
# rm ./src/test/outputFolder/Aigle.jpg

echo "Copying data in folders"
cp $fileBegin $fileFolder/aria2c_1
cp $fileEnd $fileFolder/aria2c_2
cp $file $fileFolder/aria2c_3

for i in $(seq 1 3);do
    port=$((2000 + $i))
	rpcPort=$((6800 + $i))
    echo "Starting seeder $i on port $port"

	downloadFolder=$fileFolder/aria2c_$i

	aria2c --check-integrity=true --max-upload-limit=10K --disable-ipv6=true --enable-dht=false --enable-dht6=false --enable-peer-exchange=false --enable-rpc --rpc-listen-all --rpc-listen-port=$rpcPort --seed-ratio=0.0 --listen-port $port -V -d $downloadFolder $torrent &>./src/test/logs/aria2c_$i.log &
	sleep 1
done
