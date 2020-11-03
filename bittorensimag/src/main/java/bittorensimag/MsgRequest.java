package bittorensimag;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MsgRequest extends Msg implements MsgCoder {
	private int index;
	private int beginOffset;
	private int pieceLength;

	public MsgRequest(int msgLength, int msgType, int index, int beginOffset, int pieceLength) {
		super(msgLength, msgType);
		this.index = index;
		this.beginOffset = beginOffset;
		this.pieceLength = pieceLength;
	}

	public int getIndex() {
		return index;
	}

	public int getBeginOffset() {
		return beginOffset;
	}

	public void setBeginOffset(int beginOffset) {
		this.beginOffset = beginOffset;
	}

	public int getPieceLength() {
		return pieceLength;
	}

	public void setPieceLength(int pieceLength) {
		this.pieceLength = pieceLength;
	}

	@Override
	public void accept(MsgCoderDispatcher dispatcher) throws IOException {
		dispatcher.toWire(this);
	}

	@Override
	public Msg fromWire(byte[] input) throws IOException {
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
