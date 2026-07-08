import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'notifications' table.
 */
public class NotificationDAO {

    /**
     * Inserts a notification with +10 seconds send time relative to current SQLite time.
     */
    public static void createNotification(Connection conn, int userId, String bookingId, String message) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, booking_id, message, send_at, sent) " +
                     "VALUES (?, ?, ?, datetime('now', '+10 seconds'), 0)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, bookingId);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        }
    }

    /**
     * Polls pending notification reminders.
     */
    public static String getPendingNotifications() throws SQLException {
        String sql = "SELECT n.id, n.booking_id, n.message, u.username " +
                     "FROM notifications n " +
                     "JOIN users u ON n.user_id = u.id " +
                     "WHERE n.send_at <= datetime('now') AND n.sent = 0";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return JSONMapper.mapResultSetToJson(rs);
        }
    }

    /**
     * Marks notifications as sent.
     */
    public static void markAsSent(int notificationId) throws SQLException {
        String sql = "UPDATE notifications SET sent = 1 WHERE id = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Gets sent notifications for a user to display on UI toasts.
     */
    public static String getUserSentNotificationsJson(int userId) throws SQLException {
        String sql = "SELECT message, booking_id, send_at FROM notifications WHERE user_id = ? AND sent = 1 ORDER BY send_at DESC LIMIT 3";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToJson(rs);
            }
        }
    }
}
