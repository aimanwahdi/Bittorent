#!/usr/bin/sh
echo "Fermeture des programmes"

# On récupère le nombre de clients transmissions actif (dernière ligne enlevée car c'est le grep)
COUNT=$(ps x | grep transmission-daemon | head -n -1 | wc -l)

# On boucle sur les clients
for line in $(seq 1 $COUNT);
do
    PORT=$(ps x | grep transmission-daemon | sed -n "$line"p |awk '{print $7}')
    echo "Transmission daemon trouvé avec le port $PORT"
    echo "Suppression des torrents..."
    transmission-remote $port -t all -rad
    sleep 1
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