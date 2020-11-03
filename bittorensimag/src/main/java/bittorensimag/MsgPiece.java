package bittorensimag;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MsgPiece extends Msg implements MsgCoder {
	private int pieceIndex;
	private int beginOffset;
	private byte[] data;

	public MsgPiece(int msgLength, int msgType, int pieceIndex, int beginOffset, byte[] data) {
		super(msgLength, msgType);
		this.pieceIndex = pieceIndex;
		this.beginOffset = beginOffset;
		this.data = data;
	}

	public int getPieceIndex() {
		return pieceIndex;
	}

	public void setPieceIndex(int pieceIndex) {
		this.pieceIndex = pieceIndex;
	}

	public int getBeginOffset() {
		return beginOffset;
	}

	public void setBeginOffset(int beginOffset) {
		this.beginOffset = beginOffset;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
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
		byte[] data = new byte[length - 9];
		in.readFully(data);

		return new MsgPiece(length, type, index, beginOffset, data);
	}

}
