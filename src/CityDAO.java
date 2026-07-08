import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'cities' table.
 */
public class CityDAO {

    /**
     * Gets all cities from database as a JSON array.
     */
    public static String getAllCitiesJson() throws SQLException {
        String sql = "SELECT * FROM cities ORDER BY name ASC";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return JSONMapper.mapResultSetToJson(rs);
        }
    }
}
