package bittorensimag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
            System.err.println("Error during option parsing:\n"
                    + e.getMessage());
            //TODO Display Usage of the commandline
            // options.displayUsage(); 
            System.exit(1);
        }
        //TODO ASCI Art banner
        if (options.getPrintBanner()) {
            options.bannerInTerminal();
            return;
        }

        if (options.getSourceFiles().isEmpty()) {
            options.displayUsage();
        } else {
            for (File source : options.getSourceFiles()) {
                ClientCompiler compiler = new ClientCompiler(options, source);
                if (compiler.compile()) {
                    error = true;
                }
            }
        }
        System.exit(error ? 1 : 0);
    }
}