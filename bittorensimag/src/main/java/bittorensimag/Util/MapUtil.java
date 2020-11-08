package bittorensimag.Util;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;

public class MapUtil {
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
            destMap.put(key, MapUtil.getKeyString(sourceMap, key));
        }
    }

    public static void fillBencodeMapInt(Map<String, BEncodedValue> sourceMap, Map<String, Object> destMap,
            String[] keyStrings) throws InvalidBEncodingException {
        for (String key : keyStrings) {
            destMap.put(key, MapUtil.getKeyInt(sourceMap, key));
        }
    }

    public static void fillBencodeMapBytes(Map<String, BEncodedValue> sourceMap, Map<String, Object> destMap,
            String[] keyStrings) throws InvalidBEncodingException {
        for (String key : keyStrings) {
            destMap.put(key, MapUtil.getKeyByte(sourceMap, key));
        }
    }

    public static byte[] convertHashMapToByteArray(int lengthOutput, Map<Integer, byte[]> dataMap) {
        ByteBuffer buffer = ByteBuffer.allocate(lengthOutput);
        Iterator<Map.Entry<Integer, byte[]>> iterator = dataMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, byte[]> mapEntry = (Map.Entry<Integer, byte[]>) iterator.next();
            System.out.println("clé: " + mapEntry.getKey());
            buffer.put((byte[]) mapEntry.getValue());
        }
        return buffer.array();
    }
}
