# equipe4 Bittorensimag

## Project Structure

Structure of the project is very important to execute scripts.

    .
    ├── src
    │   ├── main
    │   │   ├── assembly                        # Files to package jar
    │   │   ├── bin                             # Script to launch client
    │   │   ├── java                            
    │   │   |   └── bittorensimag               
    │   │   |       ├── Client                  # Client Main + CLI + Output
    │   │   |       ├── MessageCoder            # FromWire and toWire methods
    │   │   |       ├── Messages                # Messages classes
    │   │   |       ├── ProgressBar             # Progression/Performance
    │   │   |       ├── Torrent                 # Torrent and Tracker
    │   │   |       └── Util                    # Static help functions
    │   │   └── resources                       # Log4j properties
    │   └── test
    │       ├── Aigle*                          # Source files for scenarios
    │       ├── exampleTorrents                 # Example source torrents
    │       ├── java
    │       │   └── bittorensimag               # JUnit tests
    │       ├── logs                            # Logs of client and environment
    │       ├── outputFolder                    # Folder for testing client
    │       ├── Piece*                          # Source files for scenarios
    │       ├── script                          # Scripts + Cobertura
    │       └── webui-aria2                     # Web client for aria2 (Optional)
    └── target                          

## Installation

    git clone https://gitlab.ensimag.fr/projet-reseau/2020-2021/equipe4.git (HTTPS)
    git clone git@gitlab.ensimag.fr:projet-reseau/2020-2021/equipe4.git (SSH)

### Opentracker

For the initEnv.sh script to work, you need to have this file structure

    .
    ├── equipe4
    ├── libowfat
    ├── opentracker
            └── opentracker.debug

### Aria2 web server (Optional)

You can download webui-aria2 too and put it in src/test directory

    cd equipe4    
    git clone https://github.com/ziahamza/webui-aria2.git bittorensimag/src/test/webui-aria2

### Big Buck Bunny (Optional)

Also you can find original torrent file to download Big Buck Bunny movie (885MB). It is in folder bittorensimag/src/test/exampleFiles/Big_Buck_Bunny_source.torrent and for automated scripts the output should be in the same folder.

You can then generate half files with this script (under exampleFiles)

    ./splitBBB.sh

### 500M (Optional)

For 500M you can execute this command at root of project (equipe4)

    dd if=/dev/zero bs=500M count=1 | LANG=C tr "\000" '1' > bittorensimag/src/test/exampleFiles/500M

## Maven useful commands (under equipe4/bittorensimag)

    mvn compile                                 # Compiles the project
    mvn test                                    # Start JUnit and all-test.sh (CLI)
    
    mvn site                                    # Generate javadoc in target/site
    mvn package                                 # Generate jar in target

## Start program

    java -jar bittorensimag.jar [-d] [-i] [-b] <file.torrent> <download_folder>
    src/main/bin/bittorensimag [-d] [-i] [-b] <file.torrent> <download_folder>
    
    <file.torrent>    : source torrent file
    <download_folder> : folder where to search for source file (determines leecher or seeder)
    -b  (banner)      : print banner of the project
    -d  (debug)       : print minimal debug information
    -i  (info)        : print each second information about peers (bittorrent application, IP address, port, download/upload of pieces)

## Cobertura

    mvn cobertura:cobertura                     # Generate cobertura stats
    src/test/script/cobertura_report.sh         # Generate website
    firefox target/site/cobertura/index.html    # Display report in firefox

## Example Test with Scripts

Under bittorensimag/src/test/script folder

    cd bittorensimag/src/test/script

### Clean up

You can clean after testing with these scripts :

    ./cleanEnv.sh
    ./cleanClients.sh
    ./cleanOutput.sh

    ./cleanAll.sh does 3 scripts above

### Local

Example Leecher 0% with 3 aria2c Seeder 100% for file 500M

    cd bittorensimag/src/test/script
    ./initEnv.sh
    ./multi-seeders.sh 1 3
    ../../main/bin/bittorensimag -i ../exampleTorrents/500M.torrent ../outputFolder/

you can package the jar file with mvn package under equipe4/bittorensimag and then under /target :

    java -jar bittorensimag-1.0-jar-with-dependencies.jar -i ../src/test/exampleTorrents/500M.torrent ../src/test/outputFolder/

In multi-seeders.sh and multi-leechers.sh you can change file and torrent used.
They take as arguments beggining and end of for loop. To create only one client you can do :

    ./multi-seeders.sh 1 1

### Remote (via VPN Ensimag)

With sftp copy the test_remote_gouloisw.sh and the jar file to your home directory. Rename the jar file to bittorensimag.jar

Then graphical ssh into the SEEDER and put test_remote_gouloisw.sh in /tmp_data.

Change SEEDER and LEECHER to machines you want to test the program on.

Start wireshark (if you want) on both SEEDER and LEECHER with this command :

    wireshark -i em1 -Y bittorrent -w /tmp_data/$(whoami)/bittorensimag.pcapng -k -S -l

Finally you can start test_remote_gouloisw.sh and accept to install dependancies to begin the test.
