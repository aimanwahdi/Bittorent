package bittorensimag;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MsgBinCoder{ //coder pour choke, unchoke, uninterested, interested
	
	public byte[] toWire(Msg msg) throws IOException {		//converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);
		
		if(msg.getMsgType()>=0 && msg.getMsgType()<4) {
			out.writeLong(msg.getMsgLength());
			out.writeByte(msg.getMsgType());
		} 
		
		out.flush();
		byte[] data = byteStream.toByteArray();
	    return data;
	}
	
	public Msg fromWire(byte[] input) throws IOException{	//parses a given sequence of bytes
		return null;
		// A faire
	}
}
