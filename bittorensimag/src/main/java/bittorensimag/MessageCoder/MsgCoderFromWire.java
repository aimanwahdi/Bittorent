package bittorensimag.MessageCoder;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.apache.log4j.Logger;

import bittorensimag.Messages.*;
import bittorensimag.Util.Util;

public class MsgCoderFromWire implements MsgCoderDispatcherFromWire {
    private static final Logger LOG = Logger.getLogger(MsgCoderFromWire.class);

    public byte[] readLength(DataInputStream in, int length) {
        byte[] bytesArray = new byte[length];
        for (int i = 0; i < length; i++) {
            try {
                bytesArray[i] = in.readByte();
            } catch (IOException e) {
                LOG.error("Error while reading bytes for length " + length);
            }
        }
        return bytesArray;
    }

    public int readLengthInt(DataInputStream in, int length) {
        String lengthString = "";
        for (int i = 0; i < length; i++) {
            try {
                lengthString += Util.intToHexStringWith0(in.readUnsignedByte());
            } catch (IOException e) {
                LOG.error("Error while reading ints for length " + length);
            }
        }
        return Integer.parseInt(lengthString, 16);
    }

    @Override
    public Object fromWire(DataInputStream in) throws IOException {
        int firstByte;
        try {
            firstByte = in.readUnsignedByte();
        } catch (EOFException e) {
            LOG.error("No more messages to read ! Closing socket");
            return -1;
        }
        return fromWire(firstByte, in);
    }

    @Override
    public Object fromWire(int firstByte, DataInputStream in) throws IOException {
        if (firstByte == Handshake.HANDSHAKE_LENGTH) {
            LOG.debug("Received Message : Handshake");

            // reading the protocol name
            // TODO verify it is Bittorent
            byte[] protocol = this.readLength(in, 19);
            if (Handshake.protocolName.compareTo(new String(protocol)) != 0) {
                LOG.error("This is not bittorent protocol");
                return null;
            }

            // reading extension bytes
            byte[] extensionBytes = this.readLength(in, 8);
            long extensionBytesLong = Long.parseLong(Util.bytesToHex(extensionBytes));

            // reading sha1 hash
            // TODO verify hash
            byte[] sha1HashBytes = this.readLength(in, 20);
            String sha1Hash = Util.bytesToHex(sha1HashBytes);

            // reading peer_id
            // TODO verify peer_id ?
            byte[] peerId = this.readLength(in, 20);

            return new Handshake(sha1Hash, peerId, extensionBytesLong);
        } else {
            // read three last bytes of length
            int totalLength = this.readTotalLength(in, firstByte);

            // read type
            int type = in.readUnsignedByte();
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
                    int index = this.readLengthInt(in, totalLength - 1);
                    return new Have(index);
                case Bitfield.BITFIELD_TYPE:
                    byte[] bitfieldData = this.readLength(in, totalLength - 1);
                    return new Bitfield(bitfieldData);
                case Request.REQUEST_TYPE:
                    int requestIndex = in.readInt();
                    int requestOffset = in.readInt();
                    int lengthPiece = this.readLengthInt(in, 4);
                    return new Request(requestIndex, requestOffset, lengthPiece);
                case Piece.PIECE_TYPE:
                    int pieceIndex = in.readInt();
                    int beginOffset = in.readInt();
                    byte[] data = readData(in, pieceIndex, totalLength);
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

    private int readTotalLength(DataInputStream in, int firstByte) throws IOException {
        String firstByteString = Util.intToHexStringWith0(firstByte);
        String secondByteString = Util.intToHexStringWith0(in.readUnsignedByte());
        String thirdByteString = Util.intToHexStringWith0(in.readUnsignedByte());
        String fourthByteString = Util.intToHexStringWith0(in.readUnsignedByte());

        return Integer.parseInt(firstByteString + secondByteString + thirdByteString + fourthByteString, 16);
    }

    /*
     * read all the data of a piece and put it the map
     */
    private byte[] readData(DataInputStream in, int pieceIndex, int lengthMessage) {
        ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();

        int pieceDataLength = lengthMessage - Piece.HEADER_LENGTH;

        for (int i = 0; i < pieceDataLength; i++) {
            try {
                int nextByte = in.readUnsignedByte();
                messageBuffer.write(nextByte);

            } catch (IOException e) {
                LOG.error("Error while reading data inside piece message");
            }
        }
        return messageBuffer.toByteArray();
    }
}
