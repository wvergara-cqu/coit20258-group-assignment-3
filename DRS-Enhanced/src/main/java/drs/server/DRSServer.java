package drs.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import drs.server.db.AuditDAO;
import drs.server.db.ConnectionPool;
import drs.server.db.DatabaseManager;
import drs.server.db.ReportDAO;
import drs.server.db.ResourceDAO;
import drs.server.db.ResponseDAO;
import drs.server.db.TaskDAO;
import drs.server.db.UserDAO;
import drs.server.db.WarningDAO;
import drs.server.security.CryptoService;
import drs.server.service.AuditService;
import drs.server.service.AuthService;
import drs.server.service.CoordinationService;
import drs.server.service.ReportService;
import drs.server.service.TaskService;
import drs.server.service.WarningService;

/**
 * DRS-Enhanced multi-threaded server (middle tier). Accepts TCP connections and
 * hands each one to a fixed-size worker pool, so many clients are served
 * concurrently while the number of threads stays bounded. On start-up it creates
 * the MySQL database and tables, loads the Assignment 2 data and starts a
 * scheduled job that expires overdue public warnings.
 *
 * Run: mvn exec:java -Pserver   (or run this class from NetBeans)
 */
public class DRSServer {

    private static final Logger LOG = Logger.getLogger(DRSServer.class.getName());

    private final ServerConfig config;
    private final AtomicInteger activeClients = new AtomicInteger();
    private ExecutorService workers;
    private ScheduledExecutorService scheduler;
    private ServerSocket serverSocket;
    private ConnectionPool pool;
    private RequestDispatcher dispatcher;
    private Thread acceptThread;
    private volatile boolean running;

    public DRSServer(ServerConfig config) {
        this.config = config;
    }

    /**
     * Starts the server and returns once it is accepting connections.
     *
     * @throws SQLException if the database cannot be prepared
     * @throws IOException  if the port cannot be bound
     */
    public synchronized void start() throws SQLException, IOException {
        DatabaseManager.initialise(config);
        pool = new ConnectionPool(config.getJdbcUrl(), config.getDbUser(), config.getDbPassword(),
                config.getDbPoolSize());
        CryptoService crypto = new CryptoService(config.getAesKey());

        UserDAO userDAO = new UserDAO(pool);
        ReportDAO reportDAO = new ReportDAO(pool, crypto);
        ResourceDAO resourceDAO = new ResourceDAO(pool);
        TaskDAO taskDAO = new TaskDAO(pool);
        WarningDAO warningDAO = new WarningDAO(pool);

        new DataLoader(userDAO, resourceDAO, reportDAO).loadIfEmpty();

        ReportService reportService = new ReportService(reportDAO, taskDAO, warningDAO);
        reportService.loadTriageQueue();
        WarningService warningService = new WarningService(warningDAO, reportService);
        dispatcher = new RequestDispatcher(
                new AuthService(userDAO), reportService,
                new CoordinationService(resourceDAO, new ResponseDAO(pool), reportService),
                warningService, new TaskService(taskDAO, resourceDAO),
                new AuditService(new AuditDAO(pool)));

        workers = Executors.newFixedThreadPool(config.getThreads(), namedThreads("drs-client-"));
        scheduler = Executors.newSingleThreadScheduledExecutor(namedThreads("drs-scheduler-"));
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int n = warningService.expireOverdue();
                if (n > 0) {
                    LOG.info(n + " public warning(s) expired.");
                }
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Warning expiry job failed", e);
            }
        }, 1, 60, TimeUnit.SECONDS);

        serverSocket = new ServerSocket(config.getPort());
        running = true;
        acceptThread = new Thread(this::acceptLoop, "drs-acceptor");
        acceptThread.start();
        LOG.info("DRS-Enhanced server listening on port " + getPort() + " with " + config.getThreads()
                + " worker threads.");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                activeClients.incrementAndGet();
                workers.execute(new ClientHandler(client, dispatcher, this));
            } catch (SocketException e) {
                if (running) {
                    LOG.log(Level.WARNING, "Accept failed", e);
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Accept failed", e);
            }
        }
    }

    /** Called by a handler when its client disconnects. */
    void clientDisconnected() {
        activeClients.decrementAndGet();
    }

    /** @return number of currently connected clients */
    public int getActiveClients() {
        return activeClients.get();
    }

    /** @return the bound port (useful when configured with port 0 in tests) */
    public int getPort() {
        return serverSocket == null ? config.getPort() : serverSocket.getLocalPort();
    }

    /** Stops accepting clients and releases all resources. */
    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // closing
        }
        if (workers != null) {
            workers.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (pool != null) {
            pool.close();
        }
        LOG.info("DRS-Enhanced server stopped.");
    }

    private static ThreadFactory namedThreads(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, prefix + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    /**
     * Server entry point.
     *
     * @param args optional: port number
     */
    public static void main(String[] args) {
        ServerConfig config = new ServerConfig();
        if (args.length > 0) {
            config.set("server.port", args[0]);
        }
        DRSServer server = new DRSServer(config);
        try {
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            server.acceptThread.join();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot connect to MySQL at " + config.getDbHost() + ":" + config.getDbPort()
                    + " as '" + config.getDbUser() + "'. Check src/main/resources/drs/server/server.properties.", e);
            System.exit(1);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot listen on port " + config.getPort(), e);
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
