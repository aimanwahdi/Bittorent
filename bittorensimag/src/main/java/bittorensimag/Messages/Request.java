package bittorensimag.Messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import bittorensimag.MessageCoder.*;

public class Request extends Msg implements MsgCoder {
	private int index;
	private int beginOffset;
	private int pieceLength;

	public final static int REQUEST_LENGTH = 13;
	public final static int REQUEST_TYPE = 6;

	// Constructor with piece length default
	public Request(int index, int beginOffset) {
		super(REQUEST_LENGTH, REQUEST_TYPE);
		this.index = index;
		this.beginOffset = beginOffset;
		this.pieceLength = Piece.DATA_LENGTH;
	}

	public Request(int index, int beginOffset, int pieceLength) {
		super(REQUEST_LENGTH, REQUEST_TYPE);
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

		return new Request(index, beginOffset);
	}

}
