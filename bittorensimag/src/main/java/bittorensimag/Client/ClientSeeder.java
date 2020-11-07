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
import bittorensimag.Messages.Msg;
import bittorensimag.Messages.Request;
import bittorensimag.Messages.Simple;
import bittorensimag.Torrent.Torrent;
import bittorensimag.Torrent.Tracker;

public class ClientSeeder {
	private final Torrent torrent;
    private final MsgCoderToWire coder;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private OutputStream out;
    private InputStream in;
    private Socket socket;
    
    private final int HANDSHAKE_LENGTH = 19;
    
    public ClientSeeder(Torrent torrent, MsgCoderToWire coder) {
        this.torrent = torrent;
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
    
    private boolean verifyHandshake(DataInputStream in) {
    	String sha1 = "";
    	//read protocol name 
    	readMessage(in,19);
    	
    	//read extension bytes 
    	readMessage(in,8);
    	
    	//read sha1 hash
    	 for (int i = 0; i < 20; i++) {
             try {
                 int nextByte = in.readByte();
                 sha1 += nextByte;

             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
    	 
    	 //read peerId
     	readMessage(in,20);

    	 if(sha1.equals(this.torrent.info_hash)) {
    		 return true;
    	 }
    	
    	return false;
    }
    
    private void sendHandshake(String sha1) throws IOException {
        Handshake handShake = new Handshake(sha1);
        this.frameMsg(coder.toWire(handShake), this.out);
    }
    
    private void sendBitfield(byte[] dataBitfield) throws IOException {
        Bitfield msgBitfield = new Bitfield(dataBitfield);
        this.frameMsg(coder.toWire(msgBitfield), this.out);
    }
    
    private void sendUnchoke() throws IOException {
    	Msg unchoke = new Msg(Simple.LENGTH, Simple.INTERESTED);
        this.frameMsg(coder.toWire(unchoke), this.out);
    }
    
    private boolean receivedMsg(DataInputStream in, OutputStream Out, MsgCoderToWire coder) throws IOException {
    	if ((int) in.readByte() == HANDSHAKE_LENGTH) {
            System.out.println("received handshake msg ");
            
            if (this.verifyHandshake(in)) {
                //send handshake and bietfield message 
                this.sendHandshake(this.torrent.info_hash);
                this.sendBitfield(new byte[] { 0, 0 });
            }
            else {
            	System.out.println("info_hash not found");
            }
            

    	} else {
            System.out.println("reading");

            int secondByte = in.readByte();
            int thirdByte = in.readByte();
            int fourthByte = in.readByte();
            int type = in.readByte();

            System.out.println("type : " + type);
            
            switch (type) {
            	case Bitfield.BITFIELD_TYPE :
                    System.out.println("received bitfield message");
                    this.readMessage(in,2);
                    
                    System.out.println("received interested message");
                    this.readMessage(in,5);
                    
                    this.sendUnchoke();
                    return true;
                    
            	case Request.REQUEST_TYPE :
                    System.out.println("received request message");
                    break;
                    

            }
    	}

    	return false;
    }

    
}
