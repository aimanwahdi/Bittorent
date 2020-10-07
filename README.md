# equipe4 Bittorensimag

## Project Structure

    ├── bittorensimag            
        ├── src          
        |    ├── main         
        |    |    ├── assembly                  # Files to package jar
        |    |    ├── bin                       # Script to launch client
        |    |    └── java/bittorensimag        # Java Code 
        |    └── test                       
        |         ├── java/bittorensimag        # JUnit tests
        |         └── script                    # Scripts + Cobertura 
        ├── pom.xml

## Maven useful commands

    mvn compile                                 # Compiles the project
    mvn test                                    # Start JUnit and all-test.sh

    mvn site                                    # Generate javadoc in target/site
    mvn package                                 # Generate jar in target/package

## Start program

    src/main/bin/bittorensimag [-d] [-i] [-b] <file.torrent> <download folder>

## Cobertura

    mvn cobertura:cobertura                     # Generate cobertura stats
    src/test/script/cobertura_report.sh         # Generate website
    firefox target/site/cobertura/index.html    # Display report in firefox