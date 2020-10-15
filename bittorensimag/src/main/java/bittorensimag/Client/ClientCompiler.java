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
    private final File source;

    public ClientCompiler(ClientOptions clientOptions, File source) {
        super();
        this.clientOptions = clientOptions;
        this.source = source;
    }

    public boolean compile() {
        Torrent torrent;
        try {
            torrent = new Torrent(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}