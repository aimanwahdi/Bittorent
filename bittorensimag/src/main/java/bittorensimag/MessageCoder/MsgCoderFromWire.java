package bittorensimag.MessageCoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import bittorensimag.Messages.*;
import bittorensimag.Torrent.Torrent;
import bittorensimag.Util.Util;

public class MsgCoderFromWire implements MsgCoderDispatcherFromWire {
    private static final Logger LOG = Logger.getLogger(MsgCoderFromWire.class);
    
    private byte[] readSafe(SocketChannel clntChan, int length) throws IOException {
        int readResult = 0;
        byte[] byteArray = new byte[length];
        ByteBuffer readBuffer = ByteBuffer.allocate(length);
    
        long startTime = System.currentTimeMillis(); // fetch starting time
        while (readBuffer.hasRemaining() && (System.currentTimeMillis() - startTime) <= 10000) {
            readResult = clntChan.read(readBuffer);
            // end of stream for channel
            if (readResult == -1 && (System.currentTimeMillis() - startTime) >= 10000) {
                LOG.error("Channel reached end of stream, closing channel");
                return null;
            }
            // read nothing and timeout reached
            else if (readResult == 0 && (System.currentTimeMillis() - startTime) >= 10000) {
                LOG.debug("10 sec timeout reached, closing channel");
                return null;
            } else if (!readBuffer.hasRemaining()) {
                // we received everthing (just read last part needed)
                readBuffer.rewind();
                readBuffer.get(byteArray);
                return byteArray;
            }
        }
        // we could not read everything in time
        LOG.error("Could not read the entire message in time, closing channel");
        return null;
    }

    public int readLengthInt(SocketChannel clntChan, int length) throws IOException {
        String lengthString = "";

        byte[] byteArray = this.readSafe(clntChan, length);

        if (byteArray == null) {
            LOG.error("Unable to read length as int");
            return -1;
        }

        for (int i = 0; i < length; i++) {
            lengthString += Util.intToHexStringWith0(byteArray[i] & 0xff);
        }
        try {
            return Integer.parseInt(lengthString, 16);
        } catch (NumberFormatException e) {
            LOG.error("Could not format number, received continuation data ? " + lengthString);
            return -1;
        }

    }

    private int readTotalLength(SocketChannel clntChan, int firstByte) throws IOException {
        byte[] byteArray = this.readSafe(clntChan, 3);
        if (byteArray == null) {
            LOG.error("Unable to read total length");
            return -1;
        }

        String firstByteString = Util.intToHexStringWith0(firstByte & 0xff);
        String secondByteString = Util.intToHexStringWith0(byteArray[0] & 0xff);
        String thirdByteString = Util.intToHexStringWith0(byteArray[1] & 0xff);
        String fourthByteString = Util.intToHexStringWith0(byteArray[2] & 0xff);

        // LOG.debug("Total Lenght before cast : " + firstByteString + secondByteString
        // + thirdByteString + fourthByteString);
        try {
            return Integer.parseInt(firstByteString + secondByteString + thirdByteString + fourthByteString, 16);
        } catch (NumberFormatException e) {
            LOG.error("Could not format number, received continuation data ? " + firstByteString + secondByteString
                    + thirdByteString + fourthByteString);
            return -1;
        }
    }

    @Override
    public Object fromWire(SocketChannel clntChan) throws IOException {

        ByteBuffer firstByteBuffer = ByteBuffer.allocate(1);
        int readResult = 0;

        long startTime = System.currentTimeMillis(); // fetch starting time

        while ((System.currentTimeMillis() - startTime) <= 10000) {
            readResult = clntChan.read(firstByteBuffer);
            // end of stream for channel
            if (readResult == -1 && (System.currentTimeMillis() - startTime) >= 10000) {
                LOG.error("Channel reached end of stream, closing channel");
                return null;
            }
            // read nothing and timeout reached
            else if (readResult == 0 && (System.currentTimeMillis() - startTime) >= 10000) {
                LOG.debug("10 sec timeout reached");
                return null;
            } 
            else if (readResult > 0) {
                // we read something
                firstByteBuffer.rewind();
                return fromWire(firstByteBuffer.get(), clntChan);
            }
        }
        LOG.error("Could not read first byte in time,closing channel");
        return null;
    }

