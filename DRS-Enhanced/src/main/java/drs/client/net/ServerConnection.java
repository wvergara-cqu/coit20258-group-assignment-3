package drs.client.net;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import drs.common.net.Request;
import drs.common.net.Response;

/**
 * A persistent TCP connection to the DRS server. send() is synchronised so that
 * requests from different JavaFX timers or threads are never interleaved on the
 * stream.
 */
public class ServerConnection implements AutoCloseable {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    /** Same allow-list as the server: only DRS and JDK value types are accepted. */
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=20;maxrefs=200000;maxbytes=20000000;"
                    + "drs.common.**;java.lang.*;java.util.*;[Ljava.lang.Object;;!*");

    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Opens the socket and object streams. */
    public synchronized void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        socket.setSoTimeout(READ_TIMEOUT_MS);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        in.setObjectInputFilter(FILTER);
    }

    /**
     * Sends a request and waits for its response.
     *
     * @throws IOException if the connection fails
     */
    public synchronized Response send(Request request) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Not connected to the DRS server.");
        }
        out.writeObject(request);
        out.reset();
        out.flush();
        try {
            Object obj = in.readObject();
            if (obj instanceof Response r) {
                return r;
            }
            throw new IOException("Unexpected reply from server.");
        } catch (ClassNotFoundException e) {
            throw new IOException("Incompatible server version.", e);
        }
    }

    /** @return true while the socket is open */
    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getHost() { return host; }
    public int getPort() { return port; }

    @Override
    public synchronized void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // closing anyway
        }
    }
}
