package bittorensimag.MessageCoder;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface MsgCoderDispatcherFromWire {
    Object fromWire(SelectionKey key ) throws IOException; // parses a given sequence of bytes

    Object fromWire(ByteBuffer readBuf, SocketChannel clntChan ) throws IOException; // parses a given sequence
                                                                           // of bytes
}
