package bittorensimag.Messages;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;
import bittorensimag.Torrent.Torrent;
import bittorensimag.Util.Util;

public class Bitfield extends Msg {
	private static final Logger LOG = Logger.getLogger(Bitfield.class);

	private static int bitfieldLength = (int) Math.ceil((double) Torrent.numberOfPieces / 8);

	public static byte[] ourBitfieldData = new byte[bitfieldLength];

	private byte[] bitfieldData;

	public final static int HEADER_LENGTH = 1;
	public final static int BITFIELD_TYPE = 5;

	// Constructor for standard byte bitfield
	public Bitfield(byte[] bitfieldData) {
		super(HEADER_LENGTH + bitfieldLength, BITFIELD_TYPE);
		this.bitfieldData = bitfieldData;
	}

	public byte[] getBitfieldDATA() {
		return bitfieldData;
	}

	public void setBitfieldDATA(byte[] bitfieldDATA) {
		this.bitfieldData = bitfieldDATA;
	}

	public static void setByteInBitfield(int index, byte status) {
		Bitfield.ourBitfieldData[index] = status;
	}

	public static void sendMessage(OutputStream out) throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Bitfield msgBitfield = new Bitfield(ourBitfieldData);
		coderToWire.frameMsg(coderToWire.toWire(msgBitfield), out);
		LOG.debug("Message Bitfield sent with data : " + Util.bytesToHex(ourBitfieldData));
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
