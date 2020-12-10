#!/bin/sh

# Exemple de script de lancement de plusieurs clients transmissions

# Start by cleaning everything
./src/test/script/clean.sh

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
fileFolder=$HOME/Downloads

# Start of opentracker
../../opentracker/opentracker.debug -i 127.0.0.1 -p 6969 </dev/null &>/dev/null &
urls="localhost:6969/stats"
sleep 2

# On lance wireshark sur loopback en affichant bittorrent
echo "Lancement de wireshark"
wireshark -i lo -Y bittorrent -k -S -l </dev/null &>/dev/null &
sleep 1

#lancer plusieurs seeders en parallèle (ici 3)
leechers_number=3

for i in $(seq 1 ${leechers_number});do
    port=$((9090 + $i))
    echo "Starting seeder $i on port $port"

	downloadFolder=$fileFolder/transmission$i
	mkdir $downloadFolder

	# Start daemon
	transmission-daemon -et -GSR -M -O -T --no-utp -p $port -P $((51412+$i))
	sleep 1
	# Add torrent
	transmission-remote $port -AS -ASC -D -U -X -Y -a $torrent --find $downloadFolder -w $downloadFolder
	sleep 1
	# Move Torrent to it's own directory
	transmission-remote $port -t all --move $downloadFolder
	sleep 1
	# Verify local data
	transmission-remote $port -t all -SR -v
	sleep 1 
	# Start torrent
	transmission-remote $port -t all -s 
	sleep 1
	
    urls+=" localhost:$port"
done

# On lance toutes les instances de firefox
echo "Ouverture des liens dans firefox"
firefox $urls </dev/null &>/dev/null &
