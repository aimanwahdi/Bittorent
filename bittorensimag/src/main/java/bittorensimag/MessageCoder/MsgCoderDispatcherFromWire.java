package bittorensimag.MessageCoder;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface MsgCoderDispatcherFromWire {
    Object fromWire(SocketChannel clntChan) throws IOException; // parses a given sequence of bytes

    Object fromWire(Byte firstByte, SocketChannel clntChan) throws IOException; // parses a given sequence
                                                                           // of bytes
}
