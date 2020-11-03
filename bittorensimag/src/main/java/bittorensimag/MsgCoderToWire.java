package bittorensimag;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import bittorensimag.Util.Util;

public class MsgCoderToWire implements MsgCoderDispatcher {
	@Override
	public byte[] toWire(MsgBitfield msg) throws IOException { // converts the message to a sequence of byte
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
	public byte[] toWire(HandshakeMsg msg) throws IOException {
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
	public byte[] toWire(MsgHave msg) throws IOException {
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
	public byte[] toWire(MsgPiece msg) throws IOException { // converts the message to a sequence of byte
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);

		out.writeInt(msg.getMsgLength());
		out.writeByte(msg.getMsgType());
		out.writeInt(msg.getPieceIndex());
		out.writeInt(msg.getBeginOffset());

		// should be corrected later because right now it doesn't write all the data
		out.write(msg.getData());

		out.flush();
		byte[] data = byteStream.toByteArray();
		return data;
	}

	@Override
	public byte[] toWire(MsgRequest msg) throws IOException { // converts the message to a sequence of byte
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
