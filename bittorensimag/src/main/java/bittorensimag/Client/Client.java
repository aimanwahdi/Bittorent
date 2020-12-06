package bittorensimag.Client;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.MsgCoderFromWire;
import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Messages.*;
import bittorensimag.Torrent.*;
import bittorensimag.Util.ListUtils;

public class Client {
    private static final Logger LOG = Logger.getLogger(Client.class);

    public final static String IP = "127.0.0.1";
    public final static int PORT = 6881;

    private final Torrent torrent;
    private final Tracker tracker;
    private final MsgCoderToWire coderToWire;
    private final MsgCoderFromWire coderFromWire;
    private DataInputStream dataIn;
    private OutputStream out;
    private Socket socket;
    boolean isSeeding;
    private Bitfield bitfieldReceived;
    private ArrayList<Integer> piecesDispo;
    private ArrayList<Integer> piecesToRequest;
    private ArrayList<Integer> piecesDispoToRequest;
    private int nextIndexToRequest;

    private boolean stillReading = true;

    private Output outputFile;

    public Client(Torrent torrent, Tracker tracker, MsgCoderToWire coderToWire, MsgCoderFromWire coderFromWire,
            File destinationFolder) throws IOException {
        this.torrent = torrent;
        this.tracker = tracker;
        this.coderToWire = coderToWire;
        this.coderFromWire = coderFromWire;
        // TODO change when adding multiple peers
        // for now getting first key
        Map.Entry<String, ArrayList<Integer>> firstEntry = tracker.getPeersMap().entrySet().iterator().next();
        // Can have multiple peers on multiple ports for localhost so get first one
        this.createSocket(firstEntry.getKey(), firstEntry.getValue().get(0));
        this.createInputStream();
        this.outputFile = new Output((String) this.torrent.getMetadata().get(Torrent.NAME),
                destinationFolder.getAbsolutePath() + "/");
        this.leecherOrSeeder(destinationFolder);
    }

    private void leecherOrSeeder(File destinationFolder) throws IOException {
        this.isSeeding = false;
        File f = this.outputFile.getFile();
        LOG.debug("Verifying source file : " + f);
        if (f.exists() && f.isFile()) {
            this.outputFile.createFileObjects();
            if (this.torrent.fillBitfield(this.outputFile)) {
                this.isSeeding = true;
                LOG.info("Source file found and correct !");
                LOG.info("SEEDER MODE");
            } else {
                LOG.info("Source file found but incomplete");
                LOG.info("LEECHER MODE");
            }
        } else {
            LOG.info("Source file not found");
            LOG.info("LEECHER MODE");
            this.outputFile.createEmptyFile(f, destinationFolder);
            LOG.debug("Output file with empty data created");
        }
        this.createListsForPieces();
    }

    private void createListsForPieces() {
        ArrayList<Integer> ourPieces = Bitfield.convertBitfieldToList(Bitfield.ourBitfieldData, Torrent.numberOfPieces);
        ArrayList<Integer> allPieces = new ArrayList<>(Torrent.numberOfPieces);
        for (int i = 0; i < Torrent.numberOfPieces; i++) {
            allPieces.add(i);
        }
        this.piecesToRequest = (ArrayList<Integer>) ListUtils.substraction(allPieces, ourPieces);
        LOG.debug("Pieces needed to complete torrent :" + this.piecesToRequest);
    }

    public void startCommunication() throws IOException {
        LOG.debug("Starting communication with the peer");
        Handshake.sendMessage(this.torrent.info_hash, this.out);
        try {
            while (this.receivedMsg(this.dataIn, this.out, this.coderToWire, this.coderFromWire)) {
                ;
            }
        } catch (IOException ioe) {
            LOG.error("Error handling client: " + ioe.getMessage());

        }

    }

    private void createInputStream() {
        InputStream inputStream;
        try {
            LOG.debug("Creation of the InputStream");
            inputStream = this.socket.getInputStream();
            this.dataIn = new DataInputStream(inputStream);
            LOG.debug("InputStream created");
        } catch (IOException e) {
            LOG.fatal("Could not create InputStream");
        }

    }

