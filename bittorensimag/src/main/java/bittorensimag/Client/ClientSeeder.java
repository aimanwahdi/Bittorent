package bittorensimag.Client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Messages.Bitfield;
import bittorensimag.Messages.Handshake;
import bittorensimag.Messages.Simple;
import bittorensimag.Torrent.Torrent;
import bittorensimag.Torrent.Tracker;

public class ClientSeeder {
    private final MsgCoderToWire coder;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private OutputStream out;
    private InputStream in;
    private Socket socket;
    
    private final int HANDSHAKE_LENGTH = 19;
    //temporary sha1
    String sha1 = "" ;
    
    public ClientSeeder( MsgCoderToWire coder) {
        this.coder = coder;
        this.createOutputStream();
        this.createInputStream();
    }
    
    public void startCommunication() {
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
    
    private void sendHandshake(String sha1) throws IOException {
        Handshake handShake = new Handshake(sha1);
        this.frameMsg(coder.toWire(handShake), this.out);
    }
    
    private void sendBitfield(int msgLength, int msgType, byte[] dataBitfield) throws IOException {
        Bitfield msgBitfield = new Bitfield(msgLength, msgType, dataBitfield);
        this.frameMsg(coder.toWire(msgBitfield), this.out);
    }
    
    private void sendUnchoke(int msgLength, int msgType) throws IOException {
    	Simple unchoke = new Simple(msgLength,msgType);
        this.frameMsg(coder.toWire(unchoke), this.out);
    }
    
    private boolean receivedMsg(DataInputStream in, OutputStream Out, MsgCoderToWire coder) throws IOException {
    	if ((int) in.readByte() == HANDSHAKE_LENGTH) {
            System.out.println("reading");
            
            // reading the rest of handshake msg
            this.readMessage(in, 67);
            
            //TODO verify that we have the file requested from the leecher, otherwise exit with exception
            
            //send handshake and bietfield message 
            this.sendHandshake(sha1);
            this.sendBitfield(Bitfield.BITFIELD_LENGTH, Bitfield.BITFIELD_TYPE, new byte[] { 0, 0 });

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
            	case 5 :
                    System.out.println("received bitfield message");
                    this.readMessage(in,2);
                    
                    System.out.println("received interested message");
                    this.readMessage(in,5);
                    
                    this.sendUnchoke(sha1);

            }
    	}

    	return false;
    }

    
}
