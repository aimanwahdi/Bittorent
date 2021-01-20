package bittorensimag.Client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

import org.apache.log4j.Logger;

import bittorensimag.Torrent.Torrent;

public class Output {
	private static final Logger LOG = Logger.getLogger(Output.class);

	private File f;
	private RandomAccessFile raf;
	private FileInputStream inFile;
	private FileChannel inChannel;

	private String name;

	public Output(String nomFichier, String destination) throws FileNotFoundException {
		super();
		this.f = new File(destination + nomFichier);
		this.name = nomFichier;
	}

	public String getName() {
		return name;
	}

	public File getParentFolder() {
		return new File(this.f.getAbsoluteFile().getParent());
	}

	public void generateFile(byte[] data) {
		try {
			// Creer un nouveau fichier
			// verifier s'il n'existe pas
			if (this.f.createNewFile()) {
				Files.write(this.f.toPath(), data);
				LOG.info("File \"" + this.f.getName() + "\" created in " + this.f.getAbsolutePath());
			} else {
				LOG.warn("File \"" + this.f.getName() + "\" already exists");
			}
		} catch (IOException e) {
			LOG.fatal("Impossible to generate output file" + e.getMessage());
		}

	}

	public void createEmptyFile(File sourceFile, File destinationFolder) throws FileNotFoundException {
		byte[] emptyArray = new byte[Torrent.totalSize];
		this.generateFile(emptyArray);
		this.createFileObjects();
	}

	void createFileObjects() {
		try {
			this.raf = new RandomAccessFile(this.f, "rw");
			this.inFile = new FileInputStream(this.f);
			this.inChannel = this.inFile.getChannel();
		} catch (FileNotFoundException e) {
			LOG.fatal("Could not create objects to read or write into file");
		}
	}

	public boolean writeToFile(long offset, byte[] data) {
		try {
			this.raf.seek(offset);
			this.raf.write(data);
			LOG.debug("Piece added to the file");
		} catch (IOException e) {
			LOG.error("Fail to write to file " + this.f.getName() + " at position " + offset);
			return false;
		}
		return true;
	}

	public byte[] getPieceData(int index) {
		int length = Torrent.pieces_length;
		if (index == Torrent.numberOfPieces - 1) {
			length = Torrent.lastPieceLength;
		}
		long offset = index * Torrent.pieces_length;
		byte[] pieceData = new byte[length];
		try {
			this.raf.seek(offset);
			this.raf.read(pieceData);
		} catch (IOException e) {
			LOG.error("Fail to write to file " + this.getFile().getName() + " at position " + offset);
		}
		return pieceData;
	}

	public FileChannel getInChannel() {
		return this.inChannel;
	}

	public FileInputStream getInfile() {
		return this.inFile;
	}

	public File getFile() {
		return this.f;
	}

	public void closeInChannel() throws IOException {
		this.inFile.close();
	}
}	

