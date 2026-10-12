package drs.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import drs.common.net.Request;
import drs.common.net.Response;
import drs.server.security.Session;

/**
 * Serves one connected client on a pooled worker thread. Reads Requests until
 * the client disconnects, dispatching each and writing back a Response.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    /**
     * Deserialisation allow-list: only DRS classes and the JDK value/collection
     * types they use may be read from the network, blocking gadget-chain attacks.
     */
    static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=20;maxrefs=100000;maxbytes=5000000;"
                    + "drs.common.**;java.lang.*;java.util.*;[Ljava.lang.Object;;!*");

    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final DRSServer server;

    public ClientHandler(Socket socket, RequestDispatcher dispatcher, DRSServer server) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.server = server;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress().toString();
        Session session = new Session(remote);
        LOG.info(() -> "Client connected " + remote + " on " + Thread.currentThread().getName());
        try (socket;
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                in.setObjectInputFilter(FILTER);
                while (!socket.isClosed()) {
                    Object obj = in.readObject();
                    Response response = obj instanceof Request req
                            ? dispatcher.handle(req, session)
                            : Response.error("Unknown message type.");
                    out.writeObject(response);
                    out.reset();        // never resend cached (stale) object graphs
                    out.flush();
                }
            }
        } catch (EOFException | SocketException e) {
            // normal client disconnect
        } catch (IOException | ClassNotFoundException e) {
            LOG.log(Level.WARNING, "Connection error with " + remote, e);
        } finally {
            server.clientDisconnected();
            LOG.info(() -> "Client disconnected " + remote + " (" + session.getUsername() + ")");
        }
    }
}
