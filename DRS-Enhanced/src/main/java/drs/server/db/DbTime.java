package drs.server.db;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Server-side time stamping helpers. All timestamps are assigned by the server
 * clock (never the client) so that records cannot be back-dated.
 */
public final class DbTime {

    /** Display format used in every table. */
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DbTime() { }

    /** @return the current server time truncated to seconds */
    public static Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    /** @return now plus the given hours */
    public static Timestamp plusHours(int hours) {
        return Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusHours(hours));
    }

    /** @return the timestamp formatted for display, or "" for null */
    public static String format(Timestamp ts) {
        return ts == null ? "" : ts.toLocalDateTime().format(FORMAT);
    }
}
