package bittorensimag.Messages;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;

public class Handshake {
	private static final Logger LOG = Logger.getLogger(Handshake.class);

	private final int protocolNameLength = 19;
	public static final String protocolName = "BitTorrent protocol";
	private long reservedExtensionByte = 0x0;
	private String sha1Hash;
	private byte[] peerId;

	public final static int HANDSHAKE_LENGTH = 19;

	public Handshake(String sha1Hash) {
		this.sha1Hash = sha1Hash;
		this.peerId = new byte[20];
		new Random().nextBytes(peerId);
	}

	public Handshake(String sha1Hash, byte[] peerId, long extensionBytes) {
		this.sha1Hash = sha1Hash;
		this.peerId = peerId;
		this.reservedExtensionByte = extensionBytes;
	}

	public long getReservedExtensionByte() {
		return this.reservedExtensionByte;
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

	public void accept(MsgCoderDispatcherToWire dispatcher) throws IOException {
	dispatcher.toWire(this);
	}

	public static void sendMessage(String info_hash, OutputStream out) throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Handshake handshakeMsg = new Handshake(info_hash);
		coderToWire.frameMsg(coderToWire.toWire(handshakeMsg), out);
		LOG.debug("Sending Handshake message for info_hash : " + info_hash);
	}
}
