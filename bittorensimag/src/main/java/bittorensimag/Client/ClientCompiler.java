package bittorensimag.Client;

import java.io.File;

import bittorensimag.MessageCoder.MsgCoderFromWire;
import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Torrent.*;
import bittorensimag.Util.MapUtil;

/**
 * Instance of the bittorent compiler
 *
 * @author gouloisw
 * @date 06/10/20
 */
public class ClientCompiler {

    private final ClientOptions clientOptions;
    private final File sourceTorrent;
    private final File destinationFolder;
    private Torrent torrent;

    public ClientCompiler(ClientOptions clientOptions, File sourceTorrent, File destinationFolder) {
        super();
        this.clientOptions = clientOptions;
        this.sourceTorrent = sourceTorrent;
        this.destinationFolder = destinationFolder;
    }

    public boolean compile() throws Exception {
        this.torrent = new Torrent(sourceTorrent);
        Tracker tracker = new Tracker(this.torrent);
        tracker.generateUrl(Tracker.EVENT_STARTED);
        tracker.getRequest(Tracker.EVENT_STARTED);

        // Try each 5 sec to find other peers
        synchronized (this) {
            while (!tracker.foundAnotherPeer()) {
                this.wait(5000);
                tracker.getRequest(Tracker.EVENT_STARTED);
            }
        }
        System.out.println("Found another peer for torrent file");
        
        Client client = new Client(torrent, tracker, new MsgCoderToWire(), new MsgCoderFromWire());
        client.leecherOrSeeder();
        client.startCommunication();
        if (!client.isSeeding) {
            byte[] fileContent = MapUtil.convertHashMapToByteArray((int) this.torrent.getMetadata().get(Torrent.LENGTH),
                Torrent.dataMap);
            Output out = new Output((String) this.torrent.getMetadata().get(Torrent.NAME),
                this.destinationFolder.getAbsolutePath() + "/", fileContent);
            out.generateFile();
            tracker.generateUrl(Tracker.EVENT_COMPLETED);
            tracker.getRequest(Tracker.EVENT_COMPLETED);
        }

        return true;
    }
}