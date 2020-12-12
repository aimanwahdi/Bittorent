package bittorensimag.Util;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;

public class MapUtil {
    private static final Logger LOG = Logger.getLogger(MapUtil.class);

    // Methods to get keys
    public static String getKeyString(Map<String, BEncodedValue> map, String key) throws InvalidBEncodingException {
        try {
            if (map.containsKey(key)) {
                return map.get(key).getString();
            } else {
                // LOG.warn("Torrent file does not contain key : " + key);
            }
        } catch (InvalidBEncodingException e) {
            LOG.error("Could not decode string from key : " + key);
        }
        return "";
    }

    public static int getKeyInt(Map<String, BEncodedValue> map, String key) throws InvalidBEncodingException {
        try {
            if (map.containsKey(key)) {
                return map.get(key).getInt();
            } else {
                // LOG.warn("Torrent file does not contain key : " + key);
            }
        } catch (InvalidBEncodingException e) {
            LOG.error("Could not decode int from key : " + key);
        }
        return 0;
    }

    public static byte[] getKeyByte(Map<String, BEncodedValue> map, String key) throws InvalidBEncodingException {
        try {
            if (map.containsKey(key)) {
                return map.get(key).getBytes();
            } else {
                // LOG.warn("Torrent file does not contain key : " + key);
            }
        } catch (InvalidBEncodingException e) {
            LOG.error("Could not decode bytes from key : " + key);
        }
        return null;
    }

    public static void fillBencodeMapString(Map<String, BEncodedValue> sourceMap, Map<String, Object> destMap,
            String[] keyStrings) throws InvalidBEncodingException {
        for (String key : keyStrings) {
            destMap.put(key, MapUtil.getKeyString(sourceMap, key));
            // LOG.debug("String key " + key + " has been successfully added");
        }
    }

    public static void fillBencodeMapInt(Map<String, BEncodedValue> sourceMap, Map<String, Object> destMap,
            String[] keyStrings) throws InvalidBEncodingException {
        for (String key : keyStrings) {
            destMap.put(key, MapUtil.getKeyInt(sourceMap, key));
            // LOG.debug("Int key " + key + " has been successfully added");
        }
    }

    public static void fillBencodeMapBytes(Map<String, BEncodedValue> sourceMap, Map<String, Object> destMap,
            String[] keyStrings) throws InvalidBEncodingException {
        for (String key : keyStrings) {
            destMap.put(key, MapUtil.getKeyByte(sourceMap, key));
            // LOG.debug("Bytes key " + key + " has been successfully added");
        }
    }

    public static byte[] convertHashMapToByteArray(int lengthOutput, Map<Integer, byte[]> dataMap) {
        ByteBuffer buffer = ByteBuffer.allocate(lengthOutput);
        Iterator<Map.Entry<Integer, byte[]>> iterator = dataMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, byte[]> mapEntry = (Map.Entry<Integer, byte[]>) iterator.next();
            buffer.put((byte[]) mapEntry.getValue());
            // LOG.debug("Key " + mapEntry.getKey() + " has been successfully added");
        }
        return buffer.array();
    }
}
