package bittorensimag.Client;

import java.io.File;
import java.lang.RuntimeException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * User-specified options influencing the compilation.
 *
 * @author gouloisw
 * @date 06/10/20
 */
public class ClientOptions {
    public final static Logger LOG = Logger.getLogger(ClientOptions.class);

    public static final int QUIET = 0;
    public static final int INFO = 1;
    public static final int DEBUG = 2;
    public static final int TRACE = 3; // TODO not used for now

    private boolean noArgs = false;
    private boolean error = false;
    private int debug = 0;
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

    public void parseArgs(String[] args) throws CLIException {
        if (args.length == 0) {
            noArgs = true;
            return;
        }
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if (argument.charAt(0) == '-') {
                if (finishedOptions) {
                    throw new CLIException("Cannot pass options after files");
                }
                switch (argument.charAt(1)) {
                    // bonus afficher banniere en asci art
                    case 'b':
                        printBanner = true;
                        break;
                    case 'd':
                        debug = 2;
                        break;
                    case 'i':
                        debug = 1;
                        break;
                    default:
                        throw new CLIException("This option does not exist for the bittorent client");
                }
            } else { // si l'argument n'a pas de tiret, alors c'est un fichier
                finishedOptions = true;
                File f = new File(argument);
                if (f.isFile() && f.exists()) {
                    if (this.sourceFile == null) {
                        this.sourceFile = f;
                    } else {
                        throw new CLIException("You passed in multiple torrent files");
                    }
                } else if (f.isDirectory() && f.exists()) {
                    if (this.destinationFolder == null && this.sourceFile != null) {
                        this.destinationFolder = f;
                    } else if (this.destinationFolder == null && this.sourceFile == null) {
                        throw new CLIException("You must pass torrent file first");
                    } else {
                        throw new CLIException("You passed in multiple output folders");
                    }
                }
            }
        }
        Logger logger = Logger.getRootLogger();
        // map command-line debug option to log4j's level.
        switch (getDebug()) {
            case QUIET:
                break; // keep default
            case INFO:
                logger.setLevel(Level.INFO);
                break;
            case DEBUG:
                logger.setLevel(Level.DEBUG);
                break;
            case TRACE:
                logger.setLevel(Level.TRACE);
                break;
            default:
                logger.setLevel(Level.ALL);
                break;
        }
        logger.info("Application-wide trace level set to " + logger.getLevel());
    }

    public boolean getNoArgs() {
        return this.noArgs;
    }

    public boolean getPrintBanner() {
        return printBanner;
    }

    public int getDebug() {
        return debug;
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

    protected static void displayUsage() {
        System.out.println("Usage : \n bittorensimag [-b] [-d] [-i]  <file.torrent> <download folder>");
        System.out.println(ANSI_BOLD + ANSI_YELLOW + "-b  " + ANSI_RESET + ANSI_CYAN + "(banner)" + ANSI_RESET
                + "\t: print banner of the project");
        System.out.println(ANSI_BOLD + ANSI_YELLOW + "-d  " + ANSI_RESET + ANSI_CYAN + "(debug)" + ANSI_RESET
                + "\t: print minimal debug information");
        System.out.println(ANSI_BOLD + ANSI_YELLOW + "-i  " + ANSI_RESET + ANSI_CYAN + "(info)" + ANSI_RESET
                + "\t: print each second information about peers (bittorrent application, IP address, port, download/upload of pieces)");
    }
}