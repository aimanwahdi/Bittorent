package bittorensimag.Client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.MsgCoderFromWire;
import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Torrent.*;

/**
 * Instance of the bittorent compiler
 *
 * @author gouloisw
 * @date 06/10/20
 */
public class ClientCompiler {
    private static final Logger LOG = Logger.getLogger(ClientCompiler.class);

    private final File sourceTorrent;
    private final File destinationFolder;
    private Torrent torrent;

    public ClientCompiler(File sourceTorrent, File destinationFolder) {
        super();
        this.sourceTorrent = sourceTorrent;
        this.destinationFolder = destinationFolder;
    }

    public boolean compile() throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        LOG.debug("Creating torrent " + sourceTorrent + " with destination folder " + destinationFolder);
        this.torrent = new Torrent(sourceTorrent);
        Tracker tracker = new Tracker(this.torrent);
        LOG.debug("Creation of torrent object and tracker successful");

        LOG.debug("Generating GET Request for tracker");
        tracker.generateUrl(Tracker.EVENT_STARTED);
        LOG.debug("Successfully generated GET Request");
        tracker.getRequest(Tracker.EVENT_STARTED);

        // // Try each 5 sec to find other peers
        // synchronized (this) {
        // while (!tracker.foundAnotherPeer()) {
        // LOG.warn("There is not another peer please restart other client(s)");
        // try {
        // this.wait(5000);
        // } catch (InterruptedException e) {
        // LOG.fatal("Thread got interrupted while waiting " + e.getMessage());
        // }
        // tracker.getRequest(Tracker.EVENT_STARTED);
        // }
        // }
        if (!tracker.foundAnotherPeer()) {
            LOG.warn("There is not another peer according to tracker");
        } else {
            int numberOfPeers = tracker.getPeersMap().entrySet().iterator().next().getValue().size();
            LOG.info("Found " + numberOfPeers + " peer from tracker for torrent file : " + sourceTorrent.getName());
        }

        Client client = new Client(torrent, tracker, new MsgCoderToWire(), new MsgCoderFromWire(), destinationFolder);

        client.startProgress();
        return true;
    }
}