    private void createSocket(String destAddr, int destPort) {
        try {
            LOG.debug("Creation of the socket for " + destAddr + " and port " + destPort);
            this.socket = new Socket(destAddr, destPort);
            this.out = this.socket.getOutputStream();
            LOG.debug("Socket created");
        } catch (IOException e) {
            LOG.fatal("Could not create Socket");
        }
    }

    private boolean receivedMsg(DataInputStream dataIn, OutputStream out, MsgCoderToWire coderToWire,
            MsgCoderFromWire coderFromWire) throws IOException {

        Object msgReceived = coderFromWire.fromWire(dataIn);

        if (msgReceived instanceof Integer && (int) msgReceived == -1) {
            this.closeConnection(dataIn);
            return false;
        }

        if (msgReceived instanceof Handshake) {
            Handshake handshake = (Handshake) msgReceived;
            return this.handleHandshake(handshake, out);
        }
            
        // cast to message to get type
        Msg msg = (Msg) msgReceived;
        int msgType = msg.getMsgType();

        if (msgType < 0 || msgType > 8) {
            return false;
        }
        LOG.debug("Handling " + Msg.messagesNames.get(msgType) + " message");
        // cast to specific message and doing logic
        switch (msgType) {
            case Simple.CHOKE:
                // Simple choke = (Simple) msgReceived;
                this.closeConnection(dataIn);
                break;
            case Simple.UNCHOKE:
                // Simple unChoke = (Simple) msgReceived;
                // send first request message
                // TODO send next request correponding to dataMap our client already has
            	
            	//send request for the first piece that exist in our seeder
            	/*byte[] bitfieldReceivedData = bitfieldReceived.getBitfieldDATA();
            	int index = 0;
            	outerloop:
            	for(int i = 0; i < Torrent.numberOfPieces; i++) {
            		for(int j=0; j<8; j++) {
            			index++;
            			int valueOfBit = (bitfieldReceivedData[i] >> (7 - j)) & 1; //retrieve the value of bit from highest bit to lowest bit
            			if (valueOfBit == 1){
            				Request.sendMessageForIndex(index, Torrent.numberOfPartPerPiece, out);
            				break outerloop;
            			}
            		}
            	}*/
                //Request.sendMessageForIndex(0, Torrent.numberOfPartPerPiece, out);
                Request.sendMessageForIndex(this.piecesDispoToRequest.get(nextIndexToRequest),
                        Torrent.numberOfPartPerPiece, out);
                break;
            case Simple.INTERESTED:
                // Simple interested = (Simple) msgReceived;
                Simple.sendMessage(Simple.UNCHOKE, out);
                break;
            case Simple.NOTINTERESTED:
                // Simple notInterested = (Simple) msgReceived;
                Simple.sendMessage(Simple.CHOKE, out);
                this.closeConnection(dataIn);
                break;
            case Have.HAVE_TYPE:
                // Have have = (Have) msgReceived;
                // TODO stocker client dans map pour suivre quel client a quelle pièce
                break;
            case Bitfield.BITFIELD_TYPE:
                bitfieldReceived = (Bitfield) msgReceived;
            	/*int bytesNeeded = (int) Math.ceil((double) Torrent.numberOfPieces / 8);
                byte[] bitfieldData = new byte[bytesNeeded];
                Arrays.fill(bitfieldData, (byte) 0x0f);
                bitfieldData[bitfieldData.length - 1] = (byte) 0xf0;
                bitfieldReceived = new Bitfield(bitfieldData);*/
                
                this.piecesDispo = Bitfield.convertBitfieldToList(bitfieldReceived.getBitfieldDATA(),
                        Torrent.numberOfPieces);
                this.piecesDispoToRequest = (ArrayList<Integer>) ListUtils.intersection(this.piecesDispo,
                        this.piecesToRequest);
                LOG.debug("Pieces needed that can be requested " + this.piecesDispoToRequest);
                nextIndexToRequest = 0;
                // TODO send Interested according to bitfield data
                if (!isSeeding) {
                    Simple.sendMessage(Simple.INTERESTED, out);
                }
                break;
            case Request.REQUEST_TYPE:
                Request request = (Request) msgReceived;
                this.handleRequest(request, out);
                break;
            case Piece.PIECE_TYPE:
                Piece piece = (Piece) msgReceived;
                this.handlePieceMsg(dataIn, piece, out);
                break;
            // TODO if implementing endgame
            // case CANCEL.CANCEL_TYPE:

            // break;
            default:
                // never reached test before;
                break;
        }

        return stillReading;
    }

