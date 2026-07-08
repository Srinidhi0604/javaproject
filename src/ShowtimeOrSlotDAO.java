import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'showtimes_or_slots' table.
 */
public class ShowtimeOrSlotDAO {

    /**
     * Gets all slots of a catalog item.
     */
    public static String getSlotsJson(int catalogItemId) throws SQLException {
        String sql = "SELECT * FROM showtimes_or_slots WHERE catalog_item_id = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, catalogItemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToJson(rs);
            }
        }
    }

    /**
     * Decrements the available capacity of a slot, executing inside an active transaction.
     * Throws CapacityExceededException if requested quantity exceeds capacity.
     */
    public static void decrementCapacity(Connection conn, int slotId, int quantity) throws SQLException, CapacityExceededException {
        // Concurrency control: Lock the row using SQLite transactions or SELECT query check
        String selectSql = "SELECT available_capacity FROM showtimes_or_slots WHERE id = ?";
        int available = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setInt(1, slotId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    available = rs.getInt("available_capacity");
                } else {
                    throw new SQLException("Time slot ID " + slotId + " does not exist.");
                }
            }
        }

        if (quantity > available) {
            throw new CapacityExceededException("Capacity exceeded. Requested: " + quantity + ", Available: " + available);
        }

        String updateSql = "UPDATE showtimes_or_slots SET available_capacity = available_capacity - ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, slotId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Increments available capacity (e.g. on cancellation).
     */
    public static void incrementCapacity(Connection conn, int slotId, int quantity) throws SQLException {
        String sql = "UPDATE showtimes_or_slots SET available_capacity = available_capacity + ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, slotId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Adds showtimes/slots in bulk using Batch Operations.
     */
    public static void addBatchShowtimes(int catalogItemId, String[] startTimes, int capacity) throws SQLException {
        String sql = "INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (?, ?, ?, ?)";
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection()) {
            Connection conn = holder.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (String time : startTimes) {
                    if (!time.trim().isEmpty()) {
                        pstmt.setInt(1, catalogItemId);
                        pstmt.setString(2, time.trim());
                        pstmt.setInt(3, capacity);
                        pstmt.setInt(4, capacity);
                        pstmt.addBatch();
                    }
                }
                pstmt.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
