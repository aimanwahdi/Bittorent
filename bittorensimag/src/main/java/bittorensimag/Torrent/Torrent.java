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
    private File torrentFile;
    private FileInputStream inputStream;
    private BDecoder reader;
    private Map<String, BEncodedValue> document;
    private Map<String, BEncodedValue> info;
    private HashMap<String, Object> metadata = new HashMap<String, Object>();

    public String info_hash;
    String encoded_info_hash;

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
                this.info = this.document.get("info").getMap();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.hashInfo();
        this.metadata = fillMetadata();
    }

    public boolean hasInfo() {
        if (this.document.containsKey("info")) {
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
        String[] possibleKeysDocument = { "announce", "announce-list", "comment", "created by", "encoding" };
        String[] possibleKeysInfoString = { "pieces", "name", "pieces", "md5sum" };
        String[] possibleKeysInfoInt = { "piece length", "private", "length" };

        // Get all keys of info dictionnary
        // String keys
        for (String key : possibleKeysDocument) {
            this.metadata.put(key, getKeyString(this.document, key));
        }

        // Creation date key
        if (this.document.containsKey("creation date")) {
            Long date = this.document.get("creation date").getLong() * 1000L;
            if (date != 0) {
                this.metadata.put("creation date", new Date(date));
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
