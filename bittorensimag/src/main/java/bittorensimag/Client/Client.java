package bittorensimag.Client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.MsgCoderFromWire;
import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Messages.*;
import bittorensimag.Torrent.*;
import bittorensimag.Util.Util;

public class Client {
    private static final Logger LOG = Logger.getLogger(Client.class);

    public final static String IP = "127.0.0.1";
    public final static int PORT = 6882;

    private final Torrent torrent;
    private final Tracker tracker;
    private final MsgCoderToWire coderToWire;
    private final MsgCoderFromWire coderFromWire;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private OutputStream out;
    private InputStream in;
    private Socket socket;
    boolean isSeeding;

    private boolean stillReading = true;

    public Client(Torrent torrent, Tracker tracker, MsgCoderToWire coderToWire, MsgCoderFromWire coderFromWire) {
        this.torrent = torrent;
        this.tracker = tracker;
        this.coderToWire = coderToWire;
        this.coderFromWire = coderFromWire;
        // TODO change when adding multiple peers
        // for now getting first key
        Map.Entry<String, ArrayList<Integer>> firstEntry = tracker.getPeersMap().entrySet().iterator().next();
        // Can have multiple peers on multiple ports for localhost so get first one
        this.createSocket(firstEntry.getKey(), firstEntry.getValue().get(0));
        this.createOutputStream();
        this.createInputStream();
        this.isSeeding = false;
    }

    // THIS IS FOR SEEDER NOT IMPLEMENTED YET
    public void leecherOrSeeder() throws Exception {
        File sourceFile = new File(
                this.torrent.torrentFile.getParent() + "/" + this.torrent.getMetadata().get(Torrent.NAME));
        LOG.debug("Verifying source file : " + sourceFile);
        // TODO compare content of the file (verify hash)
        if (sourceFile.exists() && sourceFile.isFile() && this.torrent.compareContent(sourceFile)) {
            this.isSeeding = true;
            LOG.info("Source file found and correct !");
            LOG.info("SEEDER MODE");
        } else {
            LOG.info("Source file not found or incorrect !");
            LOG.info("LEECHER MODE");
        }
    }

    public void startCommunication() throws IOException {
        LOG.debug("Starting communication with the peer");
        Handshake.sendMessage(this.torrent.info_hash, this.out);
        try {
            while (this.receivedMsg(this.dataIn, this.out, this.coderToWire, this.coderFromWire)) {
                ;
            }
        } catch (IOException ioe) {
            LOG.error("Error handling client: " + ioe.getMessage());

        }

    }

    private void createOutputStream() {
        LOG.debug("Creation of the OutputStream");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        this.dataOut = new DataOutputStream(byteStream);
        LOG.debug("OutputStream created");
    }

    private void createInputStream() {
        InputStream inputStream;
        try {
            LOG.debug("Creation of the InputStream");
            inputStream = this.socket.getInputStream();
            this.dataIn = new DataInputStream(inputStream);
            LOG.debug("InputStream created");
        } catch (IOException e) {
            LOG.fatal("Could not create InputStream");
        }

    }

    private void createSocket(String destAddr, int destPort) {
        try {
            LOG.debug("Creration of the socket for " + destAddr + " and port " + destPort);
            this.socket = new Socket(destAddr, destPort);
            this.out = this.socket.getOutputStream();
            LOG.debug("Socket created");
        } catch (IOException e) {
            LOG.fatal("Could not create Socket");
        }
    }

