package bittorensimag;

import java.io.OutputStream;
import java.net.Socket;

public class ClientTCP {
	public static void main(String args[]) throws Exception{
		String destAddr = "127.0.0.1"; // Destination address
		int destPort = 6881; // Destination port
		Socket sock = new Socket(destAddr, destPort);
		OutputStream out = sock.getOutputStream();
		
		MsgCoder handshakMsgCoder = new HandshakMsgCoder();
		//TODO sha1 string 
		HandshakeMsg handshakeMsg = new HandshakeMsg("string");
		for(byte i : handshakeMsg.getPeerId()) {
			System.out.println(i);
		}
		
		byte[] data = handshakMsgCoder.toWire(handshakeMsg);
		out.write(data);
		out.flush();
		//sock.close();
	}
}
