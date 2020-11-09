package bittorensimag.Util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashage {
	private String algorithme; // l'algorithme utilisé pour le hashage peut être SHA-1, SHA-256...
	MessageDigest md = null;

	public Hashage(String algorithme) {
		super();
		this.algorithme = algorithme;
		try {
			this.md = MessageDigest.getInstance(algorithme);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("This algorithm is not correct");
			e.printStackTrace();
		}
	}

	/**
	 * return hash of the message in format Hex
	 * 
	 * @param message
	 * @return
	 * @throws Exception
	 */

	public String hashToHex(String message) throws Exception {
		MessageDigest md = MessageDigest.getInstance(algorithme);
		md.update(message.getBytes());
		byte[] digest = md.digest();
		StringBuffer hexString = new StringBuffer();

		for (int i = 0; i < digest.length; i++) {
			hexString.append(Integer.toHexString(0xFF & digest[i]));
		}
		return hexString.toString();
	}

	public byte[] hashToByteArray(byte[] byteArray) {
		this.md.update(byteArray);
		return this.md.digest();
	}
}
