package bittorensimag;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Random;

public class Test {
	public static void main(String args[])throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);
		//byte[] msg = new Hashage("SHA-1").hashToByte(new RandomString().getRandomString());
		//out.write(msg.getBytes(Charset.forName("UTF-8")));
		
		byte[] b = new byte[20];
		new Random().nextBytes(b);
		out.writeByte(19);
		out.write("BitTorrent protocol".getBytes(Charset.forName("UTF-8")));
		out.writeLong(0x0); //int sur 32 bits, 8 byte 
		
		Util util = new Util();
		
		
		byte[] sha1 = util.hexStringToByteArray("067133ace5dd0c5027b99de5d4ba512828208d5b");
		
		for(int i=0; i<sha1.length;i++ ) {
			out.writeByte(sha1[i]);
		}
		
		for(int i=0; i<b.length;i++ ) {
			out.writeByte(b[i]);
		}


		out.flush();
		byte[] data = byteStream.toByteArray();
		
		String destAddr = "127.0.0.1"; // Destination address
		int destPort = 6881; // Destination port
		Socket sock = new Socket(destAddr, destPort);
		OutputStream Out = sock.getOutputStream();
		Out.write(data);
		Out.flush();
		
		//int localPort = sock.getLocalPort();
		ServerSocket servSock = new ServerSocket(destPort);
		while(true) {
			Socket clntSock = servSock.accept();
		}
		//sock.close();
	}
}
