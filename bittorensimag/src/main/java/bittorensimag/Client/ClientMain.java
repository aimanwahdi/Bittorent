package bittorensimag.Client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.RuntimeException;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

/**
 * Main class for the command-line Bittorensimag CLient.
 *
 * @author gouloisw
 * @date 06/10/20
 */
public class ClientMain {
    private static Logger LOG = Logger.getLogger(ClientMain.class);

    public static void main(String[] args) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        LOG.info("Bittorensimag client started");
        boolean error = false;
        final ClientOptions options = new ClientOptions();
        try {
            options.parseArgs(args);
        } catch (RuntimeException | CLIException e) {
            LOG.fatal("Error during option parsing:\n" + e.getMessage());
            ClientOptions.displayUsage();
            System.exit(1);
        }
        if (options.getNoArgs()) {
            ClientOptions.displayUsage();
            return;
        }

        if (options.getPrintBanner()) {
            options.bannerInTerminal();
            return;
        }

        File sourceFile = options.getSourceFile();
        File destinationFolder = options.getDestinationFolder();

        if (sourceFile == null || destinationFolder == null) {
            LOG.fatal("Impossible to start, need source file and destination folder");
            ClientOptions.displayUsage();
            error = true;
        } else {
            ClientCompiler compiler = new ClientCompiler(options, sourceFile, destinationFolder);
            if (compiler.compile()) {
                error = false;
            } else {
                error = true;
            }

        }
        System.exit(error ? 1 : 0);
    }
}
