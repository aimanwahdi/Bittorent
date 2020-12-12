package bittorensimag.Client;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
import bittorensimag.Util.PieceManager;

import bittorensimag.ProgressBar.*;

public class Client {
    private static final Logger LOG = Logger.getLogger(Client.class);

    public final static String IP = "127.0.0.1";
    public final static int PORT = 6881;

    private final int KB = 1024;
    private final int MB = 1024 * 1024;
    private final int GB = 1024 * 1024 * 1024;

    private final Torrent torrent;
    private final Tracker tracker;
    private final MsgCoderToWire coderToWire;
    private final MsgCoderFromWire coderFromWire;;
    boolean isSeeding;
    private Selector selector;
    private List<SocketChannel> otherClientsChannels;

    private Bitfield bitfieldReceived;
    private ArrayList<Integer> piecesDispo;

    private Output outputFile;

    private PieceManager pieceManager;

    private boolean piecesMissing = true;

    public Client(Torrent torrent, Tracker tracker, MsgCoderToWire coderToWire, MsgCoderFromWire coderFromWire,
            File destinationFolder) throws IOException {

        this.torrent = torrent;
        this.tracker = tracker;
        this.coderToWire = coderToWire;
        this.coderFromWire = coderFromWire;

        this.outputFile = new Output((String) this.torrent.getMetadata().get(Torrent.NAME),
                destinationFolder.getAbsolutePath() + "/");
        this.pieceManager = new PieceManager(Torrent.numberOfPieces);

        this.leecherOrSeeder(destinationFolder);

        this.otherClientsChannels = new ArrayList<SocketChannel>();
        this.createSelector(); // create selector

        Map.Entry<String, ArrayList<Integer>> firstEntry = this.tracker.getPeersMap().entrySet().iterator().next();
        this.connectToAllClients(firstEntry);
    }

    private void leecherOrSeeder(File destinationFolder) throws IOException {
        this.isSeeding = false;
        File f = this.outputFile.getFile();
        LOG.debug("Verifying source file : " + f);
        if (f.exists() && f.isFile()) {
            this.outputFile.createFileObjects();
            if (this.torrent.fillBitfield(this.outputFile, this.pieceManager)) {
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
    }

    public void startProgress() throws IOException {

        if (LOG.isInfoEnabled()) {
            this.startProgressBars();
        } else {
            this.startCommunication(null, null, null);
        }

    }

    private void startProgressBars() {
        int unitSize = 1;
        String unitName = "";
        if (Torrent.totalSize < 1 * MB) {
            unitSize = KB;
            unitName = "KB";
        } else if (Torrent.totalSize < 1 * GB) {
            unitSize = MB;
            unitName = "MB";
        } else {
            unitSize = GB;
            unitName = "GB";
        }

        // TODO make it depend on Bitfield not totalsize for resume
        try (ProgressBar pbTorrent = new ProgressBarBuilder().setTaskName(this.outputFile.getName())
                .setInitialMax(Torrent.totalSize).setUnit(unitName, unitSize).build();
                ProgressBar pbCPU = new ProgressBarBuilder().setTaskName("CPU").setInitialMax(100)
                        .setStyle(ProgressBarStyle.UNICODE_BLOCK).build();
                ProgressBar pbMemory = new ProgressBarBuilder().setTaskName("MEMORY")
                        .setInitialMax((long) StatGetter.getTotalMemory()).setStyle(ProgressBarStyle.UNICODE_BLOCK)
                        .build()) {

            // Use ProgressBar("Test", 100, ProgressBarStyle.ASCII) if you want ASCII output

            // Set extra message to display at the end of the bar
            if (isSeeding) {
                pbTorrent.setExtraMessage("Sending...");
            } else {
                pbTorrent.setExtraMessage("Receiving...");
            }
            // Mesure usage at the beggining
            this.mesureUsage(pbCPU, pbMemory);
            this.startCommunication(pbTorrent, pbCPU, pbMemory);
        }

    } // progress bar stops automatically after completion of try-with-resource block

    private void startCommunication(ProgressBar pbTorrent, ProgressBar pbCPU, ProgressBar pbMemory) {
        LOG.debug("Starting communication with the peer");
        // send handshakes to all clients
        for (SocketChannel clntChan : this.otherClientsChannels) {
            Handshake.sendMessage(this.torrent.info_hash, clntChan);
        }
        while (piecesMissing) { // Run while reading processing available I/O operations
            // Wait for some channel to be ready (or timeout)

            try {
                if (selector.select(300) == 0) { // returns # of ready chans
                    LOG.debug("No Channel ready");
                    // TODO Experimental we enter this only in the end to stop while
                    piecesMissing = false;
                    continue;
                }
            } catch (IOException ioe) {
                LOG.error("Error handling client: " + ioe.getMessage());

            }

            // LOG.info("Available I/O operations found");

            // Get iterator on set of keys with I/O to process
            Iterator<SelectionKey> keyIter = this.selector.selectedKeys().iterator();
            // LOG.debug("Number of keys available " + this.selector.selectedKeys().size());

            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();
                if (key.isReadable()) {
                    LOG.debug("Readable key");
                    // Client socket channel is available for writing
                    if (key.isWritable()) {
                        LOG.debug("Writable key");
                        try {
                            if (!this.receivedMsg(this.coderToWire, this.coderFromWire, key, pbTorrent, pbCPU,
                                    pbMemory)) {
                                this.closeConnection((SocketChannel) key.channel());
                            }
                        } catch (IOException ioe) {
                            LOG.error("Error handling client: " + ioe.getMessage());
                        }
                    }
                }
                // LOG.debug("Key " + key + " have been treated");

                // remove from set of selected keys
                keyIter.remove();
            }
        }
    }

    private void createSelector() throws IOException {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            LOG.error("Error creating selector: " + e.getMessage());
        }
    }

