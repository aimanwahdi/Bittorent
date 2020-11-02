#! /bin/sh

cd "$(dirname "$0")"/../../.. || exit 1
PATH=./src/main/bin:"$PATH"

echo "Working directory :" $(pwd)

# Without options
out=$(bittorensimag)
if [ -z "$out" ]; then
    echo "bad response without options should return usage"
    exit 1
else
    echo "good response without option displays usage"
fi

# Banner otion
banner=$(bittorensimag -b)
if [ -z "$banner" ]; then
    echo "bad response -b should print the banner"
    exit 1
else
    echo "good response -b prints banner"
fi

# Unkown option
out=$(bittorensimag -k src/test/exampleTorrents/Aigle.jpg.torrent src/test/outputFolder/ 2>/dev/null)
if [ ! -f src/test/outputFolder/Aigle.jpg ]; then
    echo "good response with unknown option"
else
    echo "bad response shouldn't accept unknown option"
    exit 1
fi
rm -f src/test/outputFolder/Aigle.jpg

# Otions after file
out=$(bittorensimag src/test/exampleTorrents/Aigle.jpg.torrent src/test/outputFolder/ -d 2>/dev/null)
if [ ! -f src/test/outputFolder/Aigle.jpg ]; then
    echo "good response with option after file"
else
    echo "bad response shouldn't accept options after filename"
    exit 1
fi
rm -f src/test/outputFolder/Aigle.jpg

# Torrent file after folder
out=$(bittorensimag src/test/outputFolder/ src/test/exampleTorrents/Aigle.jpg.torrent  2>/dev/null)
if [ ! -f src/test/outputFolder/Aigle.jpg ]; then
    echo "good response with torrent after folder"
else
    echo "bad response shouldn't accept torrent after folder"
    exit 1
fi
rm -f src/test/outputFolder/Aigle.jpg

# Multiple Torrent files
out=$(bittorensimag src/test/exampleTorrents/Aigle.jpg.torrent src/test/exampleTorrents/lion-wallpaper.jpg.torrent src/test/outputFolder/ 2>/dev/null)
if [ ! -f src/test/outputFolder/Aigle.jpg ]; then
    echo "good response with multiple torrent files"
else
    echo "bad response shouldn't accept multiple torrent files"
    exit 1
fi
rm -f src/test/outputFolder/Aigle.jpg
rm -f src/test/outputFolder/lion-wallpaper.jpg

# Multiple folders
out=$(bittorensimag src/test/exampleTorrents/Aigle.jpg.torrent src/test/outputFolder/ target/  2>/dev/null)
if [ ! -f src/test/outputFolder/Aigle.jpg ]; then
    echo "good response with multiple output folders"
else
    echo "bad response shouldn't accept multiple output folders"
    exit 1
fi
rm -f src/test/outputFolder/Aigle.jpg
rm -f target/Aigle.jpg

# # TODO option d
# debug=$(bittorensimag -d src/test/exampleTorrents/Aigle.jpg.torrent src/test/outputFolder/ 2>/dev/null)
# rm -f src/test/outputFolder/Aigle.jpg
# foundDebug=$(echo "$debug" | grep DEBUG)
# if [ -z "$foundDebug" ]
# then
#     echo "bad response -d should output debug"
#     exit 1
# else
#     echo "good response -d"
# fi

# # TODO option i
# info=$(bittorensimag -i src/test/exampleTorrents/Aigle.jpg.torrent src/test/outputFolder/ 2>/dev/null)
# rm -f src/test/outputFolder/Aigle.jpg
# foundInfo=$(echo "$info" | grep Information)
# if [ -z "$foundInfo" ]
# then
#     echo "bad response -i should output info"
#     exit 1
# else
#     echo "good response -i"
# fi