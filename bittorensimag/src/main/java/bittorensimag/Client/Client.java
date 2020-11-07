package bittorensimag.Client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Messages.*;
import bittorensimag.Torrent.*;
import bittorensimag.Util.Hashage;
import bittorensimag.Util.Util;

public class Client {
    private final Torrent torrent;
    private final Tracker tracker;
    private final MsgCoderToWire coder;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private OutputStream out;
    private InputStream in;
    private Socket socket;
    private boolean isSeeding;

    private int numberOfPartPerPiece;
    private int numberOfPieces;
    private int lastPieceLength;
    private int lastPieceNumberOfPart;
    private int lastPartLength;
 
    private Map<Integer, byte[]> fileData;
    private Map<Integer, byte[]> piecesHashes;
    private byte[] data;

    private final String LOCALHOST = "127.0.0.1";
    private int numberOfReceivedPieces = 0;
   

    public Client(Torrent torrent, Tracker tracker, MsgCoderToWire coder) {
        this.torrent = torrent;
        this.tracker = tracker;
        this.coder = coder;
        this.fileData = new HashMap<Integer, byte[]>();
        this.createSocket(LOCALHOST, tracker.port);
        this.createOutputStream();
        this.createInputStream();
        this.calculateNumberParts();
        this.calculateNumberPieces();
        this.isSeeding = false;
    }

    public void leecherOrSeeder() throws Exception {
        File sourceFile = new File(
                this.torrent.torrentFile.getParent() + "/" + this.torrent.getMetadata().get(Torrent.NAME));
        if (sourceFile.exists() && sourceFile.isFile()) {
            if (this.verifyContent(sourceFile)) {
                this.isSeeding = true;
                System.out.println("Source file found and correct !");
                System.out.println("SEEDER MODE");
            }
        } else {
            System.out.println("Source file not found");
            System.out.println("LEECHER MODE");
        }
    }

