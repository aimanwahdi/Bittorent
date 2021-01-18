package bittorensimag.Messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;
import bittorensimag.Torrent.Torrent;

public class Cancel extends Msg {
	private static final Logger LOG = Logger.getLogger(Cancel.class);

	private int index;
	private int beginOffset;
	private int pieceLength;

	public final static int CANCEL_LENGTH = 13;
	public final static int CANCEL_TYPE = 8;

	public Cancel(int index, int beginOffset, int pieceLength) {
		super(CANCEL_LENGTH, CANCEL_TYPE);
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

	// TODO change return type to boolean to know if it worked
	// Private method to send one Cancel message
	private static void sendMessage(int index, int beginOffset, int pieceLength, SocketChannel clntChan)
			throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Cancel msgCancel = new Cancel(index, beginOffset, pieceLength);
		try {
			ByteBuffer writeBuf = ByteBuffer.wrap(coderToWire.toWire(msgCancel));

			if (writeBuf.hasRemaining()) {
				clntChan.write(writeBuf);
			}

			LOG.debug("Message Cancel sent for index=" + index + " beginOffset=" + beginOffset);
		} catch (IOException e) {
			LOG.error("Error sending Cancel message " + e.getMessage());
		}
	}

	public static void sendMessageForIndex(int index, SocketChannel clntChan) throws IOException {
		// Change according to index
		int numberOfParts = Torrent.numberOfPartPerPiece;
		if (index == Torrent.numberOfPieces - 1) {
			numberOfParts = Torrent.lastPieceNumberOfParts;
		}

		for (int j = 0; j < numberOfParts - 1; j++) {
			Cancel.sendMessage(index, j * Piece.DATA_LENGTH, Piece.DATA_LENGTH, clntChan);
		}
		if (index == Torrent.numberOfPieces - 1) {
			Cancel.sendMessage(index, (numberOfParts - 1) * Piece.DATA_LENGTH, Torrent.lastPartLength, clntChan);
		} else {
			Cancel.sendMessage(index, (numberOfParts - 1) * Piece.DATA_LENGTH, Piece.DATA_LENGTH, clntChan);
		}
	}

}
