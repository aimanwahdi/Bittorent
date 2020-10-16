package bittorensimag.Client;

import java.io.File;
import java.io.IOException;

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

    public boolean compile() {
        Torrent torrent;
        try {
            torrent = new Torrent(sourceFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}