package bittorensimag;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

public class HandshakeMsg {
	private final int protocolNameLength = 19;
	private final String protocolName = "BitTorrent protocol";
	private final long reservedExtensionByte = 0x0;
	private String sha1Hash;
	private byte[] peerId;

	public HandshakeMsg(String sha1Hash) {
		this.sha1Hash = sha1Hash;
		this.peerId = new byte[20];
		new Random().nextBytes(peerId);
	}

	public long getReservedExtensionByte() {
		return reservedExtensionByte;
	}

	public String getSha1Hash() {
		return sha1Hash;
	}

	public void setSha1Hash(String sha1Hash) {
		this.sha1Hash = sha1Hash;
	}

	public byte[] getPeerId() {
		return peerId;
	}

	public void setPeerId(byte[] peerId) {
		this.peerId = peerId;
	}

	public int getProtocolNameLength() {
		return protocolNameLength;
	}

	public String getProtocolName() {
		return protocolName;
	}

	@Override
	public String toString() {
		return "HandshakeMsg [protocolNameLength=" + protocolNameLength + ", protocolName=" + protocolName
				+ ", reservedExtensionByte=" + reservedExtensionByte + ", sha1Hash=" + sha1Hash + ", peerId=" + peerId
				+ "]";
	}

	public void accept(MsgCoderDispatcher dispatcher) throws IOException {
		dispatcher.toWire(this);
	}

	public HandshakeMsg fromWire(byte[] input) throws IOException {
		ByteArrayInputStream bs = new ByteArrayInputStream(input);
		DataInputStream in = new DataInputStream(bs);
		int protocolNameLength = in.readByte();
		// String protocolName = new String(in.re);

		return new HandshakeMsg("test");
	}
}
