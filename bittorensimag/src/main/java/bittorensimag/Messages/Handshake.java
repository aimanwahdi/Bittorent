package bittorensimag.Messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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

	public static boolean sendMessage(String info_hash, SocketChannel clntChan) {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Handshake handshakeMsg = new Handshake(info_hash);
		LOG.debug("Sending Handshake message for info_hash : " + info_hash);
		try {
			ByteBuffer writeBuf = ByteBuffer.wrap(coderToWire.toWire(handshakeMsg));

			if (writeBuf.hasRemaining()) {
				clntChan.write(writeBuf);
			}
			LOG.debug("Message Handshake sent to " + clntChan);
			return true;
		} catch (IOException e) {
			LOG.error("Error sending Handshake message " + e.getMessage());
			return false;
		}
	}
}
