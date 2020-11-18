package bittorensimag.Client;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;
import java.util.Iterator;
import java.util.List;

import bittorensimag.MessageCoder.MsgCoderFromWire;
import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Messages.*;
import bittorensimag.Torrent.*;

public class Client {
    private static final Logger LOG = Logger.getLogger(Client.class);

    public final static String IP = "127.0.0.1";
    public final static int PORT = 6881;

    private final Torrent torrent;
    private final MsgCoderToWire coderToWire;
    private final MsgCoderFromWire coderFromWire;
    private DataInputStream dataIn;
    private OutputStream out;
    private Socket socket;
    boolean isSeeding;
    
    private Selector selector;
    private List<SocketChannel> otherClientsChannels ;

    private boolean stillReading = true;

    public Client(Torrent torrent, Tracker tracker, MsgCoderToWire coderToWire, MsgCoderFromWire coderFromWire) throws IOException {
        this.torrent = torrent;
        this.coderToWire = coderToWire;
        this.coderFromWire = coderFromWire;
        this.isSeeding = false;
        this.otherClientsChannels = new ArrayList<SocketChannel>();
        this.createSelector(); //create selector

        Map.Entry<String, ArrayList<Integer>> firstEntry = tracker.getPeersMap().entrySet().iterator().next();
        this.connectToAllClients(firstEntry);

        
//        while (true) {
//        	if (selector.select(300) == 0) { 
//        		//System.out.print(".");
//        		continue;
//        	}
//        	Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
//        	
//        	while (keyIter.hasNext()) {
//        		SelectionKey key = keyIter.next();
//        		if (key.isAcceptable()) {
//        			System.out.println("Acceptable key");
//        		}
//        		keyIter.remove(); 
//        	}
//        }

    }

    // THIS IS FOR SEEDER NOT IMPLEMENTED YET
    public void leecherOrSeeder() {
        File sourceFile = new File(
                this.torrent.torrentFile.getParent() + "/" + this.torrent.getMetadata().get(Torrent.NAME));
        LOG.debug("Verifying source file : " + sourceFile);
        if (sourceFile.exists() && sourceFile.isFile() && this.torrent.compareContent(sourceFile)) {
            this.isSeeding = true;
            LOG.info("Source file found and correct !");
            LOG.info("SEEDER MODE");
        } else {
            LOG.info("Source file not found or incorrect !");
            LOG.info("LEECHER MODE");
        }
    }


    public void startCommunication() {
    	LOG.debug("Starting communication with the peer");

    	for(SocketChannel clntChan : this.otherClientsChannels) {
            Handshake.sendMessage(this.torrent.info_hash, clntChan);
    	}
    	
    	while (true) { // Run forever, processing available I/O operations
    		// Wait for some channel to be ready (or timeout)
    		
    		try {
        		if (selector.select(300) == 0) { // returns # of ready chans
        			continue;
        		}
    		}catch (IOException ioe) {
                LOG.error("Error handling client: " + ioe.getMessage());

            }

    		
        	System.out.println("available I/O operations found");
        	Iterator<SelectionKey> keyIter = this.selector.selectedKeys().iterator();

        	System.out.println(this.selector.selectedKeys().size());
        	
        	while (keyIter.hasNext()) {
        		SelectionKey key = keyIter.next();
        		if (key.isReadable()) {
        			System.out.println("Readable key");
        		}
        		if (key.isWritable()) {
        			System.out.println("Writable key");
                    try {
                        while (this.receivedMsg(this.coderToWire, this.coderFromWire ,key)) {
                            ;
                        }
                    } catch (IOException ioe) {
                        LOG.error("Error handling client: " + ioe.getMessage());

                    }
        		}
        		System.out.println("key " + key);
        		keyIter.remove(); 
        	}
        	
    	}
    	



    }


    
    private void createSelector() throws IOException {
        try {
        	this.selector = Selector.open();
        } catch (IOException e) {
    		e.printStackTrace();  
        }
    }
    
    private void createSocket(String destAddr, int destPort) {
		LOG.debug("Creration of the socket for " + destAddr + " and port " + destPort);

		try {
        	SocketChannel clntChan = SocketChannel.open();
        	if (!clntChan.connect(new InetSocketAddress(destAddr, destPort))) {
        		while (!clntChan.finishConnect()) {
        			System.out.print("Waiting for connection to finish"); 
        		}
        	}
        	clntChan.configureBlocking(false); // must be nonblocking to register
        	
        	System.out.println("socket created " + destAddr + " " + destPort );
        	
        	// Register selector with channel for both reading and writing. The returned key is ignored
        	SelectionKey key = clntChan.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        	LOG.debug("Socket created");
        	
        	System.out.println("keys size " + this.selector.keys().size());
        	
        	otherClientsChannels.add(clntChan);

    	} catch (IOException e) {
    		LOG.fatal("Could not create Socket");
    		e.printStackTrace();
    	}
    }
    
