package bittorensimag.Messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;

public class Have extends Msg {
	private static final Logger LOG = Logger.getLogger(Have.class);

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

	// TODO change return type to boolean to know if it worked
	public static void sendMessage(int index, SocketChannel clntChan) throws IOException {
		MsgCoderToWire coderToWire = new MsgCoderToWire();
		Have have = new Have(index);

		try {
			ByteBuffer writeBuf = ByteBuffer.wrap(coderToWire.toWire(have));

			if (writeBuf.hasRemaining()) {
				clntChan.write(writeBuf);
			}
			LOG.debug("Message Have sent for index=" + index);
		} catch (IOException e) {
			LOG.error("Error sending have message " + e.getMessage());
		}
	}

}
