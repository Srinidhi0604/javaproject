import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Data Access Object for 'catalog_items' table.
 */
public class CatalogItemDAO {

    /**
     * Retrieves catalog items matching filters, including averages from reviews.
     */
    public static String getCatalogJson(String city, String category, String genre, String search, int userId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT c.*, v.name AS venueName, ci.name AS venueCity, v.type AS venueType, " +
            "(SELECT AVG(r.rating) FROM reviews r WHERE r.catalog_item_id = c.id) AS avgRating, " +
            "(SELECT COUNT(*) FROM reviews r WHERE r.catalog_item_id = c.id) AS reviewCount, " +
            "(SELECT COUNT(*) FROM wishlist w WHERE w.user_id = ? AND w.catalog_item_id = c.id) > 0 AS wishlisted " +
            "FROM catalog_items c " +
            "JOIN venues v ON c.venue_id = v.id " +
            "JOIN cities ci ON v.city_id = ci.id " +
            "WHERE 1=1"
        );

        ArrayList<Object> params = new ArrayList<>();
        params.add(userId);

        if (city != null && !city.isEmpty()) {
            sql.append(" AND LOWER(ci.name) = ?");
            params.add(city.toLowerCase());
        }
        if (category != null && !category.isEmpty() && !"All".equalsIgnoreCase(category)) {
            sql.append(" AND c.category = ?");
            params.add(category);
        }
        if (genre != null && !genre.isEmpty() && !"All".equalsIgnoreCase(genre)) {
            sql.append(" AND c.genre_or_cuisine = ?");
            params.add(genre);
        }
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (LOWER(c.title) LIKE ? OR LOWER(c.genre_or_cuisine) LIKE ? OR LOWER(v.name) LIKE ? OR LOWER(c.description) LIKE ?)");
            String term = "%" + search.toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToJson(rs);
            }
        }
    }
    /**
     * Adds a new catalog item. Returns generated key.
     */
    public static int addCatalogItem(int venueId, String category, String title, String description, String genre, int duration, java.math.BigDecimal price) throws SQLException {
        String sql = "INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, venueId);
            pstmt.setString(2, category);
            pstmt.setString(3, title);
            pstmt.setString(4, description);
            pstmt.setString(5, genre);
            pstmt.setInt(6, duration);
            pstmt.setBigDecimal(7, price);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Gets recommended items for a user.
     */
    public static String getRecommendedJson(int userId) throws SQLException {
        String sql = "SELECT c.*, v.name AS venueName, ci.name AS venueCity, " +
                     "(SELECT AVG(r.rating) FROM reviews r WHERE r.catalog_item_id = c.id) AS avgRating " +
                     "FROM catalog_items c " +
                     "JOIN venues v ON c.venue_id = v.id " +
                     "JOIN cities ci ON v.city_id = ci.id " +
                     "WHERE c.category IN (" +
                     "  SELECT c2.category FROM catalog_items c2 " +
                     "  JOIN bookings b ON b.catalog_item_id = c2.id " +
                     "  WHERE b.user_id = ? " +
                     "  GROUP BY c2.category " +
                     "  ORDER BY COUNT(*) DESC LIMIT 2" +
                     ") AND c.id NOT IN (SELECT catalog_item_id FROM bookings WHERE user_id = ?) " +
                     "LIMIT 4";

        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToJson(rs);
            }
        }
    }

    /**
     * Gets trending items based on recent bookings.
     */
    public static String getTrendingJson() throws SQLException {
        String sql = "SELECT c.*, v.name AS venueName, ci.name AS venueCity, COUNT(b.id) AS booking_count, " +
                     "(SELECT AVG(r.rating) FROM reviews r WHERE r.catalog_item_id = c.id) AS avgRating " +
                     "FROM catalog_items c " +
                     "JOIN venues v ON c.venue_id = v.id " +
                     "JOIN cities ci ON v.city_id = ci.id " +
                     "LEFT JOIN bookings b ON b.catalog_item_id = c.id AND b.created_at >= datetime('now', '-7 days') " +
                     "GROUP BY c.id " +
                     "ORDER BY booking_count DESC LIMIT 4";

        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return JSONMapper.mapResultSetToJson(rs);
        }
    }

    /**
     * Gets a single catalog item detail.
     */
    public static String getItemDetailsJson(int id) throws SQLException {
        String sql = "SELECT * FROM catalog_items WHERE id = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToSingleObject(rs);
            }
        }
    }
}
