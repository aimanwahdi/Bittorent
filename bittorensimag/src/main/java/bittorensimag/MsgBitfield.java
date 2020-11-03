package bittorensimag;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MsgBitfield extends Msg implements MsgCoder {
	private byte[] bitfieldDATA = new byte[2];

	public MsgBitfield(int msgLength, int msgType, byte[] bitfieldDATA) {
		super(msgLength, msgType);
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
	public MsgBitfield fromWire(byte[] input) throws IOException { // parses a given sequence of bytes
		ByteArrayInputStream bs = new ByteArrayInputStream(input);
		DataInputStream in = new DataInputStream(bs);

		int length = in.readInt();
		int type = in.readByte();
		byte[] data = new byte[length - 1];
		in.readFully(data);

		return new MsgBitfield(length, type, data);
	}
}
