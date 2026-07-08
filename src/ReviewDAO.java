import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'reviews' table.
 */
public class ReviewDAO {

    /**
     * Adds a review transactionally and recomputes the catalog item's aggregate rating.
     */
    public static void addReview(int userId, int catalogItemId, String bookingId, int rating, String comment) throws SQLException {
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection()) {
            Connection conn = holder.getConnection();
            conn.setAutoCommit(false);

            try {
                // 1. Write review log
                String insertSql = "INSERT INTO reviews (user_id, catalog_item_id, booking_id, rating, comment) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, catalogItemId);
                    pstmt.setString(3, bookingId);
                    pstmt.setInt(4, rating);
                    pstmt.setString(5, comment);
                    pstmt.executeUpdate();
                }

                // 2. Recompute aggregate average
                double avg = 0.0;
                String avgSql = "SELECT AVG(rating) FROM reviews WHERE catalog_item_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(avgSql)) {
                    pstmt.setInt(1, catalogItemId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            avg = rs.getDouble(1);
                        }
                    }
                }

                // 3. Update catalog item aggregate average rating cache field
                String updateSql = "UPDATE catalog_items SET rating_avg = ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setDouble(1, avg);
                    pstmt.setInt(2, catalogItemId);
                    pstmt.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Review transaction rolled back: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Gets reviews list of an item as a JSON array.
     */
    public static String getReviewsJson(int catalogItemId) throws SQLException {
        String sql = "SELECT r.*, u.username FROM reviews r JOIN users u ON r.user_id = u.id WHERE r.catalog_item_id = ? ORDER BY r.created_at DESC";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, catalogItemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToJson(rs);
            }
        }
    }
}
