package bittorensimag.Util;

import java.util.Map;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;

public class BencodeMap {
    // Methods to get keys
    public static String getKeyString(Map<String, BEncodedValue> map, String key) throws InvalidBEncodingException {
        try {
            if (map.containsKey(key)) {
                return map.get(key).getString();
            } else {
                System.out.println("Torrent file does not contain key : " + key);
            }
        } catch (InvalidBEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int getKeyInt(Map<String, BEncodedValue> map, String key) throws InvalidBEncodingException {
        try {
            if (map.containsKey(key)) {
                return map.get(key).getInt();
            } else {
                System.out.println("Torrent file does not contain key : " + key);
            }
        } catch (InvalidBEncodingException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static byte[] getKeyByte(Map<String, BEncodedValue> map, String key) throws InvalidBEncodingException {
        try {
            if (map.containsKey(key)) {
                return map.get(key).getBytes();
            } else {
                System.out.println("Torrent file does not contain key : " + key);
            }
        } catch (InvalidBEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void fillBencodeMapString(Map<String, BEncodedValue> sourceMap, Map<String, Object> destMap,
            String[] keyStrings) throws InvalidBEncodingException {
        for (String key : keyStrings) {
            destMap.put(key, BencodeMap.getKeyString(sourceMap, key));
        }
    }

    public static void fillBencodeMapInt(Map<String, BEncodedValue> sourceMap, Map<String, Object> destMap,
            String[] keyStrings) throws InvalidBEncodingException {
        for (String key : keyStrings) {
            destMap.put(key, BencodeMap.getKeyInt(sourceMap, key));
        }
    }

    public static void fillBencodeMapBytes(Map<String, BEncodedValue> sourceMap, Map<String, Object> destMap,
            String[] keyStrings) throws InvalidBEncodingException {
        for (String key : keyStrings) {
            destMap.put(key, BencodeMap.getKeyByte(sourceMap, key));
        }
    }
}