    @Override
    public Object fromWire(Byte firstByte, SocketChannel clntChan) throws IOException {
        // first byte is part of length so cannot be negative
        if (firstByte < 0) {
            LOG.error("Received negative first byte : " + firstByte);
            return null;
        }
        if (firstByte == Handshake.HANDSHAKE_LENGTH) {
            LOG.debug("Received Message : Handshake");

            // reading the protocol name
            byte[] protocol = this.readSafe(clntChan, Handshake.HANDSHAKE_LENGTH);
            if (protocol == null) {
                LOG.error("Could not read protocol for handshake");
                return null;
            }
            if (Handshake.protocolName.compareTo(new String(protocol)) != 0) {
                LOG.error("This is not bittorent protocol : " + new String(protocol));
                return null;
            }

            // reading extension bytes
            byte[] extensionBytes = this.readSafe(clntChan, 8);
            if (extensionBytes == null) {
                LOG.error("Could not read extension for handshake");
                return null;
            }
            long extensionBytesLong = Long.parseLong(Util.bytesToHex(extensionBytes));
            // LOG.debug("extensionBytesLong " + extensionBytesLong);


            // reading sha1 hash
            byte[] sha1HashBytes = this.readSafe(clntChan, 20);
            if (sha1HashBytes == null) {
                LOG.error("Could not read sha1 for handshake");
                return null;
            }
            String sha1Hash = Util.bytesToHex(sha1HashBytes);
            // LOG.debug("sha1Hash " + sha1Hash);

            // reading peer_id
            byte[] peerId = this.readSafe(clntChan, 20);
            if (peerId == null) {
                LOG.error("Could not read peerID for handshake");
                return null;
            }
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
                        "Message received too long : " + totalLength + ", surely continuation data");
                return null;
            } else if (totalLength == 0) {
                LOG.error("Message received of length null, surely continuation datat");
                return null;
            }
            else if (totalLength < 0) {
                LOG.error("Message received of length negative, surely continuation data");
                return null;
            }

            // read type
            byte[] typeArray = this.readSafe(clntChan, 1);
            if (typeArray == null) {
                LOG.error("Could not read type of message");
                return null;
            }
            int type = typeArray[0];
            if (type < 0 || type > 8) {
                LOG.error("Message type not in range");
                return null;
            }
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
                    int index = this.readLengthInt(clntChan, 4);
                    if (index < 0) {
                        return null;
                    } else if (index > Torrent.numberOfPieces) {
                        LOG.error("Received have for invalid index : " + index);
                        return null;
                    } else {
                        return new Have(index);
                    }

                case Bitfield.BITFIELD_TYPE:
                    byte[] bitfieldData = this.readSafe(clntChan, totalLength - 1);
                    if (bitfieldData == null) {
                        LOG.error("Received null bitfield data");
                        return null;
                    }
                    return new Bitfield(bitfieldData);
                case Request.REQUEST_TYPE:
                    int requestIndex = this.readLengthInt(clntChan, 4);
                    int requestOffset = this.readLengthInt(clntChan, 4);
                    int lengthPiece = this.readLengthInt(clntChan, 4);
                    if (requestIndex < 0 || requestOffset < 0 || lengthPiece < 0) {
                        LOG.error("Received negative requestIndex " + requestIndex + " or beginOffset " + requestOffset
                                + " or length " + lengthPiece);
                        return null;
                    } else if (requestIndex > Torrent.numberOfPieces) {
                        LOG.error("Received request for invalid index : " + requestIndex);
                        return null;
                    } else {
                        return new Request(requestIndex, requestOffset, lengthPiece);
                    }

                case Piece.PIECE_TYPE:
                    int pieceIndex = this.readLengthInt(clntChan, 4);
                    int beginOffset = this.readLengthInt(clntChan, 4);
                    if (pieceIndex < 0 || beginOffset < 0) {
                        LOG.error("Received negative pieceIndex " + pieceIndex + " or beginOffset " + beginOffset);
                        return null;
                    } else if (pieceIndex > Torrent.numberOfPieces) {
                        LOG.error("Received piece for invalid index : " + pieceIndex);
                        return null;
                    } else {
                        byte[] data = readSafe(clntChan, totalLength - Piece.HEADER_LENGTH);

                        if (data == null) {
                            LOG.error("Received null data for piece : " + pieceIndex);
                            return null;
                        }
                        return new Piece(totalLength, pieceIndex, beginOffset, data);
                    }

                // TODO if implementing endgame
                // case CANCEL.CANCEL_TYPE:
                // return new Cancel();
                default:
                    break;
            }

        }
        LOG.error("Message type not recognized, closing channel");
        return null;
    }
}
