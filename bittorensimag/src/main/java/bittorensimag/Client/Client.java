package bittorensimag.Client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
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
    private boolean isSeeding;

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

    // THIS IS ALL FOR SEEDER NOT IMPLEMENTED YET

    // public void leecherOrSeeder() throws Exception {
    // File sourceFile = new File(
    // this.torrent.torrentFile.getParent() + "/" +
    // this.torrent.getMetadata().get(Torrent.NAME));
    // if (sourceFile.exists() && sourceFile.isFile() &&
    // this.verifyContent(sourceFile)) {
    // this.isSeeding = true;
    // System.out.println("Source file found and correct !");
    // System.out.println("SEEDER MODE");
    // } else {
    // System.out.println("Source file not found or incorrect !");
    // System.out.println("LEECHER MODE");
    // }
    // }

    // private boolean verifyContent(File sourceFile) throws Exception {
    // // Creating stream and buffer to read file
    // DataInputStream sourceDataStream = new DataInputStream(new
    // FileInputStream(sourceFile));

    // // Creating string of all pieces info of torrent file
    // String piecesString = (String)
    // this.torrent.getMetadata().get(Torrent.PIECES);
    // byte[] piecesBytes = piecesString.getBytes();

    // for (int i = 0; i < this.torrent.numberOfPieces; i++) {
    // ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();

    // for (int j = 0; j < Piece.DATA_LENGTH * 2; j++) {
    // try {
    // int nextByte = sourceDataStream.readUnsignedByte();
    // messageBuffer.write(nextByte);

    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }
    // // hash the piece
    // Hashage hasher = new Hashage("SHA-1");
    // byte[] hashOfPieceFile = hasher.hashToByteArray(messageBuffer.toByteArray());

    // // Substring corresponding to piece hash
    // byte[] hashOfPieceTorrent = Arrays.copyOfRange(piecesBytes, 0, 20);

    // if (Arrays.equals(hashOfPieceFile, hashOfPieceTorrent)) {
    // // add the piece in the map
    // this.dataMap.put(i, messageBuffer.toByteArray());
    // this.piecesHashes.put(i, hashOfPieceFile);
    // } else {
    // System.out.println("File is not identical to it's torrent");
    // return false;
    // }

    // }
    // // TODO last piece wiht this.lastPieceLength
    // return true;
    // }

    // private boolean verifyHandshake(DataInputStream in) {
    // String sha1 = "";
    // // read protocol name
    // readMessage(in, 19);

    // // read extension bytes
    // readMessage(in, 8);

    // // read sha1 hash
    // for (int i = 0; i < 20; i++) {
    // try {
    // int nextByte = in.readUnsignedByte();
    // sha1 += nextByte;

    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }

    // // read peerId
    // readMessage(in, 20);

    // if (sha1.equals(this.torrent.info_hash)) {
    // return true;
    // }

    // return false;
    // }

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
            // who send handshake first ?
            // Handshake.sendMessage(this.torrent.info_hash, out);
            Bitfield.sendMessage(new byte[] { 0, 0 }, out);
            return true;
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
                break;
            case Have.HAVE_TYPE:
                Have have = (Have) msgReceived;
                // TODO stocker client dans map pour suivre quel client a quelle pièce
                break;
            case Bitfield.BITFIELD_TYPE:
                Bitfield bitfield = (Bitfield) msgReceived;
                Simple.sendMessage(Simple.INTERESTED, out);
                break;
            case Request.REQUEST_TYPE:
                Request request = (Request) msgReceived;
                // TODO send pieces that client requested
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