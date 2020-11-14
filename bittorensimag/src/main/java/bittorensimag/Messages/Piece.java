package bittorensimag.Messages;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;

public class Piece extends Msg {
	private static final Logger LOG = Logger.getLogger(Piece.class);

	private int pieceIndex;
	private int beginOffset;
	private byte[] data;

	// piece => 2^14 = 16384 (data) + 9 (message)
	public final static int HEADER_LENGTH = 9;
	public final static int DATA_LENGTH = 16384;
	public final static int PIECE_LENGTH = HEADER_LENGTH + DATA_LENGTH;
	public final static int PIECE_TYPE = 7;

	//  Constructor with default value for length
	public Piece(int pieceIndex, int beginOffset, byte[] data) {
		super(PIECE_LENGTH, PIECE_TYPE);
		this.pieceIndex = pieceIndex;
		this.beginOffset = beginOffset;
		this.data = data;
	}

	public Piece(int msgLength, int pieceIndex, int beginOffset, byte[] data) {
		super(msgLength, PIECE_TYPE);
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

	public static void sendMessage(int msgLength, int index, int beginOffset, byte[] data, OutputStream out)
			throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Piece piece = new Piece(msgLength, index, beginOffset, data);
		coderToWire.frameMsg(coderToWire.toWire(piece), out);
		LOG.debug("Message Piece sent index=" + index + " beginOffset=" + beginOffset);
	}
}