    private boolean receivedMsg(DataInputStream in, OutputStream out, MsgCoderToWire coderToWire,
            MsgCoderFromWire coderFromWire) throws IOException {

        Object msgReceived = coderFromWire.fromWire(in);

        if (msgReceived instanceof Handshake) {
            Handshake handshake = (Handshake) msgReceived;
            return this.handleHandshake(handshake, out);
        }
            
        // cast to message to get type
        Msg msg = (Msg) msgReceived;
        int msgType = msg.getMsgType();

        if (msgType < 0 || msgType > 8) {
            return false;
        }
        LOG.debug("Handling " + msgType + " message");
        // cast to specific message and doing logic
        switch (msgType) {
            case Simple.CHOKE:
                Simple choke = (Simple) msgReceived;
                this.closeConnection(in);
                break;
            case Simple.UNCHOKE:
                Simple unChoke = (Simple) msgReceived;
                // send first request message
                // TODO send next request correponding to dataMap our client already has
                Request.sendMessageForIndex(0, Torrent.numberOfPartPerPiece, out);
                break;
            case Simple.INTERESTED:
                Simple interested = (Simple) msgReceived;
                Simple.sendMessage(Simple.UNCHOKE, out);
                break;
            case Simple.NOTINTERESTED:
                Simple notInterested = (Simple) msgReceived;
                Simple.sendMessage(Simple.CHOKE, out);
                this.closeConnection(in);
                break;
            case Have.HAVE_TYPE:
                Have have = (Have) msgReceived;
                // TODO stocker client dans map pour suivre quel client a quelle pièce
                break;
            case Bitfield.BITFIELD_TYPE:
                Bitfield bitfield = (Bitfield) msgReceived;
                if (!isSeeding) {
                    Simple.sendMessage(Simple.INTERESTED, out);
                }
                break;
            case Request.REQUEST_TYPE:
                Request request = (Request) msgReceived;
                this.handleRequest(request, out);
                break;
            case Piece.PIECE_TYPE:
                Piece piece = (Piece) msgReceived;
                this.handlePieceMsg(in, piece, out);
                break;
            // TODO if implementing endgame
            // case CANCEL.CANCEL_TYPE:

            // break;
            default:
                // never reached test before;
                break;
        }

        return stillReading;
    }

    private boolean handleHandshake(Handshake handshake, OutputStream out2) throws IOException {
        LOG.debug("Handling Handshake message");
        if (handshake.getSha1Hash().compareTo(this.torrent.info_hash) != 0) {
            LOG.error("Sha1 hash received different from torrent file");
            return false;
        }
        // who send handshake first ?
        // Handshake.sendMessage(this.torrent.info_hash, out);
        if (isSeeding) {
            Bitfield.sendMessage(new byte[] { (byte) 0xff, (byte) 0xf0 }, out);
        } else {
            Bitfield.sendMessage(new byte[] { 0, 0 }, out);
        }
        return true;
    }

    private void handleRequest(Request request, OutputStream out) throws IOException {
        LOG.debug("Handling Request message");
        int pieceIndex = request.getIndex();
        int beginOffset = request.getBeginOffset();
        int pieceLength = request.getPieceLength();
        byte[] pieceData = Torrent.dataMap.get(pieceIndex);
        byte[] partData = Arrays.copyOfRange(pieceData, beginOffset, beginOffset + pieceLength);

        LOG.debug("Sending pieces for");
        Piece.sendMessage(pieceLength + Piece.HEADER_LENGTH, pieceIndex, beginOffset, partData, out);
    }

    // TODO send new request if fail for a part
    private void handlePieceMsg(DataInputStream in, Piece piece, OutputStream out) throws IOException {
        LOG.debug("Handling Piece message");
        int pieceIndex = piece.getPieceIndex();
        int beginOffset = piece.getBeginOffset();
        byte[] data = piece.getData();

        LOG.debug("Piece with index " + pieceIndex + " with beginOffset " + beginOffset);

        this.addToMap(pieceIndex, data);

        if (pieceIndex < Torrent.numberOfPieces - 1) {
            // request only if last part of piece has been received
            if (beginOffset == Torrent.pieces_length - Piece.DATA_LENGTH) {
                Have.sendMessage(pieceIndex, out);
                Request.sendMessageForIndex(++pieceIndex, Torrent.numberOfPartPerPiece, out);
            }
        } else {
            // last piece
            if (beginOffset == Torrent.pieces_length - Piece.DATA_LENGTH) {
                // last part of last piece received
                Have.sendMessage(pieceIndex, out);
                Simple.sendMessage(Simple.NOTINTERESTED, out);
                this.closeConnection(in);
                stillReading = false;
        }

    }
}

    private void addToMap(int pieceIndex, byte[] data) {
        LOG.debug("Adding piece " + pieceIndex + " to the map");
        // TODO add beginOffset in case parts do not arrive in order
        if (Torrent.dataMap.containsKey(pieceIndex)) {
            LOG.debug("Piece already in map, concatenate to piece");
            Torrent.dataMap.replace(pieceIndex, Util.concat(Torrent.dataMap.get(pieceIndex), data));
        } else {
            Torrent.dataMap.put(pieceIndex, data);
        }
    }

    private void closeConnection(DataInputStream in) throws IOException {
        in.close();
        this.socket.close();
        LOG.info("Connection closed");
    }

}