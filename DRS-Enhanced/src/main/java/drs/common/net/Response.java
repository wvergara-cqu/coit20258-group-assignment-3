package drs.common.net;

import java.io.Serial;
import java.io.Serializable;

/**
 * A message returned by the server: a success flag, a human-readable message and
 * optional data.
 */
public class Response implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final Serializable data;

    private Response(boolean success, String message, Serializable data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /** @return a successful response carrying data */
    public static Response ok(String message, Serializable data) {
        return new Response(true, message, data);
    }

    /** @return a successful response with no data */
    public static Response ok(String message) {
        return new Response(true, message, null);
    }

    /** @return a failed response */
    public static Response error(String message) {
        return new Response(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Serializable getData() { return data; }

    @Override
    public String toString() {
        return "Response[" + (success ? "OK" : "ERROR") + ": " + message + "]";
    }
}
