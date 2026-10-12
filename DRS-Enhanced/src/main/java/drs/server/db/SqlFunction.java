package drs.server.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A unit of database work executed with a pooled connection.
 *
 * @param <T> result type
 */
@FunctionalInterface
public interface SqlFunction<T> {

    /**
     * @param connection a borrowed connection (do not close it)
     * @return the result of the work
     * @throws SQLException on database error
     */
    T apply(Connection connection) throws SQLException;
}