    private void createSocket(String destAddr, int destPort) {
        LOG.debug("Creration of the socket for " + destAddr + " and port " + destPort);

        // Create listening socket channel for each port and register selector
        try {
            SocketChannel clntChan = SocketChannel.open();
            // bind socket with port
            if (!clntChan.connect(new InetSocketAddress(destAddr, destPort))) {
                while (!clntChan.finishConnect()) {
                    LOG.debug("Waiting for connection to finish");
                }
            }
            clntChan.configureBlocking(false); // must be nonblocking to register

            LOG.debug("socket created " + destAddr + " " + destPort);

            // Register selector with channel for both reading and writing. The returned key
            // is ignored
            SelectionKey key = clntChan.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            otherClientsChannels.add(clntChan);

        } catch (IOException e) {
            LOG.fatal("Could not create Socket");
        }
    }

    private void connectToAllClients(Map.Entry<String, ArrayList<Integer>> firstEntry) {
        ArrayList<Integer> portNumbers = firstEntry.getValue();
        for (int port : portNumbers) {
            LOG.debug("port : " + port);
            this.createSocket(firstEntry.getKey(), port);
        }
    }

    private boolean receivedMsg(MsgCoderToWire coderToWire, MsgCoderFromWire coderFromWire, SelectionKey key,
            ProgressBar pbTorrent, ProgressBar pbCPU, ProgressBar pbMemory)
            throws IOException {

        SocketChannel clntChan = (SocketChannel) key.channel();

        Object msgReceived = coderFromWire.fromWire(key);

        if (msgReceived == null) {
            LOG.error("Message received is null, closing channel");
            return false;
        }

        if (msgReceived instanceof Handshake) {
            LOG.debug("received handshake from key " + key);
            Handshake handshake = (Handshake) msgReceived;
            return this.handleHandshake(handshake, key);
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
                break;
            case Simple.UNCHOKE:
                // get the next requested piece and send the request message for it
                int nextPiece = this.pieceManager.nextPieceToRequest(clntChan.socket());
                LOG.debug("Next piece to be requested " + nextPiece);
                if (nextPiece != -1) {
                    // TODO refactor request sending to support only last piece missing
                    Request.sendMessageForIndex(nextPiece, Torrent.numberOfPartPerPiece, clntChan);
                    LOG.debug("Request message sent for " + nextPiece + " to client " + clntChan);
                    // set this piece as requested
                    this.pieceManager.requestSent(nextPiece);
                } else if (!this.pieceManager.getDownloaded().contains(false)) {
                    this.piecesMissing = false;
                }
                break;
            case Simple.INTERESTED:
                Simple interested = (Simple) msgReceived;
                Simple.sendMessage(Simple.UNCHOKE, clntChan);
                LOG.debug("Send unchoke message to " + clntChan);
                break;
            case Simple.NOTINTERESTED:
                Simple notInterested = (Simple) msgReceived;
                Simple.sendMessage(Simple.CHOKE, clntChan);
                LOG.debug("Send choke message to " + clntChan);
                break;
            case Have.HAVE_TYPE:
                Have have = (Have) msgReceived;
                LOG.debug("Have received for index " + have.getIndex() + " from client " + clntChan);
                // TODO stocker have client dans map pour suivre quel client a quelle pièce
                break;
            case Bitfield.BITFIELD_TYPE:
                bitfieldReceived = (Bitfield) msgReceived;

                // Override almost full bitfield with full one (lazy bitfield)
                if (bitfieldReceived.getBitfieldDATA() == Bitfield.fakeFullBitfield) {
                    LOG.debug("Received full lazy bitfield, override to full");
                    bitfieldReceived.setBitfieldDATA(Bitfield.fullBitfield);
                }

                // create the list of available pieces for this client based on its bietfield
                piecesDispo = Bitfield.convertBitfieldToList(bitfieldReceived.getBitfieldDATA(),
                        Torrent.numberOfPieces);

                // add the client represented by its socket and all its pieces to PieceMap
                for (int pieceIndex : piecesDispo) {
                    if (!this.pieceManager.getPieceMap().containsKey(pieceIndex)) {
                        ArrayList<Socket> peers = new ArrayList<Socket>();
                        peers.add(clntChan.socket());
                        this.pieceManager.getPieceMap().put(pieceIndex, peers);
                    } else {
                        this.pieceManager.getPieceMap().get(pieceIndex).add(clntChan.socket());
                    }
                }

                // Print the peers with their available pieces
                // for (int name: this.pieceManager.getPieceMap().keySet()){
                // String value = this.pieceManager.getPieceMap().get(name).toString();
                // LOG.debug(name + " " + value);
                // }

                if (!isSeeding) {
                    Simple.sendMessage(Simple.INTERESTED, clntChan);
                    LOG.debug("Send interested message to " + clntChan);
                }
                break;
            case Request.REQUEST_TYPE:
                Request request = (Request) msgReceived;
                LOG.debug("Request message received for index " + request.getIndex() + " from client " + clntChan);
                this.handleRequest(request, clntChan);
                break;
            case Piece.PIECE_TYPE:
                Piece piece = (Piece) msgReceived;
                LOG.debug("Piece message received for index " + piece.getPieceIndex() + " from client " + clntChan);
                this.handlePieceMsg(piece, clntChan, pbTorrent);
                if (LOG.isInfoEnabled()) {
                    this.mesureUsage(pbCPU, pbMemory);
                }
                break;
            // TODO if implementing endgame
            // case Cancel.CANCEL_TYPE:
            // Cancel cancel = (Cancel) msgReceived;
            // this.handlePieceMsg(piece, clntChan);
            // break;
            default:
                // never reached test before;
                break;
        }

        return piecesMissing;
    }

