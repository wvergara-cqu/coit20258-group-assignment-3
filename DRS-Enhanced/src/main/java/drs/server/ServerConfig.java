package drs.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads server settings. Precedence (lowest to highest): bundled
 * /drs/server/server.properties, an optional drs-server.properties file in the
 * working directory, then JVM system properties (e.g. -Ddb.password=secret).
 */
public class ServerConfig {

    private final Properties props = new Properties();

    /** Loads configuration from all sources. */
    public ServerConfig() {
        try (InputStream in = ServerConfig.class.getResourceAsStream("/drs/server/server.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bundled server.properties", e);
        }
        Path external = Path.of("drs-server.properties");
        if (Files.exists(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read drs-server.properties", e);
            }
        }
        for (String key : props.stringPropertyNames()) {
            String override = System.getProperty(key);
            if (override != null) {
                props.setProperty(key, override);
            }
        }
    }

    /** @return the value for key, or the default */
    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /** @return the integer value for key, or the default */
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Overrides a value at runtime (used by integration tests). */
    public void set(String key, String value) {
        props.setProperty(key, value);
    }

    public int getPort() { return getInt("server.port", 5050); }
    public int getThreads() { return getInt("server.threads", 20); }
    public String getDbHost() { return get("db.host", "localhost"); }
    public int getDbPort() { return getInt("db.port", 3306); }
    public String getDbName() { return get("db.name", "drs_enhanced"); }
    public String getDbUser() { return get("db.user", "root"); }
    public String getDbPassword() { return get("db.password", ""); }
    public int getDbPoolSize() { return getInt("db.poolSize", 10); }
    public String getAesKey() { return get("security.aesKey", ""); }

    /** @return JDBC URL of the MySQL server without a schema (used to CREATE DATABASE) */
    public String getServerJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/?%s", getDbHost(), getDbPort(), jdbcOptions());
    }

    /** @return JDBC URL of the DRS schema */
    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?%s", getDbHost(), getDbPort(), getDbName(), jdbcOptions());
    }

    // session time zone forced to UTC so MySQL NOW() matches the stored timestamps
    // (otherwise warnings expired straight away on machines not set to UTC)
    private String jdbcOptions() {
        return "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                + "&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8";
    }
}