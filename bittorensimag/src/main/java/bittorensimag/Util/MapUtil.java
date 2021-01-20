package bittorensimag.Util;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;

public class MapUtil {
    private static final Logger LOG = Logger.getLogger(MapUtil.class);

    public static boolean ASC = true;
    public static boolean DESC = false;

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

    public static Map<Integer, ArrayList<Socket>> sortBySize(Map<Integer, ArrayList<Socket>> unsortMap,
            final boolean order) {

        List<Entry<Integer, ArrayList<Socket>>> list = new LinkedList<Entry<Integer, ArrayList<Socket>>>(
                unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<Integer, ArrayList<Socket>>>() {
            public int compare(Entry<Integer, ArrayList<Socket>> o1, Entry<Integer, ArrayList<Socket>> o2) {
                // if (order) {
                // return o1.getValue().compareTo(o2.getValue());
                // } else {
                // return o2.getValue().compareTo(o1.getValue());
                // }
                if (order) {
                    return o1.getValue().size() - o2.getValue().size();
                } else {
                    return o2.getValue().size() - o1.getValue().size();

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<Integer, ArrayList<Socket>> sortedMap = new LinkedHashMap<Integer, ArrayList<Socket>>();
        for (Entry<Integer, ArrayList<Socket>> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static String ArrayListToString(ArrayList<Integer> arraylist) {
        String s = "[ ";
        for (Integer integer : arraylist) {
            s += integer + ", ";
        }
        s += "]";
        return s;
    }
}