    private void mesureUsage(ProgressBar pbCPU, ProgressBar pbMemory) {
        pbCPU.stepTo((long) StatGetter.getLoadAverage());
        pbMemory.stepTo((long) StatGetter.getUsedMemory());
    }

    private boolean handleHandshake(Handshake handshake, SelectionKey key) throws IOException {
        LOG.debug("Handling Handshake message");
        if (handshake.getSha1Hash().compareTo(this.torrent.info_hash) != 0) {
            LOG.error("Sha1 hash received different from torrent file");
            return false;
        }
        // who send handshake first ?
        // Handshake.sendMessage(this.torrent.info_hash, out);

        Bitfield.sendMessage(Bitfield.ourBitfieldData, key);
        return true;
    }

    private void handleRequest(Request request, SocketChannel clntChan) throws IOException {
        int pieceIndex = request.getIndex();
        int beginOffset = request.getBeginOffset();
        int pieceLength = request.getPieceLength();
        byte[] pieceData = this.outputFile.getPieceData(pieceIndex);
        byte[] partData = Arrays.copyOfRange(pieceData, beginOffset, beginOffset + pieceLength);

        LOG.debug("Sending pieces for" + clntChan);
        Piece.sendMessage(pieceLength + Piece.HEADER_LENGTH, pieceIndex, beginOffset, partData, clntChan);
    }

