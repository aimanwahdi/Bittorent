package bittorensimag.Client;

/**
 * Exception raised when the command-line options are incorrect.
 *
 * @author gouloisw
 * @date 14/11/2020
 */
public class CLIException extends Exception {

    private static final long serialVersionUID = -7687556758547705280L;

    public CLIException(final String message) {
        super(message);
        ClientOptions.displayUsage();
    }
}
