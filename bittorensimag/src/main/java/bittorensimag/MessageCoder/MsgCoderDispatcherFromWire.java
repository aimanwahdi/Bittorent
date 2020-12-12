package bittorensimag.MessageCoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface MsgCoderDispatcherFromWire {
    Object fromWire(SocketChannel clntChan) throws IOException; // parses a given sequence of bytes

    Object fromWire(ByteBuffer firstByteBuffer, SocketChannel clntChan ) throws IOException; // parses a given sequence
                                                                           // of bytes
}