    private void handlePieceMsg(Piece piece, SocketChannel clntChan, ProgressBar pbTorrent) throws IOException {
        int pieceIndex = piece.getPieceIndex();
        int beginOffset = piece.getBeginOffset();
        byte[] data = piece.getData();

        LOG.debug("Piece with index " + pieceIndex + " with beginOffset " + beginOffset);

        this.outputFile.writeToFile(pieceIndex * Torrent.pieces_length + beginOffset, data);

        if (pieceIndex < Torrent.numberOfPieces - 1) {
            // request only if last part of piece has been received
            if (beginOffset == Torrent.pieces_length - Piece.DATA_LENGTH) {
                if (Piece.testPieceHash(pieceIndex, this.outputFile.getPieceData(pieceIndex))) {
                    int nextPiece = this.pieceManager.nextPieceToRequest(clntChan.socket());
                    Have.sendMessage(pieceIndex, clntChan);
                    // set this piece downloaded
                    this.pieceManager.pieceDownloaded(pieceIndex);
                    if (LOG.isInfoEnabled()) {
                        pbTorrent.stepBy(Torrent.pieces_length);
                    }
                    // search for next piece to be requested
                    LOG.debug("nextPiece to be requested " + nextPiece);
                    // only request if we still lack pieces
                    if (nextPiece != -1) {
                        Request.sendMessageForIndex(nextPiece, Torrent.numberOfPartPerPiece, clntChan);
                        LOG.debug("Request message sent for " + nextPiece + " to client " + clntChan);
                        this.pieceManager.requestSent(nextPiece);
                    }
                    else if (!this.pieceManager.getDownloaded().contains(false)) {
                        this.piecesMissing = false;
                    }

                } else {
                    Request.sendMessageForIndex(pieceIndex, Torrent.numberOfPartPerPiece, clntChan);
                }
            }
        } else {
            // last piece
            if (beginOffset == Torrent.lastPieceLength - Torrent.lastPartLength) {
                // last part of last piece received
                if (Piece.testPieceHash(pieceIndex, this.outputFile.getPieceData(pieceIndex))) {
                    int nextPiece = this.pieceManager.nextPieceToRequest(clntChan.socket());
                    this.pieceManager.pieceDownloaded(pieceIndex);
                    Have.sendMessage(pieceIndex, clntChan);
                    if (LOG.isInfoEnabled()) {
                        pbTorrent.stepBy(Torrent.lastPieceLength);
                    }
                    // search for next piece to be requested
                    LOG.debug("nextPiece to be requested " + nextPiece);
                    // only request if we still lack pieces
                    if (nextPiece != -1) {
                        Request.sendMessageForIndex(nextPiece, Torrent.numberOfPartPerPiece, clntChan);
                        LOG.debug("Request message sent for " + nextPiece + " to client " + clntChan);
                        this.pieceManager.requestSent(nextPiece);
                    } else if (!this.pieceManager.getDownloaded().contains(false)) {
                        this.piecesMissing = false;
                    }
                } else {
                    Request.sendMessageForIndex(pieceIndex, Torrent.numberOfPartPerPiece, clntChan);
                }
            }
        }
    }

    private void closeConnection(SocketChannel clntChan) throws IOException {
        // end communication with all clients
        if (!isSeeding) {
            Simple.sendMessage(Simple.NOTINTERESTED, clntChan);
        }
        LOG.info("Closing connection with channel" + clntChan);
        clntChan.close();
    }

}