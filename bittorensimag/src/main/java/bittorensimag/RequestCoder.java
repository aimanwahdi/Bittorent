package bittorensimag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RequestCoder { //coder for Request and Cancel
	public byte[] toWire(MsgRequest msg) throws IOException {		//converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);
		
		out.writeInt(msg.getMsgLength());
		out.writeByte(msg.getMsgType());
		out.writeInt(msg.getIndex());
		out.writeInt(msg.getBeginOffset());
		out.writeInt(msg.getPieceLength());
		
		out.flush();
		byte[] data = byteStream.toByteArray();
	    return data;
	}
	
	public MsgRequest fromWire(byte[] input) throws IOException{	//parses a given sequence of bytes
		ByteArrayInputStream bs = new ByteArrayInputStream(input);
	    DataInputStream in = new DataInputStream(bs);
	    
	    int length = in.readInt();
	    int type = in.readByte();
	    int index = in.readInt();
	    int beginOffset = in.readInt();
	    int pieceLength = in.readInt();
	    
		return new MsgRequest(length, type, index, beginOffset, pieceLength);
		
	}
}
