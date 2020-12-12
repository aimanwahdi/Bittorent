#!/bin/sh

torrent=./src/test/exampleTorrents/Big_Buck_Bunny_1080p.avi.torrent
file=./src/test/exampleFiles/Big_Buck_Bunny_1080p.avi
fileFolder=$HOME/Downloads

#lancer plusieurs seeders en parallèle (ici 3)
leechers_number=3

for i in $(seq 4 ${leechers_number});do
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

# On lance toutes les nouvelles tab de firefox
echo "Ouverture des liens dans firefox"
firefox --new-tab $urls </dev/null &>/dev/null &