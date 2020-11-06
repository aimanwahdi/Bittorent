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
    private final File sourceFile;
    private final File destinationFolder;

    public ClientCompiler(ClientOptions clientOptions, File sourceFile, File destinationFolder) {
        super();
        this.clientOptions = clientOptions;
        this.sourceFile = sourceFile;
        this.destinationFolder = destinationFolder;
    }

    public boolean compile() throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        Torrent torrent = new Torrent(sourceFile);
        Tracker tracker = new Tracker(torrent);
        tracker.getRequest();

        Client client = new Client(torrent, tracker, new MsgCoderToWire());
        client.createSocket("127.0.0.1", 6881);
        client.startCommunication();
        return true;
    }
}