    private void connectToAllClients(Map.Entry<String, ArrayList<Integer>> firstEntry) {
        ArrayList<Integer> portNumbers = firstEntry.getValue();
        for(int port : portNumbers ) {
        	System.out.println("port : "+ port);
        	this.createSocket(firstEntry.getKey(), port);
        }
    }

    private boolean receivedMsg( MsgCoderToWire coderToWire,
            MsgCoderFromWire coderFromWire, SelectionKey key) throws IOException {

    	ByteBuffer clntBuf  = ByteBuffer.allocate(200);
    	key.attach(clntBuf);
    	
    	System.out.println("client buffer "+ clntBuf);
    	
		
        Object msgReceived = coderFromWire.fromWire(key);

    	
        if (msgReceived instanceof Handshake) {
        	System.out.println("received handshake from key "+ key);
            Handshake handshake = (Handshake) msgReceived;
            return this.handleHandshake(handshake);
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
                Simple choke = (Simple) msgReceived;
                //TODO correct this 
//                this.closeConnection(in);
                break;
            case Simple.UNCHOKE:
                // Simple unChoke = (Simple) msgReceived;
                // send first request message
                // TODO send next request correponding to dataMap our client already has
                Request.sendMessageForIndex(0, Torrent.numberOfPartPerPiece, out);
                break;
            case Simple.INTERESTED:
                // Simple interested = (Simple) msgReceived;
                Simple.sendMessage(Simple.UNCHOKE, out);
                break;
            case Simple.NOTINTERESTED:
                // Simple notInterested = (Simple) msgReceived;
                Simple.sendMessage(Simple.CHOKE, out);
                //TODO correct this 
//                this.closeConnection(in);
                break;
            case Have.HAVE_TYPE:
                // Have have = (Have) msgReceived;
                // TODO stocker client dans map pour suivre quel client a quelle pièce
                break;
            case Bitfield.BITFIELD_TYPE:
                // Bitfield bitfield = (Bitfield) msgReceived;
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
                //TODO correct this 
//                this.handlePieceMsg(in, piece, out);
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

    private boolean handleHandshake(Handshake handshake) throws IOException {
        LOG.debug("Handling Handshake message");
        if (handshake.getSha1Hash().compareTo(this.torrent.info_hash) != 0) {
            LOG.error("Sha1 hash received different from torrent file");
            return false;
        }
        // TODO store peer_id ?

        // who send handshake first ?
        // Handshake.sendMessage(this.torrent.info_hash, out);

        int bytesNeeded = (int) Math.ceil((double) Torrent.numberOfPieces / 8);
        byte[] bitfieldData = new byte[bytesNeeded];

        // TODO for resume implement according to dataMap => which pieces are missing ?
        if (isSeeding) {
            Arrays.fill(bitfieldData, (byte) 0xff);
            bitfieldData[bitfieldData.length - 1] = (byte) 0xf0;
            Bitfield.sendMessage(bitfieldData, out);
        } else {
            if (Torrent.dataMap.isEmpty()) {
                Arrays.fill(bitfieldData, (byte) 0x00);
                Bitfield.sendMessage(bitfieldData, out);
            }
        }
        return true;
    }

    private void handleRequest(Request request, OutputStream out) throws IOException {
        int pieceIndex = request.getIndex();
        int beginOffset = request.getBeginOffset();
        int pieceLength = request.getPieceLength();
        byte[] pieceData = Torrent.dataMap.get(pieceIndex);
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

        Piece.addToMap(pieceIndex, data);

        if (pieceIndex < Torrent.numberOfPieces - 1) {
            // request only if last part of piece has been received
            if (beginOffset == Torrent.pieces_length - Piece.DATA_LENGTH) {
                if (Piece.testPieceHash(pieceIndex, Torrent.dataMap.get(pieceIndex))) {
                    Have.sendMessage(pieceIndex, out);
                    Request.sendMessageForIndex(++pieceIndex, Torrent.numberOfPartPerPiece, out);
                }
                else {
                    // request same piece again
                    Request.sendMessageForIndex(pieceIndex, Torrent.numberOfPartPerPiece, out);
                }
            }
        } else {
            // last piece
            if (beginOffset == Torrent.pieces_length - Piece.DATA_LENGTH) {
                // last part of last piece received
                if (Piece.testPieceHash(pieceIndex, Torrent.dataMap.get(pieceIndex))) {
                Have.sendMessage(pieceIndex, out);
                Simple.sendMessage(Simple.NOTINTERESTED, out);
                this.closeConnection(dataIn);
                stillReading = false;
                }
                else {
                    // request same piece again
                    Request.sendMessageForIndex(pieceIndex, Torrent.numberOfPartPerPiece, out);
                }
            }
        }
    }

    //TODO change this for nio sockets 
    private void closeConnection(DataInputStream dataIn) throws IOException {
        dataIn.close();
        this.socket.close();
        LOG.info("Connection closed");
    }

}