package bittorensimag;
import java.io.IOException;

public interface MsgCoder {
	byte[] toWire(Msg msg) throws IOException; //converts the message to a sequence of byte
	HandshakeMsg fromWire(byte[] input) throws IOException; //parses a given sequence of bytes
}
