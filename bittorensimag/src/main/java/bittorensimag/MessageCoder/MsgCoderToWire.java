package bittorensimag.MessageCoder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import bittorensimag.Util.Util;
import bittorensimag.Messages.*;

public class MsgCoderToWire implements MsgCoderDispatcherToWire {
	// writing a message in OutputStream
	public void frameMsg(byte[] message, OutputStream out) throws IOException {
		// write message
		out.write(message);
		out.flush();
	}

	@Override
	public byte[] toWire(Bitfield msg) throws IOException { // converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);

		if (msg.getMsgType() == 5) {
			out.writeInt(msg.getMsgLength());
			out.writeByte(msg.getMsgType());

			for (byte i : msg.getBitfieldDATA()) {
				out.writeByte(i);
			}
		}

		out.flush();
		byte[] data = byteStream.toByteArray();
		return data;
	}

	@Override
	public byte[] toWire(Handshake msg) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);

		out.writeByte(msg.getProtocolNameLength());
		out.write(msg.getProtocolName().getBytes(Charset.forName("UTF-8")));
		out.writeLong(msg.getReservedExtensionByte());

		byte[] sha1 = Util.hexStringToByteArray(msg.getSha1Hash());

		for (int i = 0; i < sha1.length; i++) {
			out.writeByte(sha1[i]);
		}

		for (int i = 0; i < msg.getPeerId().length; i++) {
			out.writeByte(msg.getPeerId()[i]);
		}

		out.flush();
		byte[] data = byteStream.toByteArray();
		return data;
	}

	@Override
	public byte[] toWire(Have msg) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);

		out.writeInt(msg.getMsgLength());
		out.writeByte(msg.getMsgType());
		out.writeInt(msg.getIndex());

		out.flush();
		byte[] data = byteStream.toByteArray();
		return data;
	}

	@Override
	public byte[] toWire(Msg msg) throws IOException { // converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);

		if (msg.getMsgType() >= 0 && msg.getMsgType() < 4) {
			out.writeInt(msg.getMsgLength());
			out.writeByte(msg.getMsgType());
		}

		out.flush();
		byte[] data = byteStream.toByteArray();
		return data;
	}

	@Override
	public byte[] toWire(Piece msg) throws IOException { // converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);

		out.writeInt(msg.getMsgLength());
		out.writeByte(msg.getMsgType());
		out.writeInt(msg.getPieceIndex());
		out.writeInt(msg.getBeginOffset());

		out.write(msg.getData());

		out.flush();
		byte[] data = byteStream.toByteArray();
		return data;
	}

	@Override
	public byte[] toWire(Request msg) throws IOException { // converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);

		out.writeInt(msg.getMsgLength());
		out.writeByte(msg.getMsgType());
		out.writeInt(msg.getIndex());
		out.writeInt(msg.getBeginOffset());
		out.writeInt(msg.getPieceLength());

		out.flush();
		byte[] data = byteStream.toByteArray();
		return data;
	}
}
