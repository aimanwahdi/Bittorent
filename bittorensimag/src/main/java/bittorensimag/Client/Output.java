package bittorensimag.Client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.log4j.Logger;

public class Output {
	private static final Logger LOG = Logger.getLogger(Output.class);

	private final String nomFichier;
	private final String destination;
	private byte[] data;
	
	public Output(String nomFichier, String destination, byte[] data) {
		super();
		this.nomFichier = nomFichier;
		this.destination = destination;
		this.data = data;
	}
	
	public void generateFile() {
		try {
			File f = new File(destination + nomFichier);
			//Creer un nouveau fichier 
			//verifier s'il n'existe pas 
			if (f.createNewFile()) {
				Files.write(f.toPath(), this.data);
				LOG.info("File " + f.getName() + " created in " + destination);
			} else {
				LOG.warn("File " + f.getName() + " already exists");
			}
			    
		} catch (IOException e) {
			LOG.fatal("Impossible to generate output file" + e.getMessage());
		}
		
	}
	
}
