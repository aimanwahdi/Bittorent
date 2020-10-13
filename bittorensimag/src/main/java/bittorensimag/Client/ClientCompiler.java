package bittorensimag.Client;

import java.io.File;

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
        System.out.println("The client compiles");
        return true;
    }
}