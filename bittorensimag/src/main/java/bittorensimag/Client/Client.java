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

public class Client {
    private final Torrent torrent;
    private final Tracker tracker;
    private final MsgCoderToWire coder;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private OutputStream out;
    private InputStream in;
    private Socket socket;

    private final int HANDSHAKE_LENGTH = 19;
    private final String LOCALHOST = "127.0.0.1";

    public Client(Torrent torrent, Tracker tracker, MsgCoderToWire coder) {
        this.torrent = torrent;
        this.tracker = tracker;
        this.coder = coder;
        this.createSocket(LOCALHOST, tracker.port);
        this.createOutputStream();
        this.createInputStream();
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

    public void createSocket(String destAddr, int destPort) {
        try {
            this.socket = new Socket(destAddr, destPort);
            this.out = this.socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHandshake() {
        Handshake handshakeMsg = new Handshake(this.tracker.info_hash);
        try {
            this.frameMsg(this.coder.toWire(handshakeMsg), this.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void name() {

    }

    // reading a message
    public static byte[] nextMsg(DataInputStream in) throws IOException {
        ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();

        int nextByte = in.read();
        int sum = 0;
        // correct condition
        while (nextByte != -1 && sum < 38) {
            nextByte = in.read();
            sum++;
            messageBuffer.write(nextByte); // write byte to buffer
            System.out.println("reading");
        }
        return messageBuffer.toByteArray();
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

    private void sendBitfield(int msgLength, int msgType, byte[] dataBitfield) throws IOException {
        Bitfield msgBitfield = new Bitfield(msgLength, msgType, dataBitfield);
        this.frameMsg(coder.toWire(msgBitfield), this.out);
    }

    private void sendInterested(int msgLength, int msgType) throws IOException {
        this.frameMsg(coder.toWire(new Msg(msgLength, msgType)), this.out);
    }

    private void sendHave(int msgLength, int msgType, int index) throws IOException {
        Have msgHave = new Have(5, 4, 6);
        this.frameMsg(coder.toWire(msgHave), this.out);
    }

    private void sendRequest(int msgLength, int msgType, int index, int beginOffset, int pieceLength)
            throws IOException {
        Request msgRequest = new Request(msgLength, msgType, index, beginOffset, pieceLength);
        this.frameMsg(coder.toWire(msgRequest), this.out);
    }

    private boolean receivedMsg(DataInputStream in, OutputStream Out, MsgCoderToWire coder) throws IOException {

        if ((int) in.readByte() == HANDSHAKE_LENGTH) {
            System.out.println("reading");

            // reading the rest of handshake msg
            this.readMessage(in, 67);

            // reading recieved bietfield msg
            this.readMessage(in, 7);

            this.sendBitfield(Bitfield.BITFIELD_LENGTH, Bitfield.BITFIELD_TYPE, new byte[] { 0, 0 });
            this.sendInterested(Simple.INTERESTED_LENGTH, Simple.INTERESTED_TYPE);

        } else {
            System.out.println("reading");

            int secondByte = in.readByte();
            int thirdByte = in.readByte();
            int fourthByte = in.readByte();

            int type = in.readByte();
            int totalLength = Integer.parseInt("" + HANDSHAKE_LENGTH + secondByte + thirdByte + fourthByte);

            System.out.println("secondByte : " + secondByte);
            System.out.println("thirdByte : " + thirdByte);
            System.out.println("fourthByte : " + fourthByte);

            System.out.println("type : " + type);

            switch (type) {
                case 1:
                    System.out.println("received unchoke message");
                    this.sendRequest(Request.REQUEST_LENGTH, Request.REQUEST_TYPE, 6, 0, 16384);
                    this.sendRequest(Request.REQUEST_LENGTH, Request.REQUEST_TYPE, 6, 16384, 16384);
                    break;
                case 7:
                    // Here we should do a loop to request and receive all the pieces

                    // read the two received pieces and send have message
                    System.out.println("received piece message");

                    // read piece index and begin offset of the first piece
                    int pieceIndex1 = in.readInt();
                    int beginOffset1 = in.readInt();

                    // read all the data sent in the first piece
                    this.readMessage(in, totalLength - 9);

                    // read second piece
                    int length2 = in.readInt();
                    int type2 = in.readByte();
                    int pieceIndex2 = in.readInt();
                    int beginOffset2 = in.readInt();

                    this.readMessage(in, length2 - 9);

                    // TODO need to calculate index
                    this.sendHave(Have.HAVE_LENGTH, Have.HAVE_TYPE, 6);
                    break;
                // if the type is not correct leave
                default:
                    return false;
            }

        }
        return true;

    }
}