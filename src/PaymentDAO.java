import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'payments' table.
 * Uses BigDecimal for all currency representations and processes audits on database transactions.
 */
public class PaymentDAO {

    /**
     * Creates a payment entry.
     */
    public static void createPayment(Connection conn, String bookingId, BigDecimal amount, String status, String method) throws SQLException {
        String sql = "INSERT INTO payments (booking_id, amount, status, method) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bookingId);
            pstmt.setBigDecimal(2, amount);
            pstmt.setString(3, status);
            pstmt.setString(4, method);
            pstmt.executeUpdate();
        }
    }

    /**
     * Executes atomic transaction to confirm or decline payments.
     */
    public static void confirmPaymentTransaction(String bookingId, boolean success) throws SQLException, PaymentFailedException {
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection()) {
            Connection conn = holder.getConnection();
            conn.setAutoCommit(false);

            try {
                if (success) {
                    // 1. Set payment to SUCCESS
                    String paySql = "UPDATE payments SET status = 'SUCCESS' WHERE booking_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(paySql)) {
                        pstmt.setString(1, bookingId);
                        pstmt.executeUpdate();
                    }

                    // 2. Set booking to CONFIRMED
                    String bookSql = "UPDATE bookings SET status = 'CONFIRMED' WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(bookSql)) {
                        pstmt.setString(1, bookingId);
                        pstmt.executeUpdate();
                    }

                    // Fetch user & cost for loyalty points allocation
                    int userId = -1;
                    BigDecimal cost = BigDecimal.ZERO;
                    String getSql = "SELECT user_id, total_cost FROM bookings WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(getSql)) {
                        pstmt.setString(1, bookingId);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                userId = rs.getInt("user_id");
                                cost = rs.getBigDecimal("total_cost");
                            }
                        }
                    }

                    // 3. Award 10% loyalty cashback points
                    BigDecimal pointsEarned = cost.multiply(new BigDecimal("0.10")).setScale(0, java.math.RoundingMode.HALF_UP);
                    if (pointsEarned.compareTo(BigDecimal.ZERO) > 0 && userId != -1) {
                        UserDAO.updateWalletBalance(conn, userId, pointsEarned);
                        WalletLedgerDAO.addLedgerEntry(conn, userId, pointsEarned, "EARN", bookingId);
                    }

                    // 4. Queue Notification reminder
                    String msg = "Your ticket booking " + bookingId + " starts soon!";
                    NotificationDAO.createNotification(conn, userId, bookingId, msg);

                    conn.commit();
                    TransactionLogger.logCommit("ConfirmPayment", "BookingID: " + bookingId + " | Cashback earned: " + pointsEarned);
                } else {
                    // Payment declined: update statuses and revert capacity reservations
                    String paySql = "UPDATE payments SET status = 'FAILED' WHERE booking_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(paySql)) {
                        pstmt.setString(1, bookingId);
                        pstmt.executeUpdate();
                    }

                    String bookSql = "UPDATE bookings SET status = 'FAILED' WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(bookSql)) {
                        pstmt.setString(1, bookingId);
                        pstmt.executeUpdate();
                    }

                    // Revert slot capacity
                    int slotId = -1;
                    int quantity = 0;
                    String getSql = "SELECT slot_id, quantity FROM bookings WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(getSql)) {
                        pstmt.setString(1, bookingId);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                slotId = rs.getInt("slot_id");
                                quantity = rs.getInt("quantity");
                            }
                        }
                    }

                    if (slotId != -1) {
                        ShowtimeOrSlotDAO.incrementCapacity(conn, slotId, quantity);
                    }

                    conn.commit();
                    TransactionLogger.logCommit("DeclinePayment", "BookingID: " + bookingId + " | Capacity reverted");
                    throw new PaymentFailedException("Payment transaction declined by simulated gateway.");
                }
            } catch (Exception e) {
                conn.rollback();
                TransactionLogger.logRollback("ConfirmPayment", "BookingID: " + bookingId, e);
                if (e instanceof PaymentFailedException) throw (PaymentFailedException) e;
                throw new SQLException("Payment confirmation transaction failed: " + e.getMessage(), e);
            }
        }
    }
}
