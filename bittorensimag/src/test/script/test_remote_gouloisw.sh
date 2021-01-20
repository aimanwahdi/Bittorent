#!/usr/bin/env bash

# Exemple de script de lancement à distance d'un seeder (et le tracker associé) et d'un leecher
# afin d'évaluer le débit d'échange (avec le débit affiché par aria2 ou par une capture wireshark > statistics > IOGraph > Y (bytes/tick))
# le script est à lancé sur la machine qui seede

# cela nécessite
# ensicp284:/tmp_data/votre_login
# ├── opentracker.debug
# ├── 500M
# ├── 500M.torrent
# ensicp283:/tmp_data/votre_login
# ├── 500M.torrent
# ├── votre client java

set -x # uncomment to debug

ensipcseed=ensipc289
ensipcleech=ensipc285

# change with ensimag username
whoami=$(whoami)

# fonction d'installation du tracker, .torrent et fichier sur le seeder et leecher
# l'installation de votre client java devra se faire à la main surla machine leecher
function install {
	mkdir /tmp_data/$(whoami)
	# si le fichier lié au torrent n'existe pas sur le seeder, je le crée
	if [[ ! -f /tmp_data/$(whoami)/500M ]]; then
		  dd if=/dev/zero bs=500M count=1 | LANG=C tr "\000" '1' > /tmp_data/$(whoami)/500M
	fi
  #remplace le nom du tracker ensipc284 dans le .torrent par ensipcseed
  sed s/ensipc284/${ensipcseed}/ /matieres/5MMPSEOC/500M.torrent > /tmp_data/$(whoami)/500M.torrent
  #recopie du tracker sur le seeder
  cp /matieres/5MMPSEOC/opentracker.debug /tmp_data/$(whoami)
  # recopie du torrent sur le leecher
  ssh ${ensipcleech}.ensimag.fr "mkdir /tmp_data/$whoami"
  scp /tmp_data/$(whoami)/500M.torrent /user/1/$(whoami)/bittorensimag.jar ${ensipcleech}.ensimag.fr:/tmp_data/$(whoami)
}

read -p "Voulez-vous installer les logiciels sur le leecher ${ensipcleech} (500M.torrent,bittorensimag.jar) et le seeder ${ensipcseed} (opentracker, 500M, 500M.torrent) ? (Y/N): " confirm

if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
	install
fi

# lancement du tracker en local
xterm -e "killall opentracker.debug;/tmp_data/$(whoami)/opentracker.debug -p 6969; $SHELL" &
sleep 1
# lancement de wireshark en local
#xterm -e "wireshark -i em1 -Y bittorrent -w /tmp_data/$(whoami)/bittorensimag.pcapng -k -S -l; $SHELL" &
#sleep 1
# lancement d'aria2c en local
xterm -e "killall aria2c;cd /tmp_data;aria2c -V -d /tmp_data/$(whoami) /tmp_data/$(whoami)/500M.torrent; $SHELL" &
sleep 1
# lancement de votre client java à distance (ici vous devez remplacer votre_client.jar par votre jar que vous aurez installé à la main)

xterm -e "ssh ${ensipcleech}.ensimag.fr 'cd /tmp_data/$whoami;java -jar bittorensimag.jar -d 500M.torrent ./'; $SHELL" &

echo "n'oubliez pas de supprimer toutes les fichiers sur /tmp_data afin de ne pas encombrer cet espace sur le disque local des machines une fois les expérimentation terminées"
