import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages database connections using a simple, thread-safe hand-rolled connection pool
 * with read/write replica separation. All connections are initialized with high-concurrency WAL mode.
 */
public class DatabaseManager {
    private static String dbUrl = "jdbc:sqlite:district.db";
    private static final int POOL_SIZE = 10;

    private static final BlockingQueue<Connection> writePool = new LinkedBlockingQueue<>(POOL_SIZE);
    private static final BlockingQueue<Connection> readPool = new LinkedBlockingQueue<>(POOL_SIZE);

    static {
        // Load externalized database config properties
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = new java.io.FileInputStream("config.properties")) {
            props.load(is);
            String url = props.getProperty("db.url");
            if (url != null && !url.trim().isEmpty()) {
                dbUrl = url.trim();
            }
        } catch (java.io.IOException e) {
            System.out.println("[DatabaseManager] config.properties loading skipped. Using default URL: " + dbUrl);
        }

        try {
            // Load driver
            Class.forName("org.sqlite.JDBC");
            
            // Populate write and read pools
            for (int i = 0; i < POOL_SIZE; i++) {
                writePool.add(createConnection(false));
                readPool.add(createConnection(true));
            }
            System.out.println("[DatabaseManager] Connection pool initialized with size " + POOL_SIZE + " per replica pool.");
        } catch (Exception e) {
            System.err.println("Fatal error initializing connection pools: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database pool initialization failed", e);
        }
    }

    /**
     * Creates a connection to SQLite database, configuring concurrency parameters.
     */
    private static Connection createConnection(boolean readOnly) throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        
        // Optimize SQLite concurrency settings
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;"); // Write-Ahead Logging
            stmt.execute("PRAGMA busy_timeout=5000;"); // Timeout for locked DB
        }
        
        if (readOnly) {
            // SQLite read-only mode can be emulated or set via query-only PRAGMA
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA query_only=ON;");
            }
        }
        return conn;
    }

    /**
     * Borrows a write connection from the pool, wrapped in a ConnectionHolder.
     */
    public static ConnectionHolder acquireWriteConnection() throws SQLException {
        try {
            Connection conn = writePool.take();
            return new ConnectionHolder(conn, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Database connection acquisition interrupted", e);
        }
    }

    /**
     * Releases a write connection back to the pool.
     */
    public static void releaseWriteConnection(Connection conn) {
        if (conn != null) {
            writePool.offer(conn);
        }
    }

    /**
     * Borrows a read connection from the pool, wrapped in a ConnectionHolder.
     */
    public static ConnectionHolder acquireReadConnection() throws SQLException {
        try {
            Connection conn = readPool.take();
            return new ConnectionHolder(conn, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Database connection acquisition interrupted", e);
        }
    }

    /**
     * Releases a read connection back to the pool.
     */
    public static void releaseReadConnection(Connection conn) {
        if (conn != null) {
            readPool.offer(conn);
        }
    }

    /**
     * Legacy helper method matching the original setup. Runs on read connection.
     */
    public static Connection getConnection() throws SQLException {
        try {
            return readPool.take(); // borrowed, must be manually returned via releaseReadConnection!
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Database connection acquisition interrupted", e);
        }
    }
}
