package drs.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

import drs.server.ServerConfig;

/**
 * Creates the DRS database and all tables programmatically (spec 2.4). Every
 * entity table uses an AUTO_INCREMENT primary key so MySQL assigns an ID when a
 * row is saved. All statements are idempotent (IF NOT EXISTS).
 */
public final class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());

    /** DDL for every table, in foreign-key dependency order. */
    static final List<String> DDL = List.of(
            """
            CREATE TABLE IF NOT EXISTS users (
              id            INT AUTO_INCREMENT PRIMARY KEY,
              username      VARCHAR(30)  NOT NULL UNIQUE,
              full_name     VARCHAR(150) NOT NULL,
              role          VARCHAR(20)  NOT NULL,
              password_hash VARCHAR(100) NOT NULL,
              salt          VARCHAR(50)  NOT NULL,
              public_key    TEXT NULL,
              created_at    DATETIME NOT NULL,
              last_login    DATETIME NULL
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS departments (
              id             INT AUTO_INCREMENT PRIMARY KEY,
              name           VARCHAR(100) NOT NULL UNIQUE,
              type           VARCHAR(60)  NOT NULL,
              contact_person VARCHAR(100) NOT NULL,
              phone          VARCHAR(20)  NOT NULL,
              available      BOOLEAN NOT NULL DEFAULT TRUE,
              updated_at     DATETIME NOT NULL
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS responders (
              id            INT AUTO_INCREMENT PRIMARY KEY,
              name          VARCHAR(100) NOT NULL,
              role          VARCHAR(60)  NOT NULL,
              phone         VARCHAR(20)  NOT NULL,
              department_id INT NOT NULL,
              username      VARCHAR(30) NULL UNIQUE,
              available     BOOLEAN NOT NULL DEFAULT TRUE,
              updated_at    DATETIME NOT NULL,
              CONSTRAINT fk_responder_dept FOREIGN KEY (department_id) REFERENCES departments(id)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS disaster_reports (
              id                 INT AUTO_INCREMENT PRIMARY KEY,
              type               VARCHAR(20)  NOT NULL,
              severity           VARCHAR(20)  NOT NULL,
              location           VARCHAR(150) NOT NULL,
              description        TEXT NOT NULL,
              reporter_name_enc  VARCHAR(512) NOT NULL,
              reporter_phone_enc VARCHAR(512) NOT NULL,
              status             VARCHAR(20)  NOT NULL,
              priority           INT NOT NULL,
              reported_by        INT NULL,
              signature          TEXT NULL,
              created_at         DATETIME NOT NULL,
              updated_at         DATETIME NOT NULL,
              INDEX idx_report_status_priority (status, priority),
              CONSTRAINT fk_report_user FOREIGN KEY (reported_by) REFERENCES users(id)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS disaster_responses (
              id         INT AUTO_INCREMENT PRIMARY KEY,
              report_id  INT NOT NULL,
              notes      TEXT NULL,
              created_by INT NULL,
              signature  TEXT NULL,
              created_at DATETIME NOT NULL,
              CONSTRAINT fk_response_report FOREIGN KEY (report_id) REFERENCES disaster_reports(id),
              CONSTRAINT fk_response_user FOREIGN KEY (created_by) REFERENCES users(id)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS response_departments (
              response_id   INT NOT NULL,
              department_id INT NOT NULL,
              PRIMARY KEY (response_id, department_id),
              CONSTRAINT fk_rd_response FOREIGN KEY (response_id) REFERENCES disaster_responses(id),
              CONSTRAINT fk_rd_department FOREIGN KEY (department_id) REFERENCES departments(id)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS response_responders (
              response_id  INT NOT NULL,
              responder_id INT NOT NULL,
              PRIMARY KEY (response_id, responder_id),
              CONSTRAINT fk_rr_response FOREIGN KEY (response_id) REFERENCES disaster_responses(id),
              CONSTRAINT fk_rr_responder FOREIGN KEY (responder_id) REFERENCES responders(id)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS field_tasks (
              id           INT AUTO_INCREMENT PRIMARY KEY,
              response_id  INT NOT NULL,
              report_id    INT NOT NULL,
              responder_id INT NOT NULL,
              instructions TEXT NULL,
              status       VARCHAR(20) NOT NULL,
              created_at   DATETIME NOT NULL,
              updated_at   DATETIME NOT NULL,
              INDEX idx_task_responder_status (responder_id, status),
              CONSTRAINT fk_task_response FOREIGN KEY (response_id) REFERENCES disaster_responses(id),
              CONSTRAINT fk_task_report FOREIGN KEY (report_id) REFERENCES disaster_reports(id),
              CONSTRAINT fk_task_responder FOREIGN KEY (responder_id) REFERENCES responders(id)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS task_status_updates (
              id                 INT AUTO_INCREMENT PRIMARY KEY,
              task_id            INT NOT NULL,
              from_status        VARCHAR(20) NOT NULL,
              to_status          VARCHAR(20) NOT NULL,
              note               VARCHAR(500) NULL,
              updated_by         INT NULL,
              signature          TEXT NULL,
              signature_verified BOOLEAN NOT NULL DEFAULT FALSE,
              updated_at         DATETIME NOT NULL,
              CONSTRAINT fk_update_task FOREIGN KEY (task_id) REFERENCES field_tasks(id),
              CONSTRAINT fk_update_user FOREIGN KEY (updated_by) REFERENCES users(id)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS public_warnings (
              id                 INT AUTO_INCREMENT PRIMARY KEY,
              report_id          INT NOT NULL,
              level              VARCHAR(30)  NOT NULL,
              affected_area      VARCHAR(150) NOT NULL,
              radius_km          INT NOT NULL,
              message            TEXT NOT NULL,
              recommended_action VARCHAR(500) NOT NULL,
              valid_hours        INT NOT NULL,
              status             VARCHAR(20) NOT NULL,
              issued_by          INT NULL,
              signature          TEXT NULL,
              issued_at          DATETIME NOT NULL,
              expires_at         DATETIME NOT NULL,
              INDEX idx_warning_status_expiry (status, expires_at),
              CONSTRAINT fk_warning_report FOREIGN KEY (report_id) REFERENCES disaster_reports(id),
              CONSTRAINT fk_warning_user FOREIGN KEY (issued_by) REFERENCES users(id)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS audit_log (
              id           INT AUTO_INCREMENT PRIMARY KEY,
              user_id      INT NULL,
              username     VARCHAR(30) NOT NULL,
              action       VARCHAR(40) NOT NULL,
              entity       VARCHAR(40) NOT NULL,
              entity_id    INT NOT NULL,
              payload_hash CHAR(64) NULL,
              signature    TEXT NULL,
              signed       BOOLEAN NOT NULL DEFAULT FALSE,
              verified     BOOLEAN NOT NULL DEFAULT FALSE,
              logged_at    DATETIME NOT NULL,
              INDEX idx_audit_time (logged_at)
            ) ENGINE=InnoDB""");

    private DatabaseManager() { }

    /**
     * Creates the database (if missing) and every table (if missing).
     *
     * @throws SQLException if MySQL is unreachable or DDL fails
     */
    public static void initialise(ServerConfig config) throws SQLException {
        String dbName = config.getDbName();
        if (!dbName.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("Illegal database name: " + dbName);
        }
        try (Connection c = DriverManager.getConnection(config.getServerJdbcUrl(),
                config.getDbUser(), config.getDbPassword());
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        try (Connection c = DriverManager.getConnection(config.getJdbcUrl(),
                config.getDbUser(), config.getDbPassword());
             Statement s = c.createStatement()) {
            for (String ddl : DDL) {
                s.executeUpdate(ddl);
            }
        }
        LOG.info("Database '" + dbName + "' and " + DDL.size() + " tables are ready.");
    }

    /**
     * Drops the database. Used only by integration tests on a test schema.
     */
    public static void dropDatabase(ServerConfig config) throws SQLException {
        try (Connection c = DriverManager.getConnection(config.getServerJdbcUrl(),
                config.getDbUser(), config.getDbPassword());
             Statement s = c.createStatement()) {
            s.executeUpdate("DROP DATABASE IF EXISTS " + config.getDbName());
        }
    }
}
