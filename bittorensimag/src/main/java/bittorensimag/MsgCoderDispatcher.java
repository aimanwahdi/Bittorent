package bittorensimag;

import java.io.IOException;

public interface MsgCoderDispatcher {
    byte[] toWire(MsgBitfield msg) throws IOException; // converts the message to a sequence of byte

    byte[] toWire(HandshakeMsg msg) throws IOException;

    byte[] toWire(MsgHave msg) throws IOException;

    byte[] toWire(Msg msg) throws IOException;

    byte[] toWire(MsgPiece msg) throws IOException;

    byte[] toWire(MsgRequest msg) throws IOException;
}
