import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'pricing_rules' table.
 */
public class PricingRuleDAO {

    /**
     * Retrieves surge multiplier for a slot on a specific weekday.
     */
    public static double getSurgeMultiplier(int catalogItemId, String dayOfWeek) throws SQLException {
        String sql = "SELECT multiplier FROM pricing_rules WHERE catalog_item_id = ? AND day_of_week = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, catalogItemId);
            pstmt.setString(2, dayOfWeek);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("multiplier");
                }
            }
        }
        return 1.0; // default multiplier (no surge)
    }
}
