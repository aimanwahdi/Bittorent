package bittorensimag.Client;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.util.Iterator;
import java.util.List;

import bittorensimag.MessageCoder.MsgCoderFromWire;
import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Messages.*;
import bittorensimag.Torrent.*;
import bittorensimag.Util.MapUtil;
import bittorensimag.Util.PieceManager;

import bittorensimag.ProgressBar.*;

public class Client {
    private static final Logger LOG = Logger.getLogger(Client.class);

    public final static String IP = "127.0.0.1";
    public final static int PORT = 6881;

    private final int KB = 1024;
    private final int MB = 1024 * 1024;
    private final int GB = 1024 * 1024 * 1024;

    private int unitSize = 1;
    private String unitName = "";

    private final Torrent torrent;
    private final Tracker tracker;
    private final MsgCoderToWire coderToWire;
    private final MsgCoderFromWire coderFromWire;;
    boolean isSeeding;
    private Selector selector;
    private List<SocketChannel> peersNotConnected;
    private List<SocketChannel> peersConnected;
    private List<Integer> portsConnected;

    private Bitfield bitfieldReceived;
    private ArrayList<Integer> piecesDispo;

    private HashMap<SocketChannel, String> socketToPeerIdMap = new HashMap<SocketChannel, String>();
    private ArrayList<SocketChannel> handshakeSent = new ArrayList<SocketChannel>();

    private Output outputFile;

    private PieceManager pieceManager;

    private boolean piecesMissing = true;

    private ProgressBarBuilder progressBarBuilder;

    private long startTime;

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

