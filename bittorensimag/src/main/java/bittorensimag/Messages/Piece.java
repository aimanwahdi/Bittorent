package bittorensimag.Messages;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;
import bittorensimag.Torrent.Torrent;
import bittorensimag.Util.Hashage;
import bittorensimag.Util.Util;

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

	// TODO Replace fileContent with buffer
	public static boolean testPieceHash(int index, byte[] pieceData) {

		// hash the piece
		byte[] hashOfPieceFile = Hashage.sha1Hasher.hashToByteArray(pieceData);

		// retrieve hash from torrent metadata
		byte[] hashOfPieceTorrent = Torrent.piecesHashes.get(index);

		if (Arrays.equals(hashOfPieceFile, hashOfPieceTorrent)) {
			return true;
		} else {
			LOG.error("File is not identical to it's torrent");
			return false;
		}
	}

	public static void addToMap(int pieceIndex, byte[] data) {
		LOG.debug("Adding piece " + pieceIndex + " to the map");
		// TODO add beginOffset in case parts do not arrive in order
		if (Torrent.dataMap.containsKey(pieceIndex)) {
			LOG.debug("Piece already in map, concatenate to piece");
			Torrent.dataMap.replace(pieceIndex, Util.concat(Torrent.dataMap.get(pieceIndex), data));
		} else {
			Torrent.dataMap.put(pieceIndex, data);
		}
	}
}
