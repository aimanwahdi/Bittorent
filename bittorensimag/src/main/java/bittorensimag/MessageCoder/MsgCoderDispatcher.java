package bittorensimag.MessageCoder;

import java.io.IOException;

import bittorensimag.Messages.*;

public interface MsgCoderDispatcher {
    byte[] toWire(Bitfield msg) throws IOException; // converts the message to a sequence of byte

    byte[] toWire(Handshake msg) throws IOException;

    byte[] toWire(Have msg) throws IOException;

    byte[] toWire(Msg msg) throws IOException;

    byte[] toWire(Piece msg) throws IOException;

    byte[] toWire(Request msg) throws IOException;
}
