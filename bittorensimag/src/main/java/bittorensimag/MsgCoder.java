package bittorensimag;

import java.io.IOException;

public interface MsgCoder {
	void accept(MsgCoderDispatcher dispatcher) throws IOException;

	Msg fromWire(byte[] input) throws IOException; // parses a given sequence of bytes
}
