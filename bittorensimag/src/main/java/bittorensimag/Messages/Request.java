package bittorensimag.Messages;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;
import bittorensimag.Torrent.Torrent;

public class Request extends Msg {
	private static final Logger LOG = Logger.getLogger(Request.class);

	private int index;
	private int beginOffset;
	private int pieceLength;

	public final static int REQUEST_LENGTH = 13;
	public final static int REQUEST_TYPE = 6;

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

	// Private method to send one request message
	private static void sendMessage(int index, int beginOffset, int pieceLength, SocketChannel clntChan) throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Request msgRequest = new Request(index, beginOffset, pieceLength);
		try {
			ByteBuffer writeBuf = ByteBuffer.wrap(coderToWire.toWire(msgRequest));

			if (writeBuf.hasRemaining()) {
				clntChan.write(writeBuf);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.debug("Message Request sent for index=" + index + " beginOffset=" + beginOffset);
	}

	public static void sendMessageForIndex(int index, int numberOfParts, SocketChannel clntChan) throws IOException {
		for (int j = 0; j < numberOfParts - 1; j++) {
			Request.sendMessage(index, j * Piece.DATA_LENGTH, Piece.DATA_LENGTH, clntChan);
		}
		if (index == Torrent.numberOfPieces - 1) {
			Request.sendMessage(index, (numberOfParts - 1) * Piece.DATA_LENGTH, Torrent.lastPartLength, clntChan);
		} else {
			Request.sendMessage(index, (numberOfParts - 1) * Piece.DATA_LENGTH, Piece.DATA_LENGTH, clntChan);
		}
	}
	
}
