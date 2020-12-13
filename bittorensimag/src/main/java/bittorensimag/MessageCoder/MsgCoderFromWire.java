package bittorensimag.MessageCoder;

import java.io.IOException;
import java.nio.ByteBuffer;
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
        try {
            return Integer.parseInt(lengthString, 16);
        } catch (NumberFormatException e) {
            LOG.error("Could not format number, received continuation data ? " + lengthString);
            return -1;
        }

    }

    @Override
    public Object fromWire(SocketChannel clntChan) throws IOException {

        ByteBuffer firstByteBuffer = ByteBuffer.allocate(1);
        int readResult = 0;

        long startTime = System.currentTimeMillis(); // fetch starting time

        while (true) {
            readResult = clntChan.read(firstByteBuffer);
            // end of stream for channel
            if (readResult == -1 && (System.currentTimeMillis() - startTime) >= 10000) {
                LOG.error("Channel reached end of stream, closing channel");
                return -1;
            }
            // read nothing and timeout reached
            else if (readResult == 0 && (System.currentTimeMillis() - startTime) >= 10000) {
                LOG.debug("10 sec timeout reached");
                return -1;
            } 
            else if (readResult > 0) {
                // we read something
                firstByteBuffer.rewind();
                return fromWire(firstByteBuffer.get(), clntChan);
            }
        }
    }

    @Override
    public Object fromWire(Byte firstByte, SocketChannel clntChan) throws IOException {
        ByteBuffer oneByteBuffer = ByteBuffer.allocate(1);
        // first byte is part of length so cannot be negative
        if (firstByte < 0) {
            LOG.error("Received negative first byte, surely continuation data, reading the rest");
            while (true) {
                int readResult = clntChan.read(oneByteBuffer);
                if (readResult == -1) {
                    return -1;
                }
                if (readResult == 0) {
                    break;
                }
            }
            LOG.error("Finished consuming channel");
            return null;
        }
        if (firstByte == Handshake.HANDSHAKE_LENGTH) {
            LOG.debug("Received Message : Handshake");

            // reading the protocol name
            byte[] protocol = this.readLength(clntChan, Handshake.HANDSHAKE_LENGTH);

            if (Handshake.protocolName.compareTo(new String(protocol)) != 0) {
                LOG.error("This is not bittorent protocol : " + new String(protocol));
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
            // fisrt byte is positive and not handshake try to read it's length

            // read three last bytes of length
            int totalLength = this.readTotalLength(clntChan, firstByte);

            // LOG.debug("totalLength " + totalLength);

            // not readable data surely continuation or extended
            if (totalLength > Piece.PIECE_LENGTH) {
                LOG.error(
                        "Message received too long : " + totalLength + ", surely continuation data, reading the rest");
                while (true) {
                    int readResult = clntChan.read(oneByteBuffer);
                    if (readResult == -1) {
                        return -1;
                    }
                    if (readResult == 0) {
                        break;
                    }
                }
                LOG.error("Finished consuming channel");
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
                    if (index == -1) {
                        break;
                    } else {

                    return new Have(index);
                }

                case Bitfield.BITFIELD_TYPE:
                    byte[] bitfieldData = this.readLength(clntChan, totalLength - 1);
                    return new Bitfield(bitfieldData);
                case Request.REQUEST_TYPE:
                    int requestIndex = this.readLengthInt(clntChan, 4);
                    int requestOffset = this.readLengthInt(clntChan, 4);
                    int lengthPiece = this.readLengthInt(clntChan, 4);
                    if (requestIndex == -1 || requestOffset == -1 || lengthPiece == -1) {
                        break;
                    } else {
                        return new Request(requestIndex, requestOffset, lengthPiece);
                    }

                case Piece.PIECE_TYPE:
                    int pieceIndex = this.readLengthInt(clntChan, 4);
                    int beginOffset = this.readLengthInt(clntChan, 4);
                    if (pieceIndex == -1 || beginOffset == -1) {
                        break;
                    } else {
                        byte[] data = readData(clntChan, totalLength);
                        return new Piece(totalLength, pieceIndex, beginOffset, data);
                    }

                // TODO if implementing endgame
                // case CANCEL.CANCEL_TYPE:
                // return new Cancel();
                default:
                    break;
            }

        }
        LOG.error("Message type not recognized");
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

        try {
            return Integer.parseInt(firstByteString + secondByteString + thirdByteString + fourthByteString, 16);
        } catch (NumberFormatException e) {
            LOG.error("Could not format number, received continuation data ? " + firstByteString + secondByteString
                    + thirdByteString + fourthByteString);
            return 0;
        }
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
