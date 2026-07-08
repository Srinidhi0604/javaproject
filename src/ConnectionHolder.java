import java.sql.Connection;

/**
 * An AutoCloseable holder that wraps a database connection.
 * When closed, it automatically releases the connection back to the pool in DatabaseManager.
 */
public class ConnectionHolder implements AutoCloseable {
    private final Connection connection;
    private final boolean isWriteConnection;

    public ConnectionHolder(Connection connection, boolean isWriteConnection) {
        this.connection = connection;
        this.isWriteConnection = isWriteConnection;
    }

    /**
     * Gets the underlying database connection.
     */
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        if (isWriteConnection) {
            DatabaseManager.releaseWriteConnection(connection);
        } else {
            DatabaseManager.releaseReadConnection(connection);
        }
    }
}
