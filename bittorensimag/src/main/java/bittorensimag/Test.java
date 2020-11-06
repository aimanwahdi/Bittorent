package bittorensimag;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Random;
import java.lang.Math.*;


import bittorensimag.MessageCoder.*;
import bittorensimag.Messages.*;

public class Test {
	public static void main(String args[]) throws Exception, IOException {

		MsgCoderToWire coder = new MsgCoderToWire();

		// create output stream
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);

		// create handshakeMsg
		Handshake handshakeMsg = new Handshake("067133ace5dd0c5027b99de5d4ba512828208d5b");

		// create socket
		String destAddr = "127.0.0.1"; // Destination address
		int destPort = 6881; // Destination port
		Socket sock = new Socket(destAddr, destPort);
		OutputStream Out = sock.getOutputStream();

		// send handshake msg
		frameMsg(coder.toWire(handshakeMsg), Out);

		// create input stream
		InputStream inputStream = sock.getInputStream();
		DataInputStream in = new DataInputStream(inputStream);
		
		while(true) {
			try {	
				if (receivedMsg(in,Out,coder) == 0) {
					break;
				}
			} 
			catch (IOException ioe) {
		        System.err.println("Error handling client: " + ioe.getMessage());
		    } 
		}
	}

	// reading a message
	public static byte[] nextMsg(DataInputStream in) throws IOException {
		ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();
        
		int nextByte= in.read();
		int sum =0;
		//correct condition 
		while (nextByte!= -1 && sum<38) { 
			nextByte = in.read();
			sum++;
			messageBuffer.write(nextByte); // write byte to buffer
			System.out.println("reading");
		}
		return messageBuffer.toByteArray();
	}

	// writing a message in OutputStream
	public static void frameMsg(byte[] message, OutputStream out) throws IOException {
		// write message
		System.out.println("writing");
		out.write(message);
		out.flush();

	}
	
	public static int receivedMsg(DataInputStream in, OutputStream Out, MsgCoderToWire coder) throws IOException {
        int lengthHandshake = in.readByte();
		System.out.println(lengthHandshake);
		
		if (lengthHandshake  == 19) { 
			System.out.println("reading");
			
			//reading the rest of handshake msg
			for(int i = 0; i< 67 ;i++) {
				int nextByte = in.readByte();
			}
			
			//reading recieved bietfield msg
			for(int i = 0; i<7; i++) {
				int nextByte = in.readByte();
			}
			
			//create our bietfield and interested msgs 
			byte[] dataBitfield = {0,0};
			
			Bitfield msgBitfield = new Bitfield(3,5,dataBitfield);
			Msg msgInterested = new Msg(1,2);
			
			//sending bietfield and interested msgs 
			frameMsg(coder.toWire(msgBitfield),Out);
			frameMsg(coder.toWire(msgInterested),Out);
			
		}
		else { 
			System.out.println("reading");
			
			int secondByte = in.readByte();
			int thirdByte = in.readByte();
			int fourthByte = in.readByte();
			
	        int type = in.readByte();
	        int Totallength = Integer.parseInt(""+lengthHandshake+secondByte+thirdByte+fourthByte);
	        
			System.out.println("secondByte : " + secondByte);
			System.out.println("thirdByte : " + thirdByte);
			System.out.println("fourthByte : " + fourthByte);

			System.out.println("type : " + type);

	        switch (type) {
	        	case 1 :
					System.out.println("received unchoke message");
					Request msgRequest1 = new Request(13,6,6,0,16384);
					Request msgRequest2 = new Request(13,6,6,16384,16384);
					frameMsg(coder.toWire(msgRequest1),Out);
					frameMsg(coder.toWire(msgRequest2),Out);
					break;
	        	case 7 :
	        		//Here we should do a loop to request and receive all the pieces 
	        		
	        		
	        		//read the two received pieces and send have message 
	        		System.out.println("received piece message");
					
					//read piece index and begin offset of the first piece 
					int pieceIndex1 = in.readInt();
					int beginOffset1 = in.readInt();
					
					//read all the data sent in the first piece 
					for(int i = 0; i<Totallength-9; i++) {
						int nextByte = in.readByte();
					}
					
					//read second piece 
					int length2 = in.readInt();
					int type2 = in.readByte();
					int pieceIndex2 = in.readInt();
					int beginOffset2 = in.readInt();
					
					for(int i = 0; i<length2-9; i++) {
						int nextByte = in.readByte();
					}
					
					//send Have message 
					Have msgHave = new Have(5,4,6);
					frameMsg(coder.toWire(msgHave),Out);
					break;
				//if the type is not correct leave 
				default :
					return 0;  
	        }
	        
		}
        return 1;

	}

}
