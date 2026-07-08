import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'bookings' table.
 * Uses BigDecimal for all monetary calculations and handles transaction outcome audits.
 */
public class BookingDAO {

    /**
     * Executes atomic multi-step transaction for booking creation.
     */
    public static void createBookingTransaction(
        String bookingId, int userId, int catalogItemId, int slotId, int quantity,
        BigDecimal totalCost, String[] seatLabels, String couponCode, boolean redeemWallet, BigDecimal walletBurn
    ) throws SQLException, CapacityExceededException, CouponInvalidException {

        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection()) {
            Connection conn = holder.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            try {
                // 1. Decrement time slot capacity (locks & validates)
                ShowtimeOrSlotDAO.decrementCapacity(conn, slotId, quantity);

                // 2. Insert Booking entry
                String insBookingSql = "INSERT INTO bookings (id, user_id, catalog_item_id, slot_id, quantity, total_cost, status) " +
                                       "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";
                try (PreparedStatement pstmt = conn.prepareStatement(insBookingSql)) {
                    pstmt.setString(1, bookingId);
                    pstmt.setInt(2, userId);
                    pstmt.setInt(3, catalogItemId);
                    pstmt.setInt(4, slotId);
                    pstmt.setInt(5, quantity);
                    pstmt.setBigDecimal(6, totalCost);
                    pstmt.executeUpdate();
                }

                // 3. Insert Booking seats
                if (seatLabels != null && seatLabels.length > 0) {
                    BookingSeatDAO.addBookingSeats(conn, bookingId, seatLabels);
                }

                // 4. Coupon Redemption
                if (couponCode != null && !couponCode.trim().isEmpty()) {
                    int couponId = CouponDAO.getCouponIdByCode(conn, couponCode);
                    if (couponId != -1) {
                        CouponRedemptionDAO.redeemCoupon(conn, couponId, userId, bookingId);
                    }
                }

                // 5. Wallet points burning
                if (redeemWallet && walletBurn.compareTo(BigDecimal.ZERO) > 0) {
                    UserDAO.updateWalletBalance(conn, userId, walletBurn.negate());
                    WalletLedgerDAO.addLedgerEntry(conn, userId, walletBurn.negate(), "BURN", bookingId);
                }

                // 6. Create Payment entry as PENDING
                PaymentDAO.createPayment(conn, bookingId, totalCost, "PENDING", "UPI");

                conn.commit(); // Commit all steps atomically
                TransactionLogger.logCommit("CreateBooking", "BookingID: " + bookingId + " | Cost: " + totalCost);
            } catch (Exception e) {
                conn.rollback(); // Rollback complete block on any error
                TransactionLogger.logRollback("CreateBooking", "BookingID: " + bookingId, e);
                if (e instanceof CapacityExceededException) throw (CapacityExceededException) e;
                if (e instanceof CouponInvalidException) throw (CouponInvalidException) e;
                throw new SQLException("Transaction aborted: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Executes atomic cancellation transaction.
     */
    public static void cancelBookingTransaction(String bookingId) throws SQLException, BookingNotFoundException, AlreadyCancelledException {
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection()) {
            Connection conn = holder.getConnection();
            conn.setAutoCommit(false);

            try {
                // Fetch booking details
                String bSql = "SELECT user_id, slot_id, quantity, total_cost, status FROM bookings WHERE id = ?";
                int userId = -1;
                int slotId = -1;
                int qty = 0;
                BigDecimal cost = BigDecimal.ZERO;
                String status = "";

                try (PreparedStatement pstmt = conn.prepareStatement(bSql)) {
                    pstmt.setString(1, bookingId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("user_id");
                            slotId = rs.getInt("slot_id");
                            qty = rs.getInt("quantity");
                            cost = rs.getBigDecimal("total_cost");
                            status = rs.getString("status");
                        } else {
                            throw new BookingNotFoundException("Booking ID " + bookingId + " not found.");
                        }
                    }
                }

                if ("CANCELLED".equalsIgnoreCase(status)) {
                    throw new AlreadyCancelledException("Booking has already been cancelled.");
                }

                // 1. Restore slot capacity
                ShowtimeOrSlotDAO.incrementCapacity(conn, slotId, qty);

                // 2. Set booking to CANCELLED
                String updBook = "UPDATE bookings SET status = 'CANCELLED' WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updBook)) {
                    pstmt.setString(1, bookingId);
                    pstmt.executeUpdate();
                }

                // 3. Set payment to REFUNDED
                String updPay = "UPDATE payments SET status = 'REFUNDED' WHERE booking_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updPay)) {
                    pstmt.setString(1, bookingId);
                    pstmt.executeUpdate();
                }

                // 4. Restore spent wallet points if any
                String wlSql = "SELECT amount FROM wallet_ledger WHERE reference_booking_id = ? AND type = 'BURN'";
                BigDecimal pointsSpent = BigDecimal.ZERO;
                try (PreparedStatement pstmt = conn.prepareStatement(wlSql)) {
                    pstmt.setString(1, bookingId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            pointsSpent = rs.getBigDecimal("amount").abs();
                        }
                    }
                }

                if (pointsSpent.compareTo(BigDecimal.ZERO) > 0) {
                    UserDAO.updateWalletBalance(conn, userId, pointsSpent);
                    WalletLedgerDAO.addLedgerEntry(conn, userId, pointsSpent, "EARN", bookingId);
                }

                // 5. Revert points gained from confirmation
                String earnSql = "SELECT amount FROM wallet_ledger WHERE reference_booking_id = ? AND type = 'EARN'";
                BigDecimal pointsEarned = BigDecimal.ZERO;
                try (PreparedStatement pstmt = conn.prepareStatement(earnSql)) {
                    pstmt.setString(1, bookingId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            pointsEarned = rs.getBigDecimal("amount");
                        }
                    }
                }

