package drs.common.net;

import java.io.Serial;
import java.io.Serializable;

/**
 * A message sent from client to server. The payload is any serialisable object
 * (a model object or an Object[] of arguments). For Signable payloads the client
 * also supplies a Base64 RSA signature of the payload's canonical form.
 */
public class Request implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Command command;
    private final Serializable payload;
    private String signature;
    private final long clientTimestamp;

    /**
     * @param command the operation requested
     * @param payload the argument(s); may be null
     */
    public Request(Command command, Serializable payload) {
        this.command = command;
        this.payload = payload;
        this.clientTimestamp = System.currentTimeMillis();
    }

    public Command getCommand() { return command; }
    public Serializable getPayload() { return payload; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public long getClientTimestamp() { return clientTimestamp; }

    @Override
    public String toString() {
        return "Request[" + command + (signature != null ? ", signed" : "") + "]";
    }
}
