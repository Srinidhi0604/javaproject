import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'wallet_ledger' table.
 * Uses BigDecimal for points history entries.
 */
public class WalletLedgerDAO {

    /**
     * Appends a ledger transaction entry inside an active transaction.
     */
    public static void addLedgerEntry(Connection conn, int userId, BigDecimal amount, String type, String bookingId) throws SQLException {
        String sql = "INSERT INTO wallet_ledger (user_id, amount, type, reference_booking_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setBigDecimal(2, amount);
            pstmt.setString(3, type);
            pstmt.setString(4, bookingId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves full wallet transaction ledger history as a JSON array.
     */
    public static String getWalletHistoryJson(int userId) throws SQLException {
        String sql = "SELECT amount, type, created_at FROM wallet_ledger WHERE user_id = ? ORDER BY created_at DESC";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToJson(rs);
            }
        }
    }
}
