package bittorensimag;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Test {
	public static void main(String args[]) throws Exception, IOException {

		MsgCoderToWire coder = new MsgCoderToWire();

		// create output stream
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteStream);

		// create handshakeMsg
		HandshakeMsg handshakeMsg = new HandshakeMsg("067133ace5dd0c5027b99de5d4ba512828208d5b");

		// create socket
		String destAddr = "127.0.0.1"; // Destination address
		int destPort = 6881; // Destination port
		Socket sock = new Socket(destAddr, destPort);
		OutputStream Out = sock.getOutputStream();

		// send handshake msg
		frameMsg(coder.toWire(handshakeMsg), Out);

		// create input stream
		InputStream inputStream = sock.getInputStream();
		DataInputStream in = new DataInputStream(inputStream);

		while (true) {

			try {
				byte[] req;
				req = nextMsg(in);
				if (req != null) {
					System.out.println("Received message (" + req.length + " bytes)");

					byte[] dataBitfield = { 0, 0 };

					MsgBitfield msgBitfield = new MsgBitfield(3, 5, dataBitfield);
					Msg msgInterested = new Msg(1, 2);

					frameMsg(coder.toWire(msgBitfield), Out);
					frameMsg(coder.toWire(msgInterested), Out);

				}

			} catch (IOException ioe) {
				System.err.println("Error handling client: " + ioe.getMessage());
			}

		}
	}

	// reading a message
	public static byte[] nextMsg(DataInputStream in) throws IOException {
		ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();
		int nextByte = in.read();
		int sum = 0;
		// correct condition
		while (nextByte != -1 && sum < 35) {
			nextByte = in.read();
			sum++;
			messageBuffer.write(nextByte); // write byte to buffer
			System.out.println("reading");
		}
		return messageBuffer.toByteArray();
	}

	// writing a message in OutputStream
	public static void frameMsg(byte[] message, OutputStream out) throws IOException {
		// write message
		System.out.println("writing");
		out.write(message);
		out.flush();

	}

}
