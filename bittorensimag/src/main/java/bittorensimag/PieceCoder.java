package bittorensimag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PieceCoder {
	public byte[] toWire(MsgPiece msg) throws IOException {		//converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);
		
		out.writeInt(msg.getMsgLength());
		out.writeByte(msg.getMsgType());
		out.writeInt(msg.getPieceIndex());
		out.writeInt(msg.getBeginOffset());
		
		//should be corrected later because right now it doesn't write all the data 
		out.write(msg.getData());
		
		out.flush();
		byte[] data = byteStream.toByteArray();
	    return data;
	}
	
	public MsgPiece fromWire(byte[] input) throws IOException{	//parses a given sequence of bytes
		ByteArrayInputStream bs = new ByteArrayInputStream(input);
	    DataInputStream in = new DataInputStream(bs);
	    
	    int length = in.readInt();
	    int type = in.readByte();
	    int index = in.readInt();
	    int beginOffset = in.readInt();
	    byte[] data = new byte[length - 9];
	    in.readFully(data);
	    
		return new MsgPiece(length, type, index, beginOffset, data);
	}
}
