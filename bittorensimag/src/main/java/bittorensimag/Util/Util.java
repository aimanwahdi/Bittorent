package bittorensimag.Util;

import java.util.Arrays;
import java.util.Random;

public class Util {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    // convert int to hexadecimal string with trailing 0 from 01 to 0F
    public static String intToHexStringWith0(int n) {
        return String.format("%1$02X", n);
    }

    // convert bytearray into a string
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    // convert hexString into a bytearray
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String generateRandomAlphanumeric(int targetStringLength) {
        int leftLimit = 48; // number '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97)).limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        return generatedString;
    }
    
    /*
     * concat two byte arrays 
     */
    public static byte[] concat(byte[] first, byte[] second) {
    	byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
    }
}
