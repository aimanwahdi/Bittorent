package bittorensimag.MessageCoder;

import java.io.IOException;

import bittorensimag.Messages.*;

public interface MsgCoder {
	void accept(MsgCoderDispatcher dispatcher) throws IOException;

	Msg fromWire(byte[] input) throws IOException; // parses a given sequence of bytes
}
