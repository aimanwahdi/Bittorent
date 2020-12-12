#!/bin/sh
# If we're not in the root of the Maven project, then find it using
# this script's name:
if ! [ -r pom.xml ]; then
    cd "$(dirname "$0")"/../../../
fi

fileFolder=$HOME/Downloads
# delete capture file
rm $fileFolder/bittorensimag
echo "Fermeture des programmes d'environnement"

killall wireshark
# Pour enlever aussi firefox (penser à enlever le warning pour fermer plusieurs onglets)
wmctrl -c "Firefox" -x "Navigator.Firefox"

# a ne pas utiliser car fermeture violente
# killall firefox

# Fermeture d'opentracker
killall opentracker.debug

killall node