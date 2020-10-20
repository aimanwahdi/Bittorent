package bittorensimag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class HandshakMsgCoder implements MsgCoder {
	
	public byte[] toWire(HandshakeMsg msg) throws IOException{
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);
		
		out.writeByte(msg.getProtocolNameLength());
		out.write(msg.getProtocolName().getBytes(Charset.forName("UTF-8")));
		out.writeLong(msg.getReservedExtensionByte()); 
		
		//TODO sha1
		
		for(int i=0; i<msg.getPeerId().length;i++ ) {
			out.writeByte(msg.getPeerId()[i]);
		}
		
		for(int i=0; i<msg.getPeerId().length;i++ ) {
			out.writeByte(msg.getPeerId()[i]);
		}

		out.flush();
		byte[] data = byteStream.toByteArray();
	    return data;
	}
	public HandshakeMsg fromWire(byte[] input) throws IOException{
		ByteArrayInputStream bs = new ByteArrayInputStream(input);
		DataInputStream in = new DataInputStream(bs);
		int protocolNameLength = in.readByte();
		//String protocolName = new String(in.re);
		
		return new HandshakeMsg("test");
	}

}
