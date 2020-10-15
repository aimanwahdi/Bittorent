package bittorensimag.Client;

import java.io.File;
import java.lang.RuntimeException;

/**
 * Main class for the command-line Bittorensimag CLient.
 *
 * @author gouloisw
 * @date 06/10/20
 */
public class ClientMain {
    public static void main(String[] args) throws RuntimeException {
        boolean error = false;
        final ClientOptions options = new ClientOptions();
        try {
            options.parseArgs(args);
        } catch (RuntimeException e) {
            System.err.println("Error during option parsing:\n" + e.getMessage());
            // TODO Display Usage of the commandline
            options.displayUsage();
            System.exit(1);
        }
        // TODO ASCI Art banner
        if (options.getPrintBanner()) {
            options.bannerInTerminal();
            return;
        }

        File sourceFile = options.getSourceFile();
        File destinationFolder = options.getDestinationFolder();

        if (sourceFile == null || destinationFolder == null) {
            System.err.println("Impossible to start, need source file and destination folder");
            options.displayUsage();
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