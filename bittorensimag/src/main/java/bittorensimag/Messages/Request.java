package bittorensimag.Messages;

import java.io.IOException;
import java.io.OutputStream;

import bittorensimag.MessageCoder.*;
import bittorensimag.Torrent.Torrent;

public class Request extends Msg {
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

	public static void sendMessage(int index, int beginOffset, OutputStream out) throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Request msgRequest = new Request(index, beginOffset);
		coderToWire.frameMsg(coderToWire.toWire(msgRequest), out);
		// TODO Add info
		System.out.println("Message Request sent index=" + index + " beginOffset=" + beginOffset);
	}

	public static void sendMessage(int index, int beginOffset, int pieceLength, OutputStream out) throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Request msgRequest = new Request(index, beginOffset, pieceLength);
		coderToWire.frameMsg(coderToWire.toWire(msgRequest), out);
		// TODO Add info
		System.out.println("Message Request sent (Last Part) index=" + index + " beginOffset=" + beginOffset);
	}

	public static void sendMessageForIndex(int index, int numberOfParts, OutputStream out) throws IOException {
		for (int j = 0; j < numberOfParts - 1; j++) {
			Request.sendMessage(index, j * Piece.DATA_LENGTH, out);
		}
		if (index == Torrent.numberOfPieces - 1) {
			Request.sendMessage(index, (numberOfParts - 1) * Piece.DATA_LENGTH, Torrent.lastPartLength, out);
		} else {
			Request.sendMessage(index, (numberOfParts - 1) * Piece.DATA_LENGTH, out);
		}
	}
}
