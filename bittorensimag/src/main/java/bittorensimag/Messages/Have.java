package bittorensimag.Messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import bittorensimag.MessageCoder.*;

public class Have extends Msg {
	private int index;

	public final static int HAVE_LENGTH = 5;
	public final static int HAVE_TYPE = 4;

	public Have(int index) {
		super(HAVE_LENGTH, HAVE_TYPE);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public static void sendMessage(int index, OutputStream out) throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Have have = new Have(index);
		coderToWire.frameMsg(coderToWire.toWire(have), out);
		// TODO Add info
		System.out.println("Message Have sent index=" + index);
	}

}
