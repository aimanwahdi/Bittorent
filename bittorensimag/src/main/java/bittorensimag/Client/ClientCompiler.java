package bittorensimag.Client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import bittorensimag.MessageCoder.MsgCoderToWire;
import bittorensimag.Torrent.*;

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
        tracker.getRequest();

        Client client = new Client(torrent, tracker, new MsgCoderToWire());
        client.leecherOrSeeder();
        // client.startCommunication();
        return true;
    }
}