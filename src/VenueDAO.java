import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'venues' table.
 */
public class VenueDAO {

    /**
     * Confirms if a user is the registered owner of a venue.
     */
    public static boolean isVenueOwner(int venueId, int ownerUserId) throws SQLException {
        String sql = "SELECT 1 FROM venues WHERE id = ? AND owner_user_id = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, venueId);
            pstmt.setInt(2, ownerUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Gets all venues from database as a JSON array.
     */
    public static String getAllVenuesJson() throws SQLException {
        String sql = "SELECT v.*, c.name AS city_name FROM venues v JOIN cities c ON v.city_id = c.id";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return JSONMapper.mapResultSetToJson(rs);
        }
    }
}
