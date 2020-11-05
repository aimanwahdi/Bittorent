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
				byte[] req;
				req = nextMsg(in);
				if (req  != null && req[0]==66 ) {	
					System.out.println("Received message (" + req.length + " bytes)");
					
					byte[] dataBitfield = {0,0};
					
					Bitfield msgBitfield = new Bitfield(3,5,dataBitfield);
					Msg msgInterested = new Msg(1,2);
					
					frameMsg(coder.toWire(msgBitfield),Out);
					frameMsg(coder.toWire(msgInterested),Out);
						
				}
				
				System.out.println(req[0]);
				if (req  != null && req[0]==-44 ) {
					System.out.println("request message");
					Request msgRequest1 = new Request(13,6,6,0,16384);
					Request msgRequest2 = new Request(13,6,6,16384,16384);
					frameMsg(coder.toWire(msgRequest1),Out);
					frameMsg(coder.toWire(msgRequest2),Out);

				}
				if (req  != null && Math.abs(req[0])==1 ) {
					Have msgHave = new Have(5,4,6);
					frameMsg(coder.toWire(msgHave),Out);
				}
				if (req  != null && Math.abs(req[0])==86 ) {
					Request msgRequest1 = new Request(13,6,4,0,16384);
					Request msgRequest2 = new Request(13,6,4,16384,16384);
					frameMsg(coder.toWire(msgRequest1),Out);
					frameMsg(coder.toWire(msgRequest2),Out);
				}
				
				if (req  != null && Math.abs(req[0])!=1  && req[0]!=-44 && req[0]!=66 && Math.abs(req[0])!=86) {
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

}
