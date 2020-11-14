package bittorensimag.Messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;

public class Bitfield extends Msg {
	private static final Logger LOG = Logger.getLogger(Bitfield.class);

	private byte[] bitfieldData = new byte[2];

	public final static int HEADER_LENGTH = 1;
	public final static int DATA_LENGTH = 2;
	public final static int BITFIELD_LENGTH = HEADER_LENGTH + DATA_LENGTH;
	public final static int BITFIELD_TYPE = 5;

	// Constructor for standard 2 byte bitfield
	public Bitfield(byte[] bitfieldDATA) {
		super(BITFIELD_LENGTH, BITFIELD_TYPE);
		this.bitfieldData = bitfieldDATA;
	}

	public Bitfield(int msgLength, byte[] bitfieldDATA) {
		super(msgLength, BITFIELD_TYPE);
		this.bitfieldData = bitfieldDATA;
	}

	public byte[] getBitfieldDATA() {
		return bitfieldData;
	}

	public void setBitfieldDATA(byte[] bitfieldDATA) {
		this.bitfieldData = bitfieldDATA;
	}

	public static void sendMessage(byte[] dataBitfield, OutputStream out) throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Bitfield msgBitfield = new Bitfield(dataBitfield);
		coderToWire.frameMsg(coderToWire.toWire(msgBitfield), out);
		LOG.debug("Message Bitfield sent with data : " + dataBitfield);
}

}
