package bittorensimag.Tracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;

public class Tracker {
    private File torrentFile;
    private FileInputStream inputStream;
    private BDecoder reader;
    private Map<String, BEncodedValue> document;

    public Tracker(File torrentFile) throws FileNotFoundException, IOException {
        this.torrentFile = torrentFile;
        try {
            this.inputStream = new FileInputStream(torrentFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.reader = new BDecoder(inputStream);
        try {
            this.document = reader.decodeMap().getMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, BEncodedValue> getDocument() {
        return this.document;
    }

    public String getAnnounce() throws InvalidBEncodingException {
        String announce = this.document.get("announce").getString(); // Strings
        System.out.println("Announce url : " + announce);
        return announce;
    }

    public boolean hasInfo() {
        if (this.document.containsKey("info")) {
            System.out.println("The info field exists");
            return true;
        } else {
            return false;
        }
    }

    public List<BEncodedValue> getFileInfo() {
        if (hasInfo()) {
            Map<String, BEncodedValue> info;
            try {
                info = this.document.get("info").getMap();
                List<BEncodedValue> files = info.get("files").getList(); // Lists
                return files;
            } catch (InvalidBEncodingException e) {
                e.printStackTrace();
            } // Maps
        }
        return null;
    }
}
