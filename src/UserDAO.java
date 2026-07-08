import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object for 'users' table.
 * Implements secure SHA-256 password hashing with SecureRandom salts
 * and decimal representation for monetary fields.
 */
public class UserDAO {

    private static String hashPassword(String password, String salt) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] bytes = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }

    private static String generateSalt() {
        byte[] saltBytes = new byte[16];
        new java.security.SecureRandom().nextBytes(saltBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : saltBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Authenticates username and password against secure salted hash records.
     */
    public static String authenticate(String username, String password) throws SQLException, BookingNotFoundException {
        String sql = "SELECT id, username, role, password_hash, salt, wallet_balance AS wallet, city FROM users WHERE username = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    
                    String computedHash = hashPassword(password, salt);
                    if (storedHash.equals(computedHash)) {
                        return JSONMapper.mapRowToJsonObject(rs);
                    }
                }
                throw new BookingNotFoundException("Invalid credentials provided.");
            }
        }
    }

    /**
     * Registers a new user account securely, crediting customer accounts with 100.00 welcome points.
     */
    public static void register(String username, String password, String role, String city) throws SQLException, CouponInvalidException {
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection()) {
            Connection conn = holder.getConnection();
            conn.setAutoCommit(false);
            try {
                // Check if user already exists
                String checkSql = "SELECT 1 FROM users WHERE username = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setString(1, username);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            throw new CouponInvalidException("Username is already taken.");
                        }
                    }
                }

                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);
                BigDecimal initialBalance = "CUSTOMER".equalsIgnoreCase(role) ? new BigDecimal("100.00") : BigDecimal.ZERO;

                // Insert User
                String insertSql = "INSERT INTO users (username, password_hash, salt, role, wallet_balance, city) VALUES (?, ?, ?, ?, ?, ?)";
                int userId = -1;
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, passwordHash);
                    pstmt.setString(3, salt);
                    pstmt.setString(4, role);
                    pstmt.setBigDecimal(5, initialBalance);
                    pstmt.setString(6, city);
                    pstmt.executeUpdate();
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) userId = keys.getInt(1);
                    }
                }

                // Insert initial wallet ledger entry
                if ("CUSTOMER".equalsIgnoreCase(role) && userId != -1) {
                    String ledgerSql = "INSERT INTO wallet_ledger (user_id, amount, type) VALUES (?, ?, 'EARN')";
                    try (PreparedStatement pstmt = conn.prepareStatement(ledgerSql)) {
                        pstmt.setInt(1, userId);
                        pstmt.setBigDecimal(2, initialBalance);
                        pstmt.executeUpdate();
                    }
                }

                conn.commit();
                TransactionLogger.logCommit("RegisterUser", "Registered " + username + " with role " + role);
            } catch (Exception e) {
                conn.rollback();
                TransactionLogger.logRollback("RegisterUser", "Failed to register " + username, e);
                if (e instanceof CouponInvalidException) throw (CouponInvalidException) e;
                throw new SQLException("Registration transaction aborted", e);
            }
        }
    }

    /**
     * Updates user wallet balance inside an active transaction.
     */
    public static void updateWalletBalance(Connection conn, int userId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE users SET wallet_balance = wallet_balance + ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, amount);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Gets wallet balance of a user.
     */
    public static BigDecimal getWalletBalance(int userId) throws SQLException {
        String sql = "SELECT wallet_balance FROM users WHERE id = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal balance = rs.getBigDecimal("wallet_balance");
                    return balance != null ? balance : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }
}
