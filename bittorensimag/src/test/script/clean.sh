#!/usr/bin/sh
echo "Fermeture des programmes"

# On netttoie les anciennes instances de transmission-daemon et wireshark 
killall transmission-daemon
killall wireshark
# Pour enlever aussi firefox (penser à enlever le warning pour fermer plusieurs onglets)
wmctrl -c "Firefox" -x "Navigator.Firefox"

# a ne pas utiliser car fermeture violente
# killall firefox

# Fermeture d'opentracker
killall opentracker.debug