package bittorensimag.MessageCoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import bittorensimag.Messages.*;
import bittorensimag.Util.Util;

public class MsgCoderFromWire implements MsgCoderDispatcherFromWire {
    private static final Logger LOG = Logger.getLogger(MsgCoderFromWire.class);

    public byte[] readLength(SocketChannel clntChan, int length) throws IOException {
        byte[] bytesArray = new byte[length];
        ByteBuffer readBuffer = ByteBuffer.allocate(length);
        int receivedByte = 0;

        while (readBuffer.remaining() != 0) {
            receivedByte = clntChan.read(readBuffer);
        }
        readBuffer.rewind();

        readBuffer.get(bytesArray, 0, length);
        return bytesArray;
    }

    public int readLengthInt(SocketChannel clntChan, int length) throws IOException {
        String lengthString = "";

        ByteBuffer readBuffer = ByteBuffer.allocate(length);
        int receivedByte = 0;

        long startTime = System.currentTimeMillis(); // fetch starting time

        while (readBuffer.remaining() != 0 && (System.currentTimeMillis() - startTime) < 10000) {
            receivedByte = clntChan.read(readBuffer);
        }

        if (receivedByte == 0 && (System.currentTimeMillis() - startTime) >= 10000) {
            LOG.debug("no reponse");
            return 0;
        }

        for (int i = 0; i < length; i++) {
            lengthString += Util.intToHexStringWith0(readBuffer.get(i) & 0xff);
        }
        return Integer.parseInt(lengthString, 16);
    }

    @Override
    public Object fromWire(SelectionKey key) throws IOException {
        SocketChannel clntChan = (SocketChannel) key.channel();

        // LOG.debug("created channel " + clntChan);

        ByteBuffer firstByteBuffer = ByteBuffer.allocate(1);
        int firstByte = 0;

        long startTime = System.currentTimeMillis(); // fetch starting time

        while (firstByteBuffer.remaining() != 0 && firstByte != -1
                && (System.currentTimeMillis() - startTime) < 10000) {
            firstByte = clntChan.read(firstByteBuffer);
        }
        // LOG.debug("firstByte " + firstByteBuffer.get(0));

        if (firstByte == 0 && (System.currentTimeMillis() - startTime) >= 10000) {
            LOG.debug("no reponse");
            return null;
        }

        return fromWire(firstByteBuffer, clntChan);
    }

    @Override
    public Object fromWire(ByteBuffer firstByteBuffer, SocketChannel clntChan) throws IOException {
        // set position to zero
        firstByteBuffer.rewind();
        byte firstByte = firstByteBuffer.get();

        if (firstByte == Handshake.HANDSHAKE_LENGTH) {
            LOG.debug("Received Message : Handshake");

            // reading the protocol name
            byte[] protocol = this.readLength(clntChan, Handshake.HANDSHAKE_LENGTH);

            if (Handshake.protocolName.compareTo(new String(protocol)) != 0) {
                LOG.error("This is not bittorent protocol");
                return null;
            }

            // reading extension bytes
            byte[] extensionBytes = this.readLength(clntChan, 8);
            long extensionBytesLong = Long.parseLong(Util.bytesToHex(extensionBytes));

            // LOG.debug("extensionBytesLong " + extensionBytesLong);

            // reading sha1 hash
            byte[] sha1HashBytes = this.readLength(clntChan, 20);
            String sha1Hash = Util.bytesToHex(sha1HashBytes);

            // LOG.debug("sha1Hash " + sha1Hash);

            // reading peer_id
            byte[] peerId = this.readLength(clntChan, 20);

            // LOG.debug("peerId " + peerId);

            return new Handshake(sha1Hash, peerId, extensionBytesLong);
        } else {
            // read three last bytes of length
            int totalLength = this.readTotalLength(clntChan, firstByte);

            // LOG.debug("totalLength " + totalLength);

            if (totalLength == 0) {
                return null;
            }

            // read type
            int type = this.readLength(clntChan, 1)[0];

            LOG.debug("Received Message : " + Msg.messagesNames.get(type));

            switch (type) {
                case Simple.CHOKE:
                    return new Simple(Simple.CHOKE);
                case Simple.UNCHOKE:
                    return new Simple(Simple.UNCHOKE);
                case Simple.INTERESTED:
                    return new Simple(Simple.INTERESTED);
                case Simple.NOTINTERESTED:
                    return new Simple(Simple.NOTINTERESTED);
                case Have.HAVE_TYPE:
                    int index = this.readLengthInt(clntChan, totalLength - 1);
                    return new Have(index);
                case Bitfield.BITFIELD_TYPE:
                    byte[] bitfieldData = this.readLength(clntChan, totalLength - 1);
                    return new Bitfield(bitfieldData);
                case Request.REQUEST_TYPE:
                    int requestIndex = this.readLengthInt(clntChan, 4);
                    int requestOffset = this.readLengthInt(clntChan, 4);
                    int lengthPiece = this.readLengthInt(clntChan, 4);
                    return new Request(requestIndex, requestOffset, lengthPiece);
                case Piece.PIECE_TYPE:
                    int pieceIndex = this.readLengthInt(clntChan, 4);
                    int beginOffset = this.readLengthInt(clntChan, 4);
                    byte[] data = readData(clntChan, totalLength);
                    return new Piece(totalLength, pieceIndex, beginOffset, data);
                // TODO if implementing endgame
                // case CANCEL.CANCEL_TYPE:
                // return new Cancel();
                default:
                    break;
            }

        }
        return null;
    }

    private int readTotalLength(SocketChannel clntChan, int firstByte) throws IOException {

        ByteBuffer readBuffer = ByteBuffer.allocate(3);
        int receivedByte = 0;

        while (readBuffer.remaining() != 0 && receivedByte != -1) {
            receivedByte = clntChan.read(readBuffer);
        }

        String firstByteString = Util.intToHexStringWith0(firstByte & 0xff);
        String secondByteString = Util.intToHexStringWith0(readBuffer.get(0) & 0xff);
        String thirdByteString = Util.intToHexStringWith0(readBuffer.get(1) & 0xff);
        String fourthByteString = Util.intToHexStringWith0(readBuffer.get(2) & 0xff);

        return Integer.parseInt(firstByteString + secondByteString + thirdByteString + fourthByteString, 16);
    }

    /*
     * read all the data of a piece and put it the map
     */
    private byte[] readData(SocketChannel clntChan, int lengthMessage) throws IOException {
        int pieceDataLength = lengthMessage - Piece.HEADER_LENGTH;
        byte[] bytesArray = new byte[pieceDataLength];

        ByteBuffer readBuffer = ByteBuffer.allocate(pieceDataLength);
        int receivedByte = 0;

        while (readBuffer.remaining() != 0) {
            receivedByte = clntChan.read(readBuffer);
        }
        readBuffer.rewind();

        readBuffer.get(bytesArray, 0, pieceDataLength);

        return bytesArray;

    }
}
