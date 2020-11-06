package bittorensimag.Torrent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
// import java.util.Scanner;

import java.security.NoSuchAlgorithmException;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;

import bittorensimag.Util.Util;

public class Tracker {
    Torrent torrent;
    private String url;
    private String peer_id;
    public final int port;
    private int uploaded;
    private int downloaded;
    private int left;
    // private int numwant;
    private int compact;
    private String event;

    private String query;

    private Map<String, BEncodedValue> answer;

    public Tracker(Torrent torrent) throws NoSuchAlgorithmException, IOException {
        this.torrent = torrent;
        this.url = (String) this.torrent.getMetadata().get("announce");
        this.peer_id = "-" + "BE" + "0001" + "-" + Util.generateRandomAlphanumeric(12);
        // TODO need to try ports available from 6881 to 6889
        this.port = 6881;
        this.downloaded = 0;
        this.uploaded = 0;
        // TODO how to calculate numwant ? Not equal to length in byte of the file ???
        this.left = 806512;
        // this.numwant = 50;
        this.compact = 1;
        this.event = "started";
        this.query = "";
        this.generateUrl();
    }

    private void generateUrl() throws UnsupportedEncodingException {
        try {
            this.query += "info_hash=" + URLEncoder.encode(this.torrent.encoded_info_hash, "ISO_8859_1") + "&"
                    + "peer_id="
                    + URLEncoder.encode(this.peer_id, "UTF-8") + "&" + "port=" + this.port + "&" + "uploaded="
                    + this.uploaded + "&" + "downloaded=" + this.downloaded + "&" + "left=" + this.left + "&"
                    + "compact=" + this.compact + "&" + "event=" + this.event;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void getRequest() throws IOException {
        URLConnection connection;
        try {
            System.out.println(this.query);
            connection = new URL(this.url + "?" + this.query).openConnection();

            connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.toString());
            try {
                InputStream stream = connection.getInputStream();
                // Debug to output answer of the tracker
                // try (Scanner scanner = new Scanner(stream)) {
                // String output = scanner.useDelimiter("\\A").next();
                // System.out.println(output);
                // }
                this.decodeAnswer(stream);
            } catch (IOException e) {
                System.err.println("Did not receive the answer of the tracker is he running in the background ?");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void decodeAnswer(InputStream stream) {
        BDecoder reader = new BDecoder(stream);
        try {
            this.answer = reader.decodeMap().getMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
