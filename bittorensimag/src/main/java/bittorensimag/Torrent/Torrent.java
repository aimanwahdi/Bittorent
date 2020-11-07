package bittorensimag.Torrent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import be.adaxisoft.bencode.InvalidBEncodingException;
import bittorensimag.Util.Util;

public class Torrent {
    public final File torrentFile;
    private FileInputStream inputStream;
    private BDecoder reader;
    private Map<String, BEncodedValue> document;
    private Map<String, BEncodedValue> info;
    private HashMap<String, Object> metadata = new HashMap<String, Object>();

    public String info_hash;
    String encoded_info_hash;

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

    public Torrent(File torrentFile) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        this.torrentFile = torrentFile;
        try {
            this.inputStream = new FileInputStream(torrentFile);
            // TODO add log level
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.reader = new BDecoder(inputStream);
        try {
            this.document = reader.decodeMap().getMap();
            if (hasInfo()) {
                this.info = this.document.get(INFO).getMap();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.hashInfo();
        this.metadata = fillMetadata();
    }

    public boolean hasInfo() {
        if (this.document.containsKey(INFO)) {
            System.out.println("The info field exists");
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
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(baos.toByteArray());
        byte[] digest = md.digest();
        String s = Util.bytesToHex(digest); // to test sha1
        String encodedHash = new String(digest, StandardCharsets.ISO_8859_1);
        this.info_hash = s;
        this.encoded_info_hash = encodedHash;
    }

    public Map<String, BEncodedValue> getDocument() {
        return this.document;
    }

    public Map<String, BEncodedValue> getInfo() {
        return this.info;
    }

    // Methods to get keys
    public String getKeyString(Map<String, BEncodedValue> map, String key) throws InvalidBEncodingException {
        try {
            if (map.containsKey(key)) {
                return map.get(key).getString();
            } else {
                System.out.println("Torrent file does not contain key : " + key);
            }
        } catch (InvalidBEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public int getKeyInt(Map<String, BEncodedValue> map, String key) throws InvalidBEncodingException {
        try {
            if (map.containsKey(key)) {
                return map.get(key).getInt();
            } else {
                System.out.println("Torrent file does not contain key : " + key);
            }
        } catch (InvalidBEncodingException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private HashMap<String, Object> fillMetadata() throws InvalidBEncodingException {
        String[] possibleKeysDocument = { ANNOUNCE, ANNOUNCE_LIST, COMMENT, CREATED_BY, ENCODING };
        String[] possibleKeysInfoString = { PIECES, NAME, MD5SUM };
        String[] possibleKeysInfoInt = { PIECE_LENGTH, PRIVATE, LENGTH };

        // Get all keys of info dictionnary
        // String keys
        for (String key : possibleKeysDocument) {
            this.metadata.put(key, getKeyString(this.document, key));
        }

        // Creation date key
        if (this.document.containsKey(CREATION_DATE)) {
            Long date = this.document.get(CREATION_DATE).getLong() * 1000L;
            if (date != 0) {
                this.metadata.put(CREATION_DATE, new Date(date));
            }
        }

        // Get all keys of info dictionnary
        // String keys
        for (String key : possibleKeysInfoString) {
            this.metadata.put(key, getKeyString(this.info, key));
        }
        // Int keys
        for (String key : possibleKeysInfoInt) {
            this.metadata.put(key, getKeyInt(this.info, key));
        }
        return this.metadata;
    }

    public HashMap<String, Object> getMetadata() {
        return this.metadata;
    }
}
