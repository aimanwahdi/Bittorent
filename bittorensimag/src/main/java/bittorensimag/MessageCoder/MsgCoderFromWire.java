package bittorensimag.MessageCoder;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import bittorensimag.Messages.*;
import bittorensimag.Util.Util;

public class MsgCoderFromWire implements MsgCoderDispatcherFromWire {

    public int[] readMessage(DataInputStream in, int length) {
        int[] bytesArray = new int[length];
        for (int i = 0; i < length; i++) {
            try {
                int nextByte = in.readUnsignedByte();
                bytesArray[i] = nextByte;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytesArray;
    }

    @Override
    public Object fromWire(DataInputStream in) throws IOException {
        int firstByte;
        try {
            firstByte = in.readUnsignedByte();
        } catch (EOFException e) {
            System.out.println("No more messages to read ! It is the end or Vuze not opened or not seeding ?");
            return -1;
        }
        return fromWire(firstByte, in);
    }

    @Override
    public Object fromWire(int firstByte, DataInputStream in) throws IOException {
        if (firstByte == Handshake.HANDSHAKE_LENGTH) {
            System.out.println("Received Message : Handshake");

            // reading the protocol name
            // TODO verify it is Bittorent
            this.readMessage(in, 19);

            // reading extension bytes
            this.readMessage(in, 8);

            // reading sha1 hash
            // TODO verify hash
            int[] sha1Hash = this.readMessage(in, 20);

            // reading peer_id
            // TODO verify peer_id ?
            this.readMessage(in, 20);

            return new Handshake(Arrays.toString(sha1Hash));
        } else {
            // read three last bytes of length
            int totalLength = this.readTotalLength(in, firstByte);

            // read type
            int type = in.readUnsignedByte();
            System.out.println("Received Message : " + Msg.messagesNames.get(type));
            switch (type) {
                case Simple.CHOKE:
                    return new Simple(Simple.CHOKE);
                case Simple.UNCHOKE:
                    return new Simple(Simple.UNCHOKE);
                case Simple.INTERESTED:
                    return new Simple(Simple.INTERESTED);
                case Simple.NOTINTERESTED:
                    return new Simple(Simple.NOTINTERESTED);
                // TODO Have for SEEDER
                // case Have.HAVE_TYPE:
                // return new Have(index);
                case Bitfield.BITFIELD_TYPE:
                    byte[] bitfieldData = this.handleBitfield(in, totalLength);
                    return new Bitfield(bitfieldData);
                // TODO Request for SEEDER
                // case Request.REQUEST_TYPE:
                // return new Request(index, beginOffset, pieceLength);
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

    private byte[] handleBitfield(DataInputStream in, int lengthMessage) throws IOException {
        // lengthMessage - mesageType sur 1 byte
        byte[] bitfieldData = new byte[lengthMessage - 1];

        for (int i = 0; i < lengthMessage - 1; i++) {
            bitfieldData[i] = in.readByte();
        }
        return bitfieldData;
    }

    private int readTotalLength(DataInputStream in, int firstByte) throws IOException {
        int secondByte = in.readUnsignedByte();
        int thirdByte = in.readUnsignedByte();
        int fourthByte = in.readUnsignedByte();

        return Integer.parseInt(Util.intToHexStringWith0(firstByte) + Util.intToHexStringWith0(secondByte)
                + Util.intToHexStringWith0(thirdByte) + Util.intToHexStringWith0(fourthByte), 16);
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
                e.printStackTrace();
            }
        }
        return messageBuffer.toByteArray();
    }

    private int readEndPiece(DataInputStream in) throws IOException {
        // read piece index and begin offset of the first piece
        int pieceIndex1 = in.readInt();
        int beginOffset1 = in.readInt();

        // read all the data sent in the first piece
        this.readData(in, Piece.DATA_LENGTH, pieceIndex1);
        System.out.println("pieceIndex1 " + pieceIndex1);

        return pieceIndex1;
    }
}
