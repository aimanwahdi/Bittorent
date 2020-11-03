package bittorensimag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MsgBinCoder{ //coder pour choke, unchoke, uninterested, interested
	
	public byte[] toWire(Msg msg) throws IOException {		//converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);
		
		if(msg.getMsgType()>=0 && msg.getMsgType()<4) {
			out.writeInt(msg.getMsgLength());
			out.writeByte(msg.getMsgType());
		} 
		
		out.flush();
		byte[] data = byteStream.toByteArray();
	    return data;
	}
	
	public Msg fromWire(byte[] input) throws IOException{	//parses a given sequence of bytes
		ByteArrayInputStream bs = new ByteArrayInputStream(input);
	    DataInputStream in = new DataInputStream(bs);
	    
	    int length = in.readInt();
	    int type = in.readByte();
	    
	    return new Msg(length, type);
	}
}
