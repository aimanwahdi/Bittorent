package bittorensimag;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author souha
 * Coder pour Bitfield message
 */

public class BitfieldMsgBinCoder {

	public byte[] toWire(MsgBitfield msg) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);
		
		if(msg.getMsgType()==5) {
			out.writeInt(msg.getMsgLength());
			out.writeByte(msg.getMsgType());
			
			for(byte i : msg.getBitfieldDATA()) {
				out.writeByte(i);
			}
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
