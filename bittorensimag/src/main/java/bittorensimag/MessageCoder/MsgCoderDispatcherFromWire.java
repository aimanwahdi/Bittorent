package bittorensimag.MessageCoder;

import java.io.DataInputStream;
import java.io.IOException;

public interface MsgCoderDispatcherFromWire {
    Object fromWire(DataInputStream in) throws IOException; // parses a given sequence of bytes

    Object fromWire(int firstByte, DataInputStream in) throws IOException; // parses a given sequence
                                                                           // of bytes
}
