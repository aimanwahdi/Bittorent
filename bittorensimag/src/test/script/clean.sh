#!/bin/sh
echo "Fermeture des programmes"

# On récupère le nombre de clients transmissions actif (dernière ligne enlevée car c'est le grep)
COUNT=$(ps x | grep transmission-daemon | head -n -1 | wc -l)

fileFolder=$HOME/Downloads

# delete capture file
rm $fileFolder/bittorensimag

# On boucle sur les clients
for line in $(seq 1 $COUNT);
do
    # Le sed récupère la ligne et le awk le numéro de port
    PORT=$(ps x | grep transmission-daemon | sed -n "$line"p | awk '{ for(i=1;i<=NF;i++) { if ($i ~ "-p") { print $(i+1) } } }')
    echo "Transmission daemon trouvé avec le port $PORT"
    echo "Suppression des torrents..."
    transmission-remote $PORT -t all -r
    sleep 1
    rm -rf $fileFolder/transmission*
done

# On netttoie les anciennes instances de transmission-daemon et wireshark 
killall transmission-daemon
killall wireshark
# Pour enlever aussi firefox (penser à enlever le warning pour fermer plusieurs onglets)
wmctrl -c "Firefox" -x "Navigator.Firefox"

# a ne pas utiliser car fermeture violente
# killall firefox

# Fermeture d'opentracker
killall opentracker.debug