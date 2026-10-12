package drs.client.service;

/**
 * A failure reported by the server or the network, with a message suitable for
 * display in the GUI.
 */
public class ClientException extends Exception {

    private static final long serialVersionUID = 1L;

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
