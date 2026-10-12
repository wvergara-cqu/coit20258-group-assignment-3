package drs.server.service;

/**
 * A business-rule violation whose message is safe to return to the client.
 */
public class ServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    public ServiceException(String message) {
        super(message);
    }
}
