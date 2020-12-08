package bittorensimag.Messages;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;
import bittorensimag.Util.Util;

public class Bitfield extends Msg {
	private static final Logger LOG = Logger.getLogger(Bitfield.class);

	private byte[] bitfieldData = new byte[2];

	public final static int HEADER_LENGTH = 1;
	public final static int BITFIELD_TYPE = 5;

	// Constructor for standard 2 byte bitfield
	public Bitfield(byte[] bitfieldDATA) {
		super(HEADER_LENGTH + bitfieldDATA.length, BITFIELD_TYPE);
		this.bitfieldData = bitfieldDATA;
	}

	public byte[] getBitfieldDATA() {
		return bitfieldData;
	}

	public void setBitfieldDATA(byte[] bitfieldDATA) {
		this.bitfieldData = bitfieldDATA;
	}

	public static void sendMessage(byte[] dataBitfield, SelectionKey key) throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Bitfield msgBitfield = new Bitfield(dataBitfield);
    	SocketChannel clntChan = (SocketChannel) key.channel();

		try {
			ByteBuffer writeBuf = ByteBuffer.wrap(coderToWire.toWire(msgBitfield));

			if (writeBuf.hasRemaining()) {
				clntChan.write(writeBuf);
			}
			
			System.out.println("Message Bitfield sent");
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.debug("Message Bitfield sent with data : " + Util.bytesToHex(dataBitfield));
	}
	
	public static ArrayList<Integer> convertBitfieldToList (Bitfield msg, int numberOfPiece){
		ArrayList<Integer> listePieceDispo = new ArrayList<Integer>();
		byte[] bitfieldReceivedData = msg.getBitfieldDATA();
    	int index = 0;
    	outerloop:
    	for(int i = 0; i < bitfieldReceivedData.length; i++) {
    		for(int j=0; j<8; j++) {
    			int valueOfBit = (bitfieldReceivedData[i] >> (7 - j)) & 1; //retrieve the value of bit from highest bit to lowest bit
    			if (valueOfBit == 1){
    				listePieceDispo.add(index);
    			}
    			index++;
    			if(index == numberOfPiece) {
    				break outerloop;
    			}
    		}
    	}
    	return listePieceDispo;
	}

}
