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

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static final String ANSI_BOLD = "\033[0;1m";

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
        System.out.println(
                "██████╗ ██╗████████╗████████╗ ██████╗ ██████╗ ███████╗███╗   ██╗███████╗██╗███╗   ███╗ █████╗  ██████╗ ");
        System.out.println(
                "██╔══██╗██║╚══██╔══╝╚══██╔══╝██╔═══██╗██╔══██╗██╔════╝████╗  ██║██╔════╝██║████╗ ████║██╔══██╗██╔════╝ ");
        System.out.println(
                "██████╔╝██║   ██║      ██║   ██║   ██║██████╔╝█████╗  ██╔██╗ ██║███████╗██║██╔████╔██║███████║██║  ███╗");
        System.out.println(
                "██╔══██╗██║   ██║      ██║   ██║   ██║██╔══██╗██╔══╝  ██║╚██╗██║╚════██║██║██║╚██╔╝██║██╔══██║██║   ██║");
        System.out.println(
                "██████╔╝██║   ██║      ██║   ╚██████╔╝██║  ██║███████╗██║ ╚████║███████║██║██║ ╚═╝ ██║██║  ██║╚██████╔╝");
        System.out.println(
                "╚═════╝ ╚═╝   ╚═╝      ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═══╝╚══════╝╚═╝╚═╝     ╚═╝╚═╝  ╚═╝ ╚═════╝ ");
    }

    protected void displayUsage() {
        System.out.println("Usage : \n bittorensimag [-b] [-d] [-i]  <file.torrent> <download folder>");
        System.out.println(ANSI_BOLD + ANSI_YELLOW + "-b  " + ANSI_RESET + ANSI_CYAN + "(banner)" + ANSI_RESET
                + "\t: print banner of the project");
        System.out.println(ANSI_BOLD + ANSI_YELLOW + "-d  " + ANSI_RESET + ANSI_CYAN + "(debug)" + ANSI_RESET
                + "\t: print minimal debug information");
        System.out.println(ANSI_BOLD + ANSI_YELLOW + "-i  " + ANSI_RESET + ANSI_CYAN + "(info)" + ANSI_RESET
                + "\t: print each second information about peers (bittorrent application, IP address, port, download/upload of pieces)");
    }
}
