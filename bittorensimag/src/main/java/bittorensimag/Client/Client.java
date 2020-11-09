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

import bittorensimag.MessageCoder.MsgCoderFromWire;
import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Messages.*;
import bittorensimag.Torrent.*;
import bittorensimag.Util.Util;

public class Client {
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
        // TODO compare content of the file (verify hash)
        if (sourceFile.exists() && sourceFile.isFile() && this.torrent.compareContent(sourceFile)) {
            this.isSeeding = true;
            System.out.println("Source file found and correct !");
            System.out.println("SEEDER MODE");
        } else {
            System.out.println("Source file not found or incorrect !");
            System.out.println("LEECHER MODE");
        }
    }

    public void startCommunication() {
        Handshake.sendMessage(this.torrent.info_hash, this.out);
        try {
            while (this.receivedMsg(this.dataIn, this.out, this.coderToWire, this.coderFromWire)) {
                ;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("Error handling client: " + ioe.getMessage());

        }

    }

    private void createOutputStream() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        this.dataOut = new DataOutputStream(byteStream);
    }

    private void createInputStream() {
        InputStream inputStream;
        try {
            inputStream = this.socket.getInputStream();
            this.dataIn = new DataInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createSocket(String destAddr, int destPort) {
        try {
            this.socket = new Socket(destAddr, destPort);
            this.out = this.socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
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
        if (handshake.getSha1Hash().compareTo(this.torrent.info_hash) != 0) {
            System.err.println("Sha1 hash received different from torrent file");
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
        int pieceIndex = request.getIndex();
        int beginOffset = request.getBeginOffset();
        int pieceLength = request.getPieceLength();
        byte[] pieceData = Torrent.dataMap.get(pieceIndex);
        byte[] partData = Arrays.copyOfRange(pieceData, beginOffset, beginOffset + pieceLength);

        Piece.sendMessage(pieceLength + Piece.HEADER_LENGTH, pieceIndex, beginOffset, partData, out);
    }

    // TODO send new request if fail for a part
    private void handlePieceMsg(DataInputStream in, Piece piece, OutputStream out) throws IOException {
        int pieceIndex = piece.getPieceIndex();
        int beginOffset = piece.getBeginOffset();
        byte[] data = piece.getData();

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
        // add the piece in the map
        // TODO add beginOffset in case parts do not arrive in order
        if (Torrent.dataMap.containsKey(pieceIndex)) {
            Torrent.dataMap.replace(pieceIndex, Util.concat(Torrent.dataMap.get(pieceIndex), data));
        } else {
            Torrent.dataMap.put(pieceIndex, data);
        }
    }

    private void closeConnection(DataInputStream in) throws IOException {
        in.close();
        this.socket.close();
    }

}