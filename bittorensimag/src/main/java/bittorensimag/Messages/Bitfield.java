package bittorensimag.Messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import bittorensimag.MessageCoder.*;

public class Bitfield extends Msg implements MsgCoder {
	private byte[] bitfieldDATA = new byte[2];

	public final static int HEADER_LENGTH = 1;
	public final static int DATA_LENGTH = 2;
	public final static int BITFIELD_LENGTH = HEADER_LENGTH + DATA_LENGTH;
	public final static int BITFIELD_TYPE = 5;

	// Constructor for standard 2 byte bitfield
	public Bitfield(byte[] bitfieldDATA) {
		super(BITFIELD_LENGTH, BITFIELD_TYPE);
		this.bitfieldDATA = bitfieldDATA;
	}

	public Bitfield(int msgLength, byte[] bitfieldDATA) {
		super(msgLength, BITFIELD_TYPE);
		this.bitfieldDATA = bitfieldDATA;
	}

	public byte[] getBitfieldDATA() {
		return bitfieldDATA;
	}

	public void setBitfieldDATA(byte[] bitfieldDATA) {
		this.bitfieldDATA = bitfieldDATA;
	}

	@Override
	public void accept(MsgCoderDispatcher dispatcher) throws IOException {
		dispatcher.toWire(this);
	}

	@Override
	public Bitfield fromWire(byte[] input) throws IOException { // parses a given sequence of bytes
		ByteArrayInputStream bs = new ByteArrayInputStream(input);
		DataInputStream in = new DataInputStream(bs);

		int length = in.readInt();
		int type = in.readByte();
		byte[] data = new byte[length - HEADER_LENGTH];
		in.readFully(data);

		return new Bitfield(data);
	}
}