                if (pointsEarned.compareTo(BigDecimal.ZERO) > 0) {
                    UserDAO.updateWalletBalance(conn, userId, pointsEarned.negate());
                    WalletLedgerDAO.addLedgerEntry(conn, userId, pointsEarned.negate(), "BURN", bookingId);
                }

                conn.commit();
                TransactionLogger.logCommit("CancelBooking", "BookingID: " + bookingId + " | Reverted points: " + pointsSpent);
            } catch (Exception e) {
                conn.rollback();
                TransactionLogger.logRollback("CancelBooking", "BookingID: " + bookingId, e);
                if (e instanceof BookingNotFoundException) throw (BookingNotFoundException) e;
                if (e instanceof AlreadyCancelledException) throw (AlreadyCancelledException) e;
                throw new SQLException("Cancellation transaction aborted: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Aggregates revenue stats by category and city for admin analytics.
     */
    public static String getAnalyticsJson() throws SQLException {
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection()) {
            Connection conn = holder.getConnection();
            
            // Category Revenue Aggregates
            String catSql = "SELECT c.category, SUM(b.total_cost) AS total_revenue " +
                            "FROM bookings b " +
                            "JOIN catalog_items c ON b.catalog_item_id = c.id " +
                            "WHERE b.status = 'CONFIRMED' " +
                            "GROUP BY c.category";
            String catJson;
            try (PreparedStatement pstmt = conn.prepareStatement(catSql);
                 ResultSet rs = pstmt.executeQuery()) {
                catJson = JSONMapper.mapResultSetToJson(rs);
            }

            // City Revenue Aggregates
            String citySql = "SELECT ci.name AS city, SUM(b.total_cost) AS total_revenue " +
                             "FROM bookings b " +
                             "JOIN catalog_items c ON b.catalog_item_id = c.id " +
                             "JOIN venues v ON c.venue_id = v.id " +
                             "JOIN cities ci ON v.city_id = ci.id " +
                             "WHERE b.status = 'CONFIRMED' " +
                             "GROUP BY ci.name";
            String cityJson;
            try (PreparedStatement pstmt = conn.prepareStatement(citySql);
                 ResultSet rs = pstmt.executeQuery()) {
                cityJson = JSONMapper.mapResultSetToJson(rs);
            }

            return "{\"revenueByCategory\":" + catJson + ",\"revenueByCity\":" + cityJson + "}";
        }
    }

    /**
     * Gets booking logs matching user role filters.
     */
    public static String getBookingsJson(int userId, String role) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT b.id AS bookingID, b.quantity AS seats, b.total_cost AS totalCost, b.status, b.created_at, " +
            "c.title AS movieTitle, c.category, s.start_time AS showtime, u.username AS customerName " +
            "FROM bookings b " +
            "JOIN catalog_items c ON b.catalog_item_id = c.id " +
            "JOIN showtimes_or_slots s ON b.slot_id = s.id " +
            "JOIN users u ON b.user_id = u.id " +
            "JOIN venues v ON c.venue_id = v.id"
        );

        if ("VENUE_PARTNER".equalsIgnoreCase(role)) {
            sql.append(" WHERE v.owner_user_id = ?");
        } else if ("CUSTOMER".equalsIgnoreCase(role)) {
            sql.append(" WHERE b.user_id = ?");
        } else {
            sql.append(" WHERE 1=1"); // ADMIN
        }
        
        sql.append(" ORDER BY b.created_at DESC");

        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql.toString())) {
            if ("VENUE_PARTNER".equalsIgnoreCase(role) || "CUSTOMER".equalsIgnoreCase(role)) {
                pstmt.setInt(1, userId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return JSONMapper.mapResultSetToJson(rs);
            }
        }
    }
}
