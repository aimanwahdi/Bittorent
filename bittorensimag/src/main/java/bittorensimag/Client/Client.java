package bittorensimag.Client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Messages.*;
import bittorensimag.Torrent.*;
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
    
    private int numberOfPartPerPiece;
    private int numberOfPieces;
    private int lastPieceLength;

    private final String LOCALHOST = "127.0.0.1";
    

    public Client(Torrent torrent, Tracker tracker, MsgCoderToWire coder) {
        this.torrent = torrent;
        this.tracker = tracker;
        this.coder = coder;
        this.createSocket(LOCALHOST, tracker.port);
        this.createOutputStream();
        this.createInputStream();
        this.calculateNumberParts();
        this.calculateNumberPieces();
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
        this.numberOfPieces = length / (this.numberOfPartPerPiece * Piece.DATA_LENGTH);
        this.lastPieceLength = length % Piece.DATA_LENGTH;
    }

    public void startCommunication() {
        this.sendHandshake();
        try {
        while (this.receivedMsg(this.dataIn, this.out, this.coder)) {
        ;
        }
        } catch (IOException ioe) {
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

    private void sendBitfield(byte[] dataBitfield) throws IOException {
        Bitfield msgBitfield = new Bitfield(dataBitfield);
        this.frameMsg(coder.toWire(msgBitfield), this.out);
    }

    private void sendInterested() throws IOException {
        this.frameMsg(coder.toWire(new Msg(Simple.LENGTH, Simple.INTERESTED)), this.out);
    }

    private void sendHave(int index) throws IOException {
        Have msgHave = new Have(index);
        this.frameMsg(coder.toWire(msgHave), this.out);
    }

    private void sendRequest(int index, int beginOffset)
            throws IOException {
        Request msgRequest = new Request(index, beginOffset);
        this.frameMsg(coder.toWire(msgRequest), this.out);
    }

    private boolean receivedMsg(DataInputStream in, OutputStream Out, MsgCoderToWire coder) throws IOException {

        int firstByte = in.readByte();
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
            
            int totalLength = Integer
                    .parseInt(Util.intToHexStringWith0(firstByte) + Util.intToHexStringWith0(secondByte)
                            + Util.intToHexStringWith0(thirdByte) + Util.intToHexStringWith0(fourthByte), 16);

            int type = in.readByte();
            System.out.println("Received Message : " + Msg.messagesNames.get(type));

            switch (type) {
                case Simple.UNCHOKE:
                    this.sendAllRequests();
                    break;
                case Piece.PIECE_TYPE:
                     // read piece index and begin offset of the first piece
                    int pieceIndex1 = in.readInt();
                    int beginOffset1 = in.readInt();

                    // read all the data sent in the first piece
                    this.readMessage(in, totalLength - Piece.HEADER_LENGTH);

                    // read second piece
                    int length2 = in.readInt();
                    int type2 = in.readByte();
                    int pieceIndex2 = in.readInt();
                    int beginOffset2 = in.readInt();

                    this.readMessage(in, length2 - Piece.HEADER_LENGTH);

                    // TODO need to calculate index
                    this.sendHave(pieceIndex1);
                    break;
                // if the type is not correct leave
                default:
                    return false;
            }
        }
        return true;
    }

    private void sendAllRequests() throws IOException {
        for (int i = 0; i < this.numberOfPieces - 1; i++) {
            for (int j = 0; j < this.numberOfPartPerPiece; j++) {
                sendRequest(i, j * Piece.DATA_LENGTH);
            }
        }
        // TODO last piece
    }
}