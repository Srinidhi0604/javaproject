import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'wishlist' table.
 */
public class WishlistDAO {

    /**
     * Toggles wishlist status. Returns true if added, false if removed.
     */
    public static boolean toggleWishlist(int userId, int catalogItemId) throws SQLException {
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection()) {
            Connection conn = holder.getConnection();
            conn.setAutoCommit(false);
            try {
                String checkSql = "SELECT 1 FROM wishlist WHERE user_id = ? AND catalog_item_id = ?";
                boolean exists = false;
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, catalogItemId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) exists = true;
                    }
                }

                if (exists) {
                    String deleteSql = "DELETE FROM wishlist WHERE user_id = ? AND catalog_item_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                        pstmt.setInt(1, userId);
                        pstmt.setInt(2, catalogItemId);
                        pstmt.executeUpdate();
                    }
                    conn.commit();
                    return false; // removed
                } else {
                    String insertSql = "INSERT INTO wishlist (user_id, catalog_item_id) VALUES (?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        pstmt.setInt(1, userId);
                        pstmt.setInt(2, catalogItemId);
                        pstmt.executeUpdate();
                    }
                    conn.commit();
                    return true; // added
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Gets wishlisted items of a user.
     */
    public static String getWishlistJson(int userId) throws SQLException {
        String sql = "SELECT c.*, v.name AS venue_name, ci.name AS venue_city " +
                     "FROM catalog_items c " +
                     "JOIN wishlist w ON w.catalog_item_id = c.id " +
                     "JOIN venues v ON c.venue_id = v.id " +
                     "JOIN cities ci ON v.city_id = ci.id " +
                     "WHERE w.user_id = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToJson(rs);
            }
        }
    }
}
