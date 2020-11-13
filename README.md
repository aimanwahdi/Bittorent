# equipe4 Bittorensimag

## Project Structure

    .
    ├── src
    │   ├── main
    │   │   ├── assembly                        # Files to package jar
    │   │   ├── bin                             # Script to launch client
    │   │   └── java                            
    │   │       └── bittorensimag               
    │   │           ├── Client                  # Client Main + CLI + Output
    │   │           ├── MessageCoder            # FromWire and toWire methods
    │   │           ├── Messages                # Messages classes
    │   │           ├── Torrent                 # Torrent and Tracker
    │   │           └── Util                    # Static help functions
    │   └── test
    │       ├── captureWireshark                # Example of Wireshark captures
    │       ├── exampleFiles                    # Example source files
    │       ├── exampleTorrents                 # Example source torrents
    │       ├── java
    │       │   └── bittorensimag               # JUnit tests
    │       ├── outputFolder
    │       └── script                          # Scripts + Cobertura
    └── target                          

## Maven useful commands

    mvn compile                                 # Compiles the project + prepare tracker and seeders
    mvn test                                    # Start JUnit and all-test.sh

    mvn site                                    # Generate javadoc in target/site
    mvn package                                 # Generate jar in target/package

## Start program

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
