package bittorensimag;

import java.util.Random;

public class HandshakeMsg {
	private final int protocolNameLength = 19;
	private final String protocolName = "BitTorrent protocol";
	private final int reservedExtensionByte=0x0;
	private String sha1Hash ;
	private byte[] peerId; 
	
	public HandshakeMsg(String sha1Hash) {
		this.sha1Hash = sha1Hash;
		this.peerId= new byte[20];
		new Random().nextBytes(peerId);
	}

	public int getReservedExtensionByte() {
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
	
}
