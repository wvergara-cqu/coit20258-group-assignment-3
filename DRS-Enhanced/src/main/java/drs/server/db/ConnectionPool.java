package drs.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A small fixed-size JDBC connection pool backed by an ArrayBlockingQueue.
 * Client-handler threads borrow a connection, run their work and return it, so
 * concurrent clients never share a Connection and the server never opens more
 * than poolSize connections to MySQL.
 */
public class ConnectionPool implements AutoCloseable {

    private static final int BORROW_TIMEOUT_SECONDS = 10;

    private final BlockingQueue<Connection> idle;
    private final String url;
    private final String user;
    private final String password;

    /**
     * Opens poolSize connections eagerly.
     *
     * @throws SQLException if MySQL cannot be reached
     */
    public ConnectionPool(String url, String user, String password, int poolSize) throws SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.idle = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            idle.add(open());
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Runs work with a pooled connection and always returns the connection.
     *
     * @return the work's result
     * @throws SQLException on database error or pool exhaustion
     */
    public <T> T execute(SqlFunction<T> work) throws SQLException {
        Connection c = borrow();
        try {
            return work.apply(c);
        } finally {
            release(c);
        }
    }

    /**
     * Runs work inside a single transaction; rolls back on any failure.
     *
     * @return the work's result
     * @throws SQLException on database error
     */
    public <T> T transaction(SqlFunction<T> work) throws SQLException {
        Connection c = borrow();
        try {
            c.setAutoCommit(false);
            T result = work.apply(c);
            c.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
            release(c);
        }
    }

    private Connection borrow() throws SQLException {
        try {
            Connection c = idle.poll(BORROW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (c == null) {
                throw new SQLException("Database busy: no connection available");
            }
            if (!c.isValid(2)) {
                c.close();
                c = open();
            }
            return c;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a connection", e);
        }
    }

    private void release(Connection c) {
        if (c != null && !idle.offer(c)) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // connection is being discarded anyway
            }
        }
    }

    @Override
    public void close() {
        Connection c;
        while ((c = idle.poll()) != null) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // shutting down
            }
        }
    }
}