        this.peersNotConnected = new ArrayList<SocketChannel>();
        this.peersConnected = new ArrayList<SocketChannel>();
        this.portsConnected = new ArrayList<Integer>();
        this.createSelector(); // create selector
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
            LOG.info("Creating empty file...");
            this.outputFile.createEmptyFile(f, destinationFolder);
            LOG.debug("Output file with empty data created");
            this.pieceManager.initNeededPiecesList(Torrent.numberOfPieces);
        }
    }

    public void startProgress() throws IOException {
        // StatGetter.clearScreen();
        if (Logger.getRootLogger().getLevel() == Level.INFO) {
            this.startProgressBars();
        } else {
            this.startCommunication(null, null, null);
        }

    }

    private void startProgressBars() {

        try (ProgressBarArray torrentProgressBars = this.createTorrentProgress();
                ProgressBar pbCPU = new ProgressBarBuilder().setTaskName("CPU").setInitialMax(100)
                        .setStyle(ProgressBarStyle.UNICODE_BLOCK).build();
                ProgressBar pbMemory = new ProgressBarBuilder().setTaskName("MEMORY")
                        .setInitialMax((long) StatGetter.getTotalMemory()).setStyle(ProgressBarStyle.UNICODE_BLOCK)
                        .setUnit("MB", 1).build()) {

            // Use ProgressBar("Test", 100, ProgressBarStyle.ASCII) if you want ASCII output

            // Set extra message to display at the end of the bar
            if (isSeeding) {
                torrentProgressBars.getByName(this.outputFile.getName()).setExtraMessage("Seeding...");
            } else {
                torrentProgressBars.getByName(this.outputFile.getName()).setExtraMessage("Waiting...");
            }

            ArrayList<Integer> ourPieces = Bitfield.convertBitfieldToList(Bitfield.ourBitfieldData,
                    Torrent.numberOfPieces);

            for (Integer pieceIndex : ourPieces) {
                this.increaseOurProgress(pieceIndex, this.outputFile.getName(), torrentProgressBars);
            }

            // Mesure usage at the beggining
            this.mesureUsage(pbCPU, pbMemory);
            this.startCommunication(torrentProgressBars, pbCPU, pbMemory);
        }

    } // progress bar stops automatically after completion of try-with-resource block

    private ProgressBarArray createTorrentProgress() {

        if (Torrent.totalSize < MB) {
            this.unitSize = KB;
            this.unitName = "KB";
        } else if (Torrent.totalSize < GB) {
            this.unitSize = MB;
            this.unitName = "MB";
        } else {
            this.unitSize = GB;
            this.unitName = "GB";
        }

        ArrayList<String> taskNamesArray = new ArrayList<String>();

        taskNamesArray.add(this.outputFile.getName());

        this.progressBarBuilder = new ProgressBarBuilder().setInitialMax(Torrent.totalSize).setUnit(this.unitName,
                this.unitSize);

        ProgressBarArray pbArray = new ProgressBarArrayBuilder().setTaskName(taskNamesArray)
                .setInitialMax(Torrent.totalSize).setUnit(this.unitName, this.unitSize).build();
        return pbArray;
    }

    private void startCommunication(ProgressBarArray torrentProgressBars, ProgressBar pbCPU, ProgressBar pbMemory) {
        this.connectToAllClients(torrentProgressBars);
        try {
            // // Instead of creating a ServerSocket, create a ServerSocketChannel
            // ServerSocketChannel ssc = ServerSocketChannel.open();

            // // Set it to non-blocking, so we can use select
            // ssc.configureBlocking(false);

            // // Get the Socket connected to this channel, and bind it to the
            // // listening port
            // ServerSocket ss = ssc.socket();
            // InetSocketAddress isa = new InetSocketAddress(PORT);
            // ss.bind(isa);

            // // Register the ServerSocketChannel, so we can listen for incoming
            // // connections
            // ssc.register(selector, SelectionKey.OP_ACCEPT);
            // LOG.debug("Listening on port " + PORT + " for incoming connections");

            this.startTime = System.currentTimeMillis(); // fetch starting time

            while (true) {
                if ((System.currentTimeMillis() - startTime) >= 5000) {
                    this.printPorts();
                    this.fetchTracker(torrentProgressBars);
                }

                // Run while reading processing available I/O operations
                // Wait for some channel to be ready (or timeout)

                if (selector.select(300) == 0) { // returns # of ready chans
                    // LOG.debug("No Channel ready");
                    continue;
                }

                // LOG.info("Available I/O operations found");

                // Get iterator on set of keys with I/O to process
                Iterator<SelectionKey> keyIter = this.selector.selectedKeys().iterator();
                // LOG.debug("Number of keys available " + this.selector.selectedKeys().size());

                while (keyIter.hasNext()) {
                    SelectionKey key = keyIter.next();
                    // if we receive a new connnection from a new client
                    // if (key.isAcceptable()) {
                    // // It's an incoming connection. Register this socket with
                    // // the Selector so we can listen for input on it
                    // SocketChannel clntChan = ss.accept().getChannel();

                    // LOG.info("Received a new connection from " + clntChan);

                    // clntChan.configureBlocking(false);

                    // // Register it with the selector, for reading and writing
                    // clntChan.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    // peersNotConnected.add(clntChan);
                    // Handshake.sendMessage(this.torrent.info_hash, clntChan);
                    // this.handshakeSent.add(clntChan);
                    // if (Logger.getRootLogger().getLevel() == Level.INFO) {
                    // torrentProgressBars.add(this.progressBarBuilder,
                    // clntChan.socket().getRemoteSocketAddress().toString());
                    // }
                    // }
                    if (key.isReadable()) {
                        // LOG.debug("Readable key");
                        // Client socket channel is available for writing
                        if (key.isWritable()) {
                            // LOG.debug("Writable key");
                            try {
                                SocketChannel clntChan = (SocketChannel) key.channel();
                                if (!this.receivedMsg(this.coderToWire, this.coderFromWire, clntChan,
                                        torrentProgressBars, pbCPU, pbMemory)) {
                                    this.closeConnection((SocketChannel) key.channel(), torrentProgressBars);
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
        } catch (IOException ie) {
            LOG.error("Error during try loop : " + ie.getMessage());
        }
    }

    private void printPorts() {
        LOG.warn(MapUtil.ArrayListToString((ArrayList<Integer>) this.portsConnected));
    }

    private void fetchTracker(ProgressBarArray torrentProgressBars) throws IOException {
        LOG.debug("Generating GET Request for tracker");
        this.tracker.generateUrl(Tracker.EVENT_STARTED);
        LOG.debug("Successfully generated GET Request");
        this.tracker.getRequest(Tracker.EVENT_STARTED);
        this.connectToAllClients(torrentProgressBars);
        this.startTime = System.currentTimeMillis(); // reset starting time
    }

    private void createSelector() throws IOException {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            LOG.error("Error creating selector: " + e.getMessage());
        }
    }

    private void createSocket(String destAddr, int destPort) {
        LOG.debug("Creation of the socket for " + destAddr + " and port " + destPort);

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
            clntChan.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            peersNotConnected.add(clntChan);

        } catch (IOException e) {
            LOG.fatal("Could not create Socket, is the client still up ?");
        }
    }

    private void connectToAllClients(ProgressBarArray torrentProgressBars) {
        // if we found peers from tracker, register them and send handshake
        // HashMap<String, ArrayList<Integer>> craftedMap = new HashMap<String,
        // ArrayList<Integer>>();
        // craftedMap.put("127.0.0.1", new ArrayList<Integer>(Arrays.asList(2001, 2002,
        // 2003)));
        // Map.Entry<String, ArrayList<Integer>> firstEntry =
        // craftedMap.entrySet().iterator().next();
        // LOG.warn("Using crafted map instead of tracker answer");
        Iterator<Map.Entry<String, ArrayList<Integer>>> iterator = this.tracker.getPeersMap().entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<String, ArrayList<Integer>> firstEntry = iterator.next();
                String destAddress = firstEntry.getKey();
                ArrayList<Integer> portNumbers = firstEntry.getValue();
                for (int port : portNumbers) {
                    if (this.portsConnected.contains(port)) {
                        continue;
                    } else {
                        LOG.debug("Connecting to localhost with port : " + port);
                        this.createSocket(destAddress, port);
                        this.portsConnected.add(port);
                        if (Logger.getRootLogger().getLevel() == Level.INFO) {
                            torrentProgressBars.add(this.progressBarBuilder, "/" + destAddress + ":" + port);
                        }
                        // send handshakes to new client
                        for (SocketChannel clntChan : this.peersNotConnected) {
                            if (!this.handshakeSent.contains(clntChan)) {
                                Handshake.sendMessage(this.torrent.info_hash, clntChan);
                                this.handshakeSent.add(clntChan);
                            }
                        }
                    }
                }
            }
    }

    private boolean receivedMsg(MsgCoderToWire coderToWire, MsgCoderFromWire coderFromWire, SocketChannel clntChan,
            ProgressBarArray torrentProgressBars, ProgressBar pbCPU, ProgressBar pbMemory) throws IOException {

        Object msgReceived = coderFromWire.fromWire(clntChan);

        if (msgReceived == null) {

            // LOG.error("Message received is null or not readablem closing channel");

            return false;
        }
        if (msgReceived instanceof Integer && (int) msgReceived == -1) {
            // error during reading, keep connection
            // StatGetter.clearScreen();
            return true;
        }

        if (msgReceived instanceof Handshake) {
            LOG.debug("received handshake from client " + clntChan);
            Handshake handshake = (Handshake) msgReceived;
            return this.handleHandshake(handshake, clntChan, torrentProgressBars);
        }

        // cast to message to get type
        Msg msg = (Msg) msgReceived;
        int msgType = msg.getMsgType();

        if (msgType < 0 || msgType > 8) {
            LOG.error("Message type not in range");
            return false;
        }

        LOG.debug("Handling " + Msg.messagesNames.get(msgType) + " message");
        // cast to specific message and doing logic
        int nextPiece;
        switch (msgType) {
            case Simple.CHOKE:
                Simple choke = (Simple) msgReceived;
                break;
            case Simple.UNCHOKE:
                // get the next requested piece and send the request message for it
                nextPiece = this.pieceManager.nextPieceToRequest(clntChan.socket());
                LOG.debug("Next piece to be requested " + nextPiece);
                if (nextPiece != -1) {
                    if (this.pieceManager.getEndgameStatus()) {
                        for (SocketChannel channel : this.peersConnected) {
                            Request.sendMessageForIndex(nextPiece, channel);
                            LOG.debug("Request message sent for " + nextPiece + " to client " + clntChan);
                        }
                    } else {
                        Request.sendMessageForIndex(nextPiece, clntChan);
                        LOG.debug("Request message sent for " + nextPiece + " to client " + clntChan);
                    }
                    // set this piece as requested
                    this.pieceManager.requestSent(nextPiece);
                } else if (this.pieceManager.getPieceNeeded().size() == 0) {
                    if (!isSeeding) {
                        Simple.sendMessage(Simple.NOTINTERESTED, clntChan);
                    }
                    LOG.debug("Generating GET Request for tracker");
                    this.tracker.generateUrl(Tracker.EVENT_COMPLETED);
                    LOG.debug("Successfully generated GET Request");
                    this.tracker.getRequest(Tracker.EVENT_COMPLETED);
                    this.leecherOrSeeder(this.outputFile.getParentFolder());
                    if (isSeeding) {
                        torrentProgressBars.getByName(this.outputFile.getName()).setExtraMessage("Seeding...");
                    }
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
                if (Logger.getRootLogger().getLevel() == Level.INFO) {
                    this.increaseClientProgress(have.getIndex(), clntChan, torrentProgressBars);
                    this.mesureUsage(pbCPU, pbMemory);
                }
                this.handleHave(have.getIndex(), clntChan);
                break;
            case Bitfield.BITFIELD_TYPE:
                bitfieldReceived = (Bitfield) msgReceived;
                this.handleBitfield(bitfieldReceived, clntChan, torrentProgressBars);
                if (!isSeeding) {
                    // send interested only if peer has piece we need
                    if (this.pieceManager.nextPieceToRequest(clntChan.socket()) != -1) {
                        Simple.sendMessage(Simple.INTERESTED, clntChan);
                        LOG.debug("Send interested message to " + clntChan);
                    }
                }
                break;
            case Request.REQUEST_TYPE:
                Request request = (Request) msgReceived;
                LOG.debug("Request message received for index " + request.getIndex() + " from client " + clntChan);
                this.handleRequest(request, clntChan);
                if (Logger.getRootLogger().getLevel() == Level.INFO) {
                    torrentProgressBars.getByName(this.outputFile.getName()).setExtraMessage("Sending...");
                }
                break;
            case Piece.PIECE_TYPE:
                Piece piece = (Piece) msgReceived;
                LOG.debug("Piece message received for index " + piece.getPieceIndex() + " from client " + clntChan);
                boolean increaseNeeded = this.handlePieceMsg(piece, clntChan, torrentProgressBars);
                if (Logger.getRootLogger().getLevel() == Level.INFO) {
                    this.mesureUsage(pbCPU, pbMemory);
                    torrentProgressBars.getByName(this.outputFile.getName()).setExtraMessage("Receiving...");
                    if (increaseNeeded) {
                        this.increaseOurProgress(piece.getPieceIndex(), this.outputFile.getName(), torrentProgressBars);
                    }
                }
                break;
            case Cancel.CANCEL_TYPE:
                Cancel cancel = (Cancel) msgReceived;
                LOG.debug("Cancel message received for index " + cancel.getIndex() + " from client " + clntChan);
                this.handleCancel(cancel, clntChan);
                break;
            default:
                // never reached test before;
                break;
        }

        return piecesMissing;
    }

    private void handleCancel(Cancel cancel, SocketChannel clntChan) {
        // TODO if multiple messages read at the same time
    }

    private void handleHave(int pieceIndex, SocketChannel clntChan) {
        if (!this.pieceManager.getPieceMap().containsKey(pieceIndex)) {
            ArrayList<Socket> peers = new ArrayList<Socket>();
            peers.add(clntChan.socket());
            this.pieceManager.getPieceMap().put(pieceIndex, peers);
        } else {
            if (!this.pieceManager.getPieceMap().get(pieceIndex).contains(clntChan.socket())) {
                this.pieceManager.getPieceMap().get(pieceIndex).add(clntChan.socket());
            }
        }
    }

    private void handleBitfield(Bitfield bitfieldReceived2, SocketChannel clntChan,
            ProgressBarArray torrentProgressBars) {
        // Override almost full bitfield with full one (lazy bitfield)
        if (bitfieldReceived.getBitfieldDATA() == Bitfield.fakeFullBitfield) {
            LOG.debug("Received full lazy bitfield, override to full");
            bitfieldReceived.setBitfieldDATA(Bitfield.fullBitfield);
        }

        // create the list of available pieces for this client based on its bietfield
        piecesDispo = Bitfield.convertBitfieldToList(bitfieldReceived.getBitfieldDATA(), Torrent.numberOfPieces);

        // reset progressbar if receive a bitfield because more accurate
        if (Logger.getRootLogger().getLevel() == Level.INFO) {
            this.resetClientProgress(clntChan, torrentProgressBars);
        }
        // add the client represented by its socket and all its pieces to PieceMap
        for (int pieceIndex : piecesDispo) {
            if (!this.pieceManager.getPieceMap().containsKey(pieceIndex)) {
                ArrayList<Socket> peers = new ArrayList<Socket>();
                peers.add(clntChan.socket());
                this.pieceManager.getPieceMap().put(pieceIndex, peers);
            } else {
                if (!this.pieceManager.getPieceMap().get(pieceIndex).contains(clntChan.socket())) {
                    this.pieceManager.getPieceMap().get(pieceIndex).add(clntChan.socket());
                }
            }
            if (Logger.getRootLogger().getLevel() == Level.INFO) {
                this.increaseClientProgress(pieceIndex, clntChan, torrentProgressBars);
            }
        }

        this.pieceManager.setPieceMap(
                (HashMap<Integer, ArrayList<Socket>>) MapUtil.sortBySize(this.pieceManager.getPieceMap(), MapUtil.ASC));

        this.pieceManager.sortPiecesNeeded();

        LOG.debug("New order for pieces needed : " + MapUtil.ArrayListToString(this.pieceManager.getPieceNeeded()));

        // Move this peer status from not connected to connected
        this.peersNotConnected.remove(clntChan);
        this.peersConnected.add(clntChan);
        // Print the peers with their available pieces
        // for (int name: this.pieceManager.getPieceMap().keySet()){
        // String value = this.pieceManager.getPieceMap().get(name).toString();
        // LOG.debug(name + " " + value);
        // }
    }

    private void resetClientProgress(SocketChannel clntChan, ProgressBarArray torrentProgressBars) {
        torrentProgressBars.getByIPPort(clntChan).stepTo(0);
    }

    private void increaseClientProgress(int index, SocketChannel clntChan, ProgressBarArray torrentProgressBars) {
        if (index == Torrent.numberOfPieces - 1) {
            torrentProgressBars.getByIPPort(clntChan).stepBy(Torrent.lastPieceLength);
        } else {
            torrentProgressBars.getByIPPort(clntChan).stepBy(Torrent.pieces_length);
        }
    }

    private void increaseOurProgress(int index, String fileName, ProgressBarArray torrentProgressBars) {
        if (index == Torrent.numberOfPieces - 1) {
            torrentProgressBars.getByName(fileName).stepBy(Torrent.lastPieceLength);
        } else {
            torrentProgressBars.getByName(fileName).stepBy(Torrent.pieces_length);
        }
    }

    private void mesureUsage(ProgressBar pbCPU, ProgressBar pbMemory) {
        pbCPU.stepTo((long) StatGetter.getLoadAverage());
        pbMemory.stepTo((long) StatGetter.getUsedMemory());
    }

    private boolean handleHandshake(Handshake handshake, SocketChannel clntChan, ProgressBarArray torrentProgressBars)
            throws IOException {
        LOG.debug("Handling Handshake message");
        if (handshake.getSha1Hash().compareTo(this.torrent.info_hash) != 0) {
            LOG.error("Sha1 hash received different from torrent file");
            return false;
        }
        String peerId = new String(handshake.getPeerId());
        this.socketToPeerIdMap.put(clntChan, peerId);

        if (Logger.getRootLogger().getLevel() == Level.INFO) {
            torrentProgressBars.getByIPPort(clntChan).setExtraMessage(peerId);
        }
        
        if (!this.handshakeSent.contains(clntChan)) {
            Handshake.sendMessage(this.torrent.info_hash, clntChan);
            this.handshakeSent.add(clntChan);
        }
        Bitfield.sendMessage(Bitfield.ourBitfieldData, clntChan);
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
        // Not reliable in case one piece is lost but most beautiful, clients does not
        // send all the have
    }

    // returns if update needs to be done to progressbar
    private boolean handlePieceMsg(Piece piece, SocketChannel clntChan, ProgressBarArray torrentProgressBars)
            throws IOException {
        int pieceIndex = piece.getPieceIndex();
        int beginOffset = piece.getBeginOffset();
        byte[] data = piece.getData();

        LOG.debug("Piece with index " + pieceIndex + " with beginOffset " + beginOffset);

        // if piece is not needed (already received from other peer during endgame)
        if (!this.pieceManager.isNeeded(pieceIndex)) {
            LOG.warn("Piece no more needed, not adding part to the file");
            return false;
        }
        this.outputFile.writeToFile(pieceIndex * Torrent.pieces_length + beginOffset, data);

        int pieceLength = Torrent.pieces_length;
        if (pieceIndex == Torrent.numberOfPieces - 1) {
            pieceLength = Torrent.lastPieceLength;
        }

        // request only if last part of piece has been received
        if (beginOffset == pieceLength - data.length) {
            if (Piece.testPieceHash(pieceIndex, this.outputFile.getPieceData(pieceIndex))) {

                // send have for this piece to all other peers
                for (SocketChannel channel : this.peersConnected) {
                    Have.sendMessage(pieceIndex, channel);
                    // send cancel to all other channels
                    if (this.pieceManager.getEndgameStatus() && clntChan != channel) {
                        Cancel.sendMessageForIndex(pieceIndex, channel);
                    }
                }
                // set this piece downloaded
                this.pieceManager.pieceNoMoreNeeded(pieceIndex);

                // search for next piece to be requested
                int nextPiece = this.pieceManager.nextPieceToRequest(clntChan.socket());
                LOG.debug("Next Piece to be requested " + nextPiece);
                // only request if we still lack pieces
                if (nextPiece != -1) {
                    if (this.pieceManager.getEndgameStatus()) {
                        for (SocketChannel channel : this.peersConnected) {
                            Request.sendMessageForIndex(nextPiece, channel);
                            LOG.debug("Request message sent for " + nextPiece + " to client " + clntChan);
                        }
                    } else {
                        Request.sendMessageForIndex(nextPiece, clntChan);
                        LOG.debug("Request message sent for " + nextPiece + " to client " + clntChan);
                    }
                    // set this piece as requested
                    this.pieceManager.requestSent(nextPiece);
                } else if (this.pieceManager.getPieceNeeded().size() == 0) {
                    if (!isSeeding) {
                        Simple.sendMessage(Simple.NOTINTERESTED, clntChan);
                    }
                    LOG.debug("Generating GET Request for tracker");
                    this.tracker.generateUrl(Tracker.EVENT_COMPLETED);
                    LOG.debug("Successfully generated GET Request");
                    this.tracker.getRequest(Tracker.EVENT_COMPLETED);
                    this.leecherOrSeeder(this.outputFile.getParentFolder());
                    if (isSeeding) {
                        torrentProgressBars.getByName(this.outputFile.getName()).setExtraMessage("Seeding...");
                    }
                }
                return true;
            } else {
                // piece not good ask for the same again
                Request.sendMessageForIndex(pieceIndex, clntChan);
                return false;
            }
        }
        return false;

    }

    private void closeConnection(SocketChannel clntChan, ProgressBarArray torrentProgressBars) throws IOException {
        // end communication with client

        LOG.debug("Closing connection with channel" + clntChan);
        int port = clntChan.socket().getPort();
        if (this.peersNotConnected.contains(clntChan) && this.portsConnected.contains(port)) {
            this.peersNotConnected.remove(clntChan);
            this.portsConnected.remove(Integer.valueOf(port));
        }
        if (this.peersConnected.contains(clntChan)) {
            this.peersConnected.remove(clntChan);
        }

        if (Logger.getRootLogger().getLevel() == Level.INFO) {
            torrentProgressBars.removeByIPPort(clntChan);
        }

        clntChan.close();

        // Buggy does not print all peers
        // StatGetter.clearScreen();
    }

}