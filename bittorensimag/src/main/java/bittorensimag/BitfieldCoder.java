package bittorensimag;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BitfieldCoder {
	public byte[] toWire(MsgBitfield msg) throws IOException {		//converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);
		
		out.write(msg.getMsgLength());
		out.writeByte(msg.getMsgType());
		out.write(msg.getBitfieldDATA());
		
		out.flush();
		byte[] data = byteStream.toByteArray();
	    return data;
	}
	
	public MsgBitfield fromWire(byte[] input) throws IOException{	//parses a given sequence of bytes
		ByteArrayInputStream bs = new ByteArrayInputStream(input);
	    DataInputStream in = new DataInputStream(bs);
	    
	    int length = in.readInt();
	    int type = in.readByte();
	    byte[] data = new byte[length - 1];
	    in.readFully(data);
	    
		return new MsgBitfield(length, type, data);
		
	}
}
