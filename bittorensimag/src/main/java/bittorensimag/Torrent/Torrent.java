package bittorensimag.Torrent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import be.adaxisoft.bencode.InvalidBEncodingException;
import bittorensimag.Client.Output;
import bittorensimag.Messages.Bitfield;
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

    public static int numberOfPartPerPiece;
    public static int numberOfPieces;
    public static int lastPieceLength;
    public static int lastPieceNumberOfPart;
    public static int lastPartLength;
    public static int pieces_length;
    public static int totalSize;

    public static Map<Integer, byte[]> dataMap = new HashMap<Integer, byte[]>();
    public static Map<Integer, byte[]> piecesHashes = new HashMap<Integer, byte[]>();


    public final static String INFO = "info";

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
    private final String[] possibleKeysInfoString = { NAME };
    private final String[] possibleKeysInfoInt = { PIECE_LENGTH, PRIVATE, LENGTH };
    private final String[] possibleKeysInfoBytes = { PIECES, MD5SUM };

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
        this.fillPiecesHashes();
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
        byte[] digest = Hashage.sha1Hasher.hashToByteArray(baos.toByteArray());
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

        MapUtil.fillBencodeMapBytes(this.info, this.metadata, possibleKeysInfoBytes);

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
        Torrent.totalSize = (int) this.getMetadata().get(LENGTH);
        Torrent.numberOfPieces = (int) Math
                .ceil((double) totalSize / (double) (Torrent.numberOfPartPerPiece * Piece.DATA_LENGTH));
        Torrent.lastPieceLength = totalSize % (Torrent.numberOfPartPerPiece * Piece.DATA_LENGTH);
        Torrent.lastPartLength = totalSize % Piece.DATA_LENGTH;
        Torrent.lastPieceNumberOfPart = (int) Math.ceil((double) Torrent.lastPieceLength / (double) Piece.DATA_LENGTH);

    }

    private void fillPiecesHashes() {
        // Creating string of all pieces info of torrent file
        byte[] piecesBytes = (byte[]) this.getMetadata().get(Torrent.PIECES);
        int i;
        for (i = 0; i < Torrent.numberOfPieces; i++) {
            // Substring corresponding to piece hash
            byte[] hashOfPieceTorrent = Arrays.copyOfRange(piecesBytes, i * 20, (i + 1) * 20);
            Torrent.piecesHashes.put(i, hashOfPieceTorrent);
        }
    }

    public boolean fillBitfield(Output file) throws IOException {
        boolean isComplete = true;
        ;

        ByteBuffer buffer = ByteBuffer.allocate(Torrent.pieces_length);
        byte[] pieceData;
        BitSet bitSet = new BitSet(8);
        int numberOfBytes = 0;

        FileChannel channel = file.getInChannel();

        for (; numberOfBytes < Torrent.numberOfPieces / 8; numberOfBytes++) {
            for (int b = 0; b < 8; b++) {
                int pieceNumber = b + 8 * numberOfBytes;
                if (channel.read(buffer) == -1) {
                    LOG.debug("Error reading file in buffer");
                }
                pieceData = buffer.array();
                if (Piece.testPieceHash(pieceNumber, pieceData)) {
                    LOG.debug("Piece " + pieceNumber + " correct");
                    bitSet.set(b);
                } else {
                    // else bit stays as false
                    isComplete = false;
                }
                buffer.clear();
            }
            // each 8 bits we create a byte
            if (bitSet.toByteArray().length == 0) {
                // if no byte are set to true
                Bitfield.setByteInBitfield(numberOfBytes, (byte) 0x0);
            } else {
                Bitfield.setByteInBitfield(numberOfBytes, Util.reverseBitsByte(bitSet.toByteArray()[0]));
            }
            bitSet.clear();
        }

        // last pieces of uncomplete byte to read
        for (int b = 0; b < Torrent.numberOfPieces % 8; b++) {
            int pieceNumber = b + 8 * numberOfBytes;
            if (pieceNumber == Torrent.numberOfPieces - 1) {
                buffer = ByteBuffer.allocate(Torrent.lastPieceLength);
            }
            if (channel.read(buffer) == -1) {
                LOG.debug("Error reading file in buffer");
            }
            pieceData = buffer.array();
            if (Piece.testPieceHash(pieceNumber, pieceData)) {
                LOG.debug("Piece " + pieceNumber + " correct");
                bitSet.set(b);
            } else {
                // else bit stays as false
                isComplete = false;
            }
            buffer.clear();
        }
        // create the last byte
        if (bitSet.toByteArray().length == 0) {
            // if no byte are set to true
            Bitfield.setByteInBitfield(numberOfBytes, (byte) 0x0);
        } else {
            Bitfield.setByteInBitfield(numberOfBytes, Util.reverseBitsByte(bitSet.toByteArray()[0]));
        }
        bitSet.clear();

        file.closeInChannel();
        return isComplete;
    }
}
