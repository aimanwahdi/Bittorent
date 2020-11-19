package bittorensimag.MessageCoder;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import bittorensimag.Messages.*;
import bittorensimag.Util.Util;

public class MsgCoderFromWire implements MsgCoderDispatcherFromWire {
    private static final Logger LOG = Logger.getLogger(MsgCoderFromWire.class);

    public byte[] readLength(ByteBuffer readBuf, int length) {
        byte[] bytesArray = new byte[length];
        readBuf.get(bytesArray, 0 , length);
        return bytesArray;
    }

    public int readLengthInt(ByteBuffer readBuf, int length) {
        String lengthString = "";
        
        for (int i = 0; i < length; i++) {
                lengthString += Util.intToHexStringWith0(readBuf.get());
                LOG.error("Error while reading ints for length " + length);
        }
        return Integer.parseInt(lengthString, 16);
    }

    @Override
    public Object fromWire(SelectionKey key ) throws IOException {
    	ByteBuffer readBuf = (ByteBuffer) key.attachment();
    	
    	System.out.println("created buffer "+ readBuf);
    	
    	SocketChannel clntChan = (SocketChannel) key.channel();

    	System.out.println("created channel "+ clntChan);

    	
    	int bytesRcvd;
    	int totalBytesRcvd = 0;
    	
    	while ((bytesRcvd = clntChan.read(readBuf)) > 0 || totalBytesRcvd < 3) {
//    		if ((bytesRcvd = clntChan.read(readBuf)) == -1) {
//    			System.out.println("Connection closed prematurely");
//    		}
        	totalBytesRcvd += bytesRcvd;
    	}
    	System.out.println("received bytes : " + totalBytesRcvd);
    	
        return fromWire(readBuf,clntChan);
    }

    @Override
    public Object fromWire(ByteBuffer readBuf,SocketChannel clntChan) throws IOException {
    	//set position to zero 
    	readBuf.rewind();
    	byte firstByte = readBuf.get();
    	System.out.println("firstByte : " +firstByte);
    	System.out.println("client buffer "+ readBuf);

        if (firstByte == Handshake.HANDSHAKE_LENGTH) {
            LOG.debug("Received Message : Handshake");

            System.out.println("Received Message : Handshake");
            // reading the protocol name
            byte[] protocol = this.readLength(readBuf, 19);

            System.out.println("protocol name : " + new String(protocol));
            if (Handshake.protocolName.compareTo(new String(protocol)) != 0) {
                LOG.error("This is not bittorent protocol");
                return null;
            }

            // reading extension bytes
            byte[] extensionBytes = this.readLength(readBuf, 8);
            long extensionBytesLong = Long.parseLong(Util.bytesToHex(extensionBytes));
            
            System.out.println("extensionBytesLong " + extensionBytesLong);

            // reading sha1 hash
            byte[] sha1HashBytes = this.readLength(readBuf, 20);
            String sha1Hash = Util.bytesToHex(sha1HashBytes);
            
            System.out.println("sha1Hash " + sha1Hash);


            // reading peer_id
            byte[] peerId = this.readLength(readBuf, 20);
            
            System.out.println("peerId " + peerId);

            
//            return new Object();

            return new Handshake(sha1Hash, peerId, extensionBytesLong);
        } 
            else {
            // read three last bytes of length
            int totalLength = this.readTotalLength(readBuf ,firstByte);
            System.out.println("totalLength " + totalLength);

            // read type
            int type = readBuf.get();
            System.out.println("type " + type);

            
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
                    int index = this.readLengthInt(readBuf, totalLength - 1);
                    return new Have(index);
                case Bitfield.BITFIELD_TYPE:
                    byte[] bitfieldData = this.readLength(readBuf, totalLength - 1);
                    return new Bitfield(bitfieldData);
                case Request.REQUEST_TYPE:
                    int requestIndex = readBuf.getInt();
                    int requestOffset = readBuf.getInt();
                    int lengthPiece = this.readLengthInt(readBuf, 4);
                    return new Request(requestIndex, requestOffset, lengthPiece);
                case Piece.PIECE_TYPE:
                    int pieceIndex = readBuf.getInt();
                    int beginOffset = readBuf.getInt();
                    byte[] data = readData(readBuf, totalLength);
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

    private int readTotalLength(ByteBuffer readBuf, int firstByte) throws IOException {
        
        String firstByteString = Util.intToHexStringWith0(firstByte);
        String secondByteString = Util.intToHexStringWith0(readBuf.get());
        String thirdByteString = Util.intToHexStringWith0(readBuf.get());
        String fourthByteString = Util.intToHexStringWith0(readBuf.get());

        return Integer.parseInt(firstByteString + secondByteString + thirdByteString + fourthByteString, 16);
    }

    /*
     * read all the data of a piece and put it the map
     */
    private byte[] readData(ByteBuffer readBuf, int lengthMessage) {
//        ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();

        int pieceDataLength = lengthMessage - Piece.HEADER_LENGTH;

//        for (int i = 0; i < pieceDataLength; i++) {
//                int nextByte = readBuf.get();
//                messageBuffer.write(nextByte);
//        }
        byte[] bytesArray = new byte[pieceDataLength];

        readBuf.get(bytesArray, 0, pieceDataLength);
        return bytesArray ;
        
//        return messageBuffer.toByteArray();
    }
}
