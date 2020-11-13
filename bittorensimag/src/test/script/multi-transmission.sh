#!/usr/bin/sh

# Exemple de script de lancement de plusieurs clients transmissions

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
fileFolder=../exampleFiles

./src/test/script/clean.sh

# Start of opentracker
../../opentracker/opentracker.debug -i 127.0.0.1 -p 6969 </dev/null &>/dev/null &
urls="localhost:6969/stats"
sleep 2

#lancer plusieurs seeders en parallèle (ici 3)
seeder_number=3

for i in $(seq 1 ${seeder_number});do
    port=$((9090 + $i))
    echo "Starting seeder $i on port $port"
	transmission-daemon -p $port -w $fileFolder
	sleep 1
	# limitation possible de l'upload à 1Mo/s via -u 1M
	transmission-remote $port -a $torrent

    urls+=" localhost:$port"
done

# On lance wireshark sur loopback en affichant bittorrent
echo "Lancement de wireshark"
wireshark -i lo -Y bittorrent -k -S -l </dev/null &>/dev/null &

# On lance toutes les instances de firefox
echo "Ouverture des liens dans firefox"
firefox $urls </dev/null &>/dev/null &
