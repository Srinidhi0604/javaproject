import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'booking_seats' table.
 */
public class BookingSeatDAO {

    /**
     * Bulk inserts booking seat assignments inside transaction.
     */
    public static void addBookingSeats(Connection conn, String bookingId, String[] seatLabels) throws SQLException {
        String sql = "INSERT INTO booking_seats (booking_id, seat_label) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String seat : seatLabels) {
                if (seat != null && !seat.trim().isEmpty()) {
                    pstmt.setString(1, bookingId);
                    pstmt.setString(2, seat.trim());
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
        }
    }

    /**
     * Gets all seats associated with a booking.
     */
    public static String getBookingSeats(String bookingId) throws SQLException {
        String sql = "SELECT seat_label FROM booking_seats WHERE booking_id = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToJson(rs);
            }
        }
    }
}
