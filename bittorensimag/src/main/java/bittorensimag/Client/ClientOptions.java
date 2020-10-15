package bittorensimag.Client;

import java.io.File;
import java.lang.RuntimeException;

/**
 * User-specified options influencing the compilation.
 *
 * @author gouloisw
 * @date 06/10/20
 */
public class ClientOptions {

    private boolean debug = false;
    private boolean info = false;
    private boolean printBanner = false;
    private boolean finishedOptions = false;
    private File sourceFile = null;
    private File destinationFolder = null;

    public void parseArgs(String[] args) throws RuntimeException {
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if (argument.charAt(0) == '-') {
                if (finishedOptions) {
                    throw new RuntimeException("Cannot pass options after files");
                }
                switch (argument.charAt(1)) {
                    // bonus afficher banniere en asci art
                    case 'b':
                        printBanner = true;
                        break;
                    case 'd':
                        debug = true;
                        break;
                    case 'i':
                        info = true;
                        break;
                    default:
                        throw new RuntimeException("This option does not exist for the bittorent client");
                }
            } else { // si l'argument n'a pas de tiret, alors c'est un fichier
                finishedOptions = true;
                File f = new File(argument);
                if (f.isFile() && f.exists()) {
                    if (this.sourceFile == null) {
                        this.sourceFile = f;
                    } else {
                        System.err.println("You passed in multiple torrent files");
                        this.displayUsage();
                    }
                } else if (f.isDirectory() && f.exists()) {
                    if (this.destinationFolder == null && this.sourceFile != null) {
                        this.destinationFolder = f;
                    } else if (this.destinationFolder == null && this.sourceFile == null) {
                        System.err.println("You must pass torrent file first");
                        this.displayUsage();
                    } else {
                        System.err.println("You passed in too much arguments");
                        this.displayUsage();
                    }
                }
            }
        }
    }

    public boolean getPrintBanner() {
        return printBanner;
    }

    public boolean getDebug() {
        return debug;
    }

    public boolean getInfo() {
        return info;
    }

    public File getSourceFile() {
        return this.sourceFile;
    }

    public File getDestinationFolder() {
        return this.destinationFolder;
    }

    protected void bannerInTerminal() {
        // TODO
        System.out.println("ASCI ART BANNER");
    }

    protected void displayUsage() {
        // TODO
        System.out.println("This is how you should use Bittorensimag");
    }
}