    private boolean handleHandshake(Handshake handshake, OutputStream out) throws IOException {
        LOG.debug("Handling Handshake message");
        if (handshake.getSha1Hash().compareTo(this.torrent.info_hash) != 0) {
            LOG.error("Sha1 hash received different from torrent file");
            return false;
        }
        // TODO store peer_id ?

        // who send handshake first ?
        // Handshake.sendMessage(this.torrent.info_hash, out);

        Bitfield.sendMessage(out);
        return true;
    }

    private void handleRequest(Request request, OutputStream out) throws IOException {
        int pieceIndex = request.getIndex();
        int beginOffset = request.getBeginOffset();
        int pieceLength = request.getPieceLength();
        byte[] pieceData = this.outputFile.getPieceData(pieceIndex);
        byte[] partData = Arrays.copyOfRange(pieceData, beginOffset, beginOffset + pieceLength);

        LOG.debug("Sending pieces for");
        Piece.sendMessage(pieceLength + Piece.HEADER_LENGTH, pieceIndex, beginOffset, partData, out);
    }

    // TODO send new request if fail for a part
    private void handlePieceMsg(DataInputStream dataIn, Piece piece, OutputStream out) throws IOException {
        int pieceIndex = piece.getPieceIndex();
        int beginOffset = piece.getBeginOffset();
        byte[] data = piece.getData();

        LOG.debug("Piece with index " + pieceIndex + " with beginOffset " + beginOffset);

        this.outputFile.writeToFile(pieceIndex * Torrent.pieces_length + beginOffset, data);
        // nextIndexToRequest++;

        if (pieceIndex < Torrent.numberOfPieces - 1) {
            // request only if last part of piece has been received
            if (beginOffset == Torrent.pieces_length - Piece.DATA_LENGTH) {
                if (Piece.testPieceHash(pieceIndex, this.outputFile.getPieceData(pieceIndex))) {
                    Have.sendMessage(pieceIndex, out);
                    Request.sendMessageForIndex(this.piecesDispoToRequest.get(++nextIndexToRequest),
                            Torrent.numberOfPartPerPiece, out);
                    LOG.info(this.piecesDispoToRequest.get(nextIndexToRequest));
                }
                else {
                    // request same piece again
                    Request.sendMessageForIndex(this.piecesDispoToRequest.get(nextIndexToRequest),
                            Torrent.numberOfPartPerPiece, out);
                }
            }
        } else {
            // last piece
            if (beginOffset == Torrent.pieces_length - Piece.DATA_LENGTH) {
                // last part of last piece received
                if (Piece.testPieceHash(pieceIndex, this.outputFile.getPieceData(pieceIndex))) {
                Have.sendMessage(pieceIndex, out);
                this.tracker.generateUrl(Tracker.EVENT_COMPLETED);
                this.tracker.getRequest(Tracker.EVENT_COMPLETED);
                Simple.sendMessage(Simple.NOTINTERESTED, out);
                this.closeConnection(dataIn);
                stillReading = false;
                }
                else {
                    // request same piece again
                    Request.sendMessageForIndex(this.piecesDispoToRequest.get(nextIndexToRequest),
                            Torrent.numberOfPartPerPiece, out);
                }
            }
        }
    }

    private void closeConnection(DataInputStream dataIn) throws IOException {
        dataIn.close();
        this.socket.close();
        LOG.info("Connection closed");
    }

}