    private boolean verifyContent(File sourceFile) throws Exception {
        // Creating stream and buffer to read file
        DataInputStream sourceDataStream = new DataInputStream(new FileInputStream(sourceFile));

        // Creating string of all pieces info of torrent file
        String piecesString = (String) this.torrent.getMetadata().get(Torrent.PIECES);
        byte[] piecesBytes = piecesString.getBytes();

        for (int i = 0; i < this.numberOfPieces; i++) {
            ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();

            for (int j = 0; j < Piece.DATA_LENGTH * 2; j++) {
                try {
                    int nextByte = sourceDataStream.readByte();
                    messageBuffer.write(nextByte);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // hash the piece
            Hashage hasher = new Hashage("SHA-1");
            byte[] hashOfPieceFile = hasher.hashToByteArray(messageBuffer.toByteArray());

            // Substring corresponding to piece hash
            byte[] hashOfPieceTorrent = Arrays.copyOfRange(piecesBytes, 0, 20);

            if (Arrays.equals(hashOfPieceFile, hashOfPieceTorrent)) {
                // add the piece in the map
                this.fileData.put(i, messageBuffer.toByteArray());
                this.piecesHashes.put(i, hashOfPieceFile);
            } else {
                System.out.println("File is not identical to it's torrent");
                return false;
            }

        }
        // TODO last piece wiht this.lastPieceLength
        return true;
    }

    private boolean verifyHandshake(DataInputStream in) {
        String sha1 = "";
        // read protocol name
        readMessage(in, 19);

        // read extension bytes
        readMessage(in, 8);

        // read sha1 hash
        for (int i = 0; i < 20; i++) {
            try {
                int nextByte = in.readByte();
                sha1 += nextByte;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // read peerId
        readMessage(in, 20);

        if (sha1.equals(this.torrent.info_hash)) {
            return true;
        }

        return false;
    }

    private void calculateNumberParts() {
        int pieces_length = (int) this.torrent.getMetadata().get(Torrent.PIECE_LENGTH);
        this.numberOfPartPerPiece = pieces_length / Piece.DATA_LENGTH;
        if (pieces_length % Piece.DATA_LENGTH != 0) {
            System.err.println("Warning : pieces length is not a multiple of 16Kb");
        }
    }

    private void calculateNumberPieces() {
        int length = (int) this.torrent.getMetadata().get(Torrent.LENGTH);
        this.numberOfPieces = (int) Math
                .ceil((double) length / (double) (this.numberOfPartPerPiece * Piece.DATA_LENGTH));
        this.lastPieceLength = length % (this.numberOfPartPerPiece * Piece.DATA_LENGTH);
        this.lastPartLength = length % Piece.DATA_LENGTH;
        this.lastPieceNumberOfPart = (int) Math.ceil((double) this.lastPieceLength / (double) Piece.DATA_LENGTH);
        this.data = new byte[length];
    }

    public void startCommunication() {
        this.sendHandshake();
        try {
            while (this.receivedMsg(this.dataIn, this.out, this.coder)) {
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

    private void sendHandshake() {
        Handshake handshakeMsg = new Handshake(this.torrent.info_hash);
        try {
            this.frameMsg(this.coder.toWire(handshakeMsg), this.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // writing a message in OutputStream
    private void frameMsg(byte[] message, OutputStream out) throws IOException {
        // write message
        System.out.println("writing");
        out.write(message);
        out.flush();

    }

    private void readMessage(DataInputStream in, int length) {
        for (int i = 0; i < length; i++) {
            try {
                int nextByte = in.readByte();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int readEndPiece(DataInputStream in) throws IOException {
        // read piece index and begin offset of the first piece
        int pieceIndex1 = in.readInt();
        int beginOffset1 = in.readInt();

        // read all the data sent in the first piece
        this.readData(in, Piece.DATA_LENGTH, pieceIndex1);
        System.out.println("pieceIndex1 " + pieceIndex1);

        return pieceIndex1;
    }

    private int readPieces(DataInputStream in, int numberOfParts) throws IOException {
        int pieceIndex1 = this.readEndPiece(in);
        // read the rest of the pieces
        for (int i = 0; i < numberOfParts - 1; i++) {

            int length2 = in.readInt();
            int type2 = in.readByte();
            int pieceIndex2 = in.readInt();
            int beginOffset2 = in.readInt();
            this.readData(in, Piece.DATA_LENGTH, pieceIndex2);

        }

        return pieceIndex1;
    }

    private int readLastPiece(DataInputStream in, int numberOfParts) throws IOException {
        int pieceIndex1 = this.readEndPiece(in);
        // read the rest of the pieces
        if (numberOfParts >= 2) {
            for (int i = 0; i < numberOfParts - 2; i++) {

                int length2 = in.readInt();
                int type2 = in.readByte();
                int pieceIndex2 = in.readInt();
                int beginOffset2 = in.readInt();
                this.readData(in, Piece.DATA_LENGTH, pieceIndex2);

            }
            // last one has different length
            int length2 = in.readInt();
            int type2 = in.readByte();
            int pieceIndex2 = in.readInt();
            int beginOffset2 = in.readInt();
            this.readData(in, this.lastPartLength, pieceIndex2);
        }

        return pieceIndex1;
    }

    /*
     * read all the data of a piece and put it the map
     */
    private void readData(DataInputStream in, int length, int pieceIndex) {
        ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();

        for (int i = 0; i < length; i++) {
            try {
                int nextByte = in.readByte();
                messageBuffer.write(nextByte);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // add the piece in the map
        if (fileData.containsKey(pieceIndex)) {
            fileData.replace(pieceIndex, Util.concat(fileData.get(pieceIndex), messageBuffer.toByteArray()));
        } else {
            fileData.put(pieceIndex, messageBuffer.toByteArray());
        }
    }

    private void sendBitfield(byte[] dataBitfield) throws IOException {
        Bitfield msgBitfield = new Bitfield(dataBitfield);
        this.frameMsg(coder.toWire(msgBitfield), this.out);
    }

    private void sendUnchoke() throws IOException {
        Msg unchoke = new Msg(Simple.LENGTH, Simple.INTERESTED);
        this.frameMsg(coder.toWire(unchoke), this.out);
    }

    private void sendInterested() throws IOException {
        this.frameMsg(coder.toWire(new Msg(Simple.LENGTH, Simple.INTERESTED)), this.out);
    }

    private void sendNotInterested() throws IOException {
        this.frameMsg(coder.toWire(new Msg(Simple.LENGTH, Simple.NOTINTERESTED)), this.out);
    }

    private void sendHave(int index) throws IOException {
        Have msgHave = new Have(index);
        this.frameMsg(coder.toWire(msgHave), this.out);
    }

    private void sendRequest(int index, int beginOffset) throws IOException {
        Request msgRequest = new Request(index, beginOffset);
        this.frameMsg(coder.toWire(msgRequest), this.out);
    }

    private void sendRequestsForSameIndex(int pieceIndex, int numberOfParts) throws IOException {
        for (int j = 0; j < numberOfParts; j++) {
            sendRequest(pieceIndex, j * Piece.DATA_LENGTH);
        }
    }

    private void sendLastRequest(int pieceIndex, int numberOfParts) throws IOException {
        for (int j = 0; j < numberOfParts - 1; j++) {
            sendRequest(pieceIndex, j * Piece.DATA_LENGTH);
        }
        Request msgRequest = new Request(pieceIndex, (numberOfParts - 1) * Piece.DATA_LENGTH, this.lastPartLength);
        this.frameMsg(coder.toWire(msgRequest), this.out);
    }

    private boolean receivedMsg(DataInputStream in, OutputStream Out, MsgCoderToWire coder) throws IOException {

        int firstByte;
        try {
            firstByte = in.readByte();
        } catch (EOFException e) {
            System.out.println("No more messages to read ! Is Vuze opened and seeding ?");
            return false;
        }
        ;
        if (firstByte == Handshake.HANDSHAKE_LENGTH) {
            System.out.println("Received Message : Handshake");

            // reading the rest of handshake msg
            this.readMessage(in, 67);

            // reading recieved bietfield msg
            this.readMessage(in, 7);

            this.sendBitfield(new byte[] { 0, 0 });
            this.sendInterested();

        } else {
            int secondByte = in.readByte();
            int thirdByte = in.readByte();
            int fourthByte = in.readByte();

            int type = in.readByte();
            System.out.println("Received Message : " + Msg.messagesNames.get(type));

            switch (type) {
                case Simple.UNCHOKE:
                    System.out.println("received unchoke message");
                    // this.sendAllRequests();

                    // send first request message
                    sendRequest(0, 0);
                    sendRequest(0, Piece.DATA_LENGTH);

                    break;
                case Piece.PIECE_TYPE:
                    System.out.println("received piece message");

                    System.out.println(this.numberOfPieces);
                    System.out.println(this.numberOfReceivedPieces);

                    // before before last piece
                    if (this.numberOfReceivedPieces < this.numberOfPieces - 2) {

                        int pieceIndex = readPieces(in, this.numberOfPartPerPiece);

                        this.sendHave(pieceIndex);

                        this.sendRequestsForSameIndex(pieceIndex + 1, this.numberOfPartPerPiece);
                    }
                    // before last piece
                    else if (this.numberOfReceivedPieces == this.numberOfPieces - 2) {
                        int pieceIndex = readPieces(in, this.numberOfPartPerPiece);
                        this.sendHave(pieceIndex);

                        this.sendLastRequest(pieceIndex + 1, this.lastPieceNumberOfPart);
                        this.sendHave(pieceIndex + 1);

                    }
                    // last piece
                    else if (this.numberOfReceivedPieces == this.numberOfPieces - 1) {
                        int pieceIndex = readLastPiece(in, this.lastPieceNumberOfPart);
                        this.sendHave(pieceIndex);
                        this.sendNotInterested();
                    }

                    this.numberOfReceivedPieces++;
                    break;
                case Simple.CHOKE:
                    this.closeConnection(in);
                // if the type is not correct leave
                default:
                    return false;
            }
        }
        return true;
    }

    private void closeConnection(DataInputStream in) throws IOException {
        in.close();
        this.socket.close();
    }

    public void convertHashMapToByteArray() {
        ByteBuffer buff = ByteBuffer.allocate(this.data.length);
        Iterator iterator = this.fileData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry mapentry = (Map.Entry) iterator.next();
            System.out.println("clé: " + mapentry.getKey());
            buff.put((byte[]) mapentry.getValue());
        }
        this.data = buff.array();
    }

    public byte[] getData() {
        return this.data;
    }

}