package bittorensimag.Torrent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import be.adaxisoft.bencode.InvalidBEncodingException;
import bittorensimag.Messages.Piece;
import bittorensimag.Util.Hashage;
import bittorensimag.Util.MapUtil;
import bittorensimag.Util.Util;

public class Torrent {
    private static final Logger LOG = Logger.getLogger(Torrent.class);

    public final File torrentFile;
    private FileInputStream inputStream;
    private BDecoder reader;
    private Map<String, BEncodedValue> document;
    private Map<String, BEncodedValue> info;
    private HashMap<String, Object> metadata = new HashMap<String, Object>();

    public String info_hash;
    String encoded_info_hash;

    // TODO change to static (complex)
    public static int numberOfPartPerPiece;
    public static int numberOfPieces;
    public static int lastPieceLength;
    public static int lastPieceNumberOfPart;
    public static int lastPartLength;
    public static int pieces_length;

    public static Map<Integer, byte[]> dataMap = new HashMap<Integer, byte[]>();
    public static Map<Integer, byte[]> piecesHashes = new HashMap<Integer, byte[]>();

    public final static String INFO = "info";
    public final static String SHA_1 = "SHA-1";

    public final static String ANNOUNCE = "announce";
    public final static String ANNOUNCE_LIST = "announce-list";
    public final static String COMMENT = "comment";
    public final static String CREATED_BY = "created by";
    public final static String ENCODING = "encoding";
    public final static String CREATION_DATE = "creation date";

    public final static String PIECES = "pieces";
    public final static String NAME = "name";
    public final static String MD5SUM = "md5sum";

    public final static String PIECE_LENGTH = "piece length";
    public final static String LENGTH = "length";
    public final static String PRIVATE = "private";

    private final String[] possibleKeysDocument = { ANNOUNCE, ANNOUNCE_LIST, COMMENT, CREATED_BY, ENCODING };
    private final String[] possibleKeysInfoString = { PIECES, NAME, MD5SUM };
    private final String[] possibleKeysInfoInt = { PIECE_LENGTH, PRIVATE, LENGTH };

    public Torrent(File torrentFile) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        this.torrentFile = torrentFile;
        try {
            this.inputStream = new FileInputStream(torrentFile);
        } catch (FileNotFoundException e) {
            LOG.fatal("Could not create FileInputStream for torrent : " + torrentFile);
        }
        this.reader = new BDecoder(inputStream);
        try {
            this.document = reader.decodeMap().getMap();
            if (hasInfo()) {
                this.info = this.document.get(INFO).getMap();
            }
        } catch (IOException e) {
            LOG.fatal("Could not decode document map from torrent file");
        }
        this.hashInfo();
        this.metadata = fillMetadata();
        this.setFields();
    }

    public boolean hasInfo() {
        if (this.document.containsKey(INFO)) {
            LOG.debug("The info field exists");
            return true;
        } else {
            return false;
        }
    }

    private void hashInfo() throws IOException, NoSuchAlgorithmException {
        Map<String, BEncodedValue> info = this.getInfo();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            BEncoder.encode(info, baos);
        } catch (IOException e) {
            LOG.fatal("Could not encode info dictionnary");
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(SHA_1);
        } catch (NoSuchAlgorithmException e) {
            LOG.fatal("Algorithm " + SHA_1 + " does not exist");
        }
        md.update(baos.toByteArray());
        byte[] digest = md.digest();
        String s = Util.bytesToHex(digest); // to test sha1
        String encodedHash = new String(digest, StandardCharsets.ISO_8859_1);
        this.info_hash = s;
        this.encoded_info_hash = encodedHash;
    }

    private void setFields() {
        LOG.debug("Calculating number of parts and number of pieces");
        this.calculateNumberParts();
        this.calculateNumberPieces();
    }

    public Map<String, BEncodedValue> getDocument() {
        return this.document;
    }

    public Map<String, BEncodedValue> getInfo() {
        return this.info;
    }

    private HashMap<String, Object> fillMetadata() throws InvalidBEncodingException {
        // Get all keys of document dictionnary
        MapUtil.fillBencodeMapString(this.document, this.metadata, possibleKeysDocument);

        // Creation date key
        if (this.document.containsKey(CREATION_DATE)) {
            Long date = this.document.get(CREATION_DATE).getLong() * 1000L;
            if (date != 0) {
                this.metadata.put(CREATION_DATE, new Date(date));
                LOG.debug("Added creation date to torrent metadata");
            }
        }

        // Get all keys of info dictionnary
        MapUtil.fillBencodeMapString(this.info, this.metadata, possibleKeysInfoString);

        MapUtil.fillBencodeMapInt(this.info, this.metadata, possibleKeysInfoInt);

        return this.metadata;
    }

    public HashMap<String, Object> getMetadata() {
        return this.metadata;
    }

    private void calculateNumberParts() {
        Torrent.pieces_length = (int) this.getMetadata().get(PIECE_LENGTH);
        Torrent.numberOfPartPerPiece = pieces_length / Piece.DATA_LENGTH;
        if (pieces_length % Piece.DATA_LENGTH != 0) {
            LOG.error("Warning : pieces length is not a multiple of 16Kb");
        }
    }

    private void calculateNumberPieces() {
        int length = (int) this.getMetadata().get(LENGTH);
        Torrent.numberOfPieces = (int) Math
                .ceil((double) length / (double) (Torrent.numberOfPartPerPiece * Piece.DATA_LENGTH));
        Torrent.lastPieceLength = length % (Torrent.numberOfPartPerPiece * Piece.DATA_LENGTH);
        Torrent.lastPartLength = length % Piece.DATA_LENGTH;
        Torrent.lastPieceNumberOfPart = (int) Math.ceil((double) Torrent.lastPieceLength / (double) Piece.DATA_LENGTH);

    }

    // TODO for SEEDER
    public boolean compareContent(File sourceFile) {
        // Creating stream and buffer to read file
        byte[] fileContent = null;
        try {
            fileContent = Files.readAllBytes(sourceFile.toPath());
        } catch (IOException e) {
            LOG.fatal("Could not readAllBytes from source file : " + sourceFile);
        }

        // Creating string of all pieces info of torrent file
        String piecesString = (String) this.getMetadata().get(Torrent.PIECES);
        byte[] piecesBytes = piecesString.getBytes();
        int i;
        for (i = 0; i < Torrent.numberOfPieces - 1; i++) {
            byte[] byteArray = Arrays.copyOfRange(fileContent, i * Torrent.pieces_length,
                    (i + 1) * Torrent.pieces_length);


            // hash the piece
            Hashage hasher = new Hashage("SHA-1");
            byte[] hashOfPieceFile = hasher.hashToByteArray(byteArray);

            // Substring corresponding to piece hash
            byte[] hashOfPieceTorrent = Arrays.copyOfRange(piecesBytes, 0, 20);

            // TODO Verify Hash
            // if (Arrays.equals(hashOfPieceFile, hashOfPieceTorrent)) {
            // // add the piece in the map
            LOG.debug("Adding piece " + i + " to dataMap");
            Torrent.dataMap.put(i, byteArray);
            // Torrent.piecesHashes.put(i, hashOfPieceFile);
            // } else {
            // LOG.error("File is not identical to it's torrent");
            // return false;
            // }

        }
        // put last piece in Map
        byte[] byteArray = Arrays.copyOfRange(fileContent, i * Torrent.pieces_length,
                (i * Torrent.pieces_length) + Torrent.lastPieceLength);
        LOG.debug("Adding piece " + i + "(last) to dataMap");
        Torrent.dataMap.put(i, byteArray);
        return true;
    }
}
