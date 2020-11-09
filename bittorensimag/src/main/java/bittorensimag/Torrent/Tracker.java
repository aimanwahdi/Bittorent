package bittorensimag.Torrent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
// import java.util.Scanner;

import java.security.NoSuchAlgorithmException;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import bittorensimag.Client.Client;
import bittorensimag.Util.MapUtil;
import bittorensimag.Util.IPv4ValidatorRegex;
import bittorensimag.Util.Util;

public class Tracker {
    Torrent torrent;
    private String url;
    private String peer_id;
    public final int PORT = 6969;
    private int uploaded;
    private int downloaded;
    private int left;
    // private int numwant;
    private int compact;
    private String peerIP;
    private int peerPort;
    private int numberOfPeers;

    private String query;

    private HashMap<String, Object> answer = new HashMap<String, Object>();
    private HashMap<String, ArrayList<Integer>> peersMap = new HashMap<String, ArrayList<Integer>>();

    public final static String INCOMPLETE = "incomplete";
    public final static String PEERS = "peers";
    public final static String INTERVAL = "interval";
    public final static String COMPLETE = "complete";
    public final static String DOWNLOADED = "downloaded";
    public final static String MIN_INTERVAL = "min interval";

    public final static String EVENT_STARTED = "started";
    public final static String EVENT_COMPLETED = "completed";
    public final static String EVENT_STOPPED = "stopped";

    private final String[] possibleKeysAnswerInt = { INCOMPLETE, INTERVAL, COMPLETE, DOWNLOADED, MIN_INTERVAL };

    public Tracker(Torrent torrent) throws NoSuchAlgorithmException, IOException {
        this.torrent = torrent;
        this.url = (String) this.torrent.getMetadata().get(Torrent.ANNOUNCE);
        this.peer_id = "-" + "BE" + "0001" + "-" + Util.generateRandomAlphanumeric(12);
        // TODO need to try ports available from 6881 to 6889
        this.downloaded = 0;
        this.uploaded = 0;
        // TODO how to calculate numwant ? Not equal to length in byte of the file ???
        // this.left = 806512;
        // this.numwant = 50;
        this.compact = 1;
        this.query = "";
        this.peerIP = "";
    }

    public void generateUrl(String event) throws UnsupportedEncodingException {
        try {
            this.query = "info_hash=" + URLEncoder.encode(this.torrent.encoded_info_hash, "ISO_8859_1") + "&peer_id="
                    + URLEncoder.encode(this.peer_id, "UTF-8") + "&port=" + Client.PORT + "&uploaded=" + this.uploaded
                    + "&downloaded="
                    + this.downloaded + "&compact=" + this.compact + "&event=" + event;
            ;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void getRequest(String event) throws IOException {
        System.out.println("Sending GET request to the tracker for event=" + event);
        URLConnection connection;
        try {
            URL url = new URL(this.url + "?" + this.query);
            URL newUrl = new URL(url.getProtocol(), url.getHost(), PORT, url.getFile());
            connection = newUrl.openConnection();

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
            Map<String, BEncodedValue> encodedAnswer = reader.decodeMap().getMap();

            MapUtil.fillBencodeMapBytes(encodedAnswer, this.answer, new String[] { PEERS });

            this.getPeerIPPort();

            MapUtil.fillBencodeMapInt(encodedAnswer, this.answer, possibleKeysAnswerInt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getPeerIPPort() {
        byte[] peersAnswer = (byte[]) this.answer.get(PEERS);

        if (peersAnswer.length == 0) {
            System.out.println("No peers in the swarm (from tracker)");
            return;
        }

        this.numberOfPeers = peersAnswer.length / 6; 
        if (peersAnswer.length % 6 != 0) {
            System.err.println("Warning ! Peers from tracker does not respect specification");
        }

        int i = 0, j = 0;
        for (i = 0; i < numberOfPeers; i++) {
            this.peerIP = "";

            for (j = 6 * i; j < 6 * i + 3; j++) {
                this.peerIP += (int) peersAnswer[j] + ".";
            }

            this.peerIP += (int) peersAnswer[j];
            this.peerPort = Integer.parseInt(Util.bytesToHex(Arrays.copyOfRange(peersAnswer, ++j, j + 2)), 16);

            if (!IPv4ValidatorRegex.isValid(this.peerIP) || this.peerPort == 0) {
                System.err.println("Peer IP or port is not valid : IP=" + this.peerIP + " port=" + this.peerPort);
                continue;
            }

            if (this.peerPort == Client.PORT && this.peerIP.compareTo(Client.IP) == 0) {
                continue;
            }

            // add the peer in the map
            if (this.peersMap.containsKey(this.peerIP)) {
                ArrayList<Integer> portList = this.peersMap.get(this.peerIP);
                if (!portList.contains(this.peerPort)) {
                    portList.add(this.peerPort);
                }
            } else {
                this.peersMap.put(this.peerIP, new ArrayList<Integer>(Arrays.asList(this.peerPort)));
            }
        }
    }

    public HashMap<String, ArrayList<Integer>> getPeersMap() {
        return this.peersMap;
    }

    public boolean foundAnotherPeer() throws IOException {
        if (this.peersMap.isEmpty()) {
            System.err.println("There is not another peer please restart Vuze");
            return false;
        }

        return true;
    }
}
