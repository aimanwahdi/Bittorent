package bittorensimag.Util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

public class Hashage {
	private static final Logger LOG = Logger.getLogger(Hashage.class);

	private String algorithm; // l'algorithme utilisé pour le hashage peut être SHA-1, SHA-256...
	MessageDigest md = null;

	private final static String SHA_1 = "SHA-1";
	public final static Hashage sha1Hasher = new Hashage(SHA_1);

	public Hashage(String algorithm) {
		super();
		this.algorithm = algorithm;
		try {
			this.md = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			LOG.fatal("The algorithm " + algorithm + " is not correct");
		}
	}

	/**
	 * return hash of the message in format Hex
	 * 
	 * @param message
	 * @return
	 * @throws NoSuchAlgorithmException
	 */

	public String hashToHex(String message) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(algorithm);
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
