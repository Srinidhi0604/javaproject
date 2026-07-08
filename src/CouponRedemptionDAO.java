import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Data Access Object for 'coupon_redemptions' table.
 */
public class CouponRedemptionDAO {

    /**
     * Records a coupon redemption event inside an active transaction.
     */
    public static void redeemCoupon(Connection conn, int couponId, int userId, String bookingId) throws SQLException {
        String sql = "INSERT INTO coupon_redemptions (coupon_id, user_id, booking_id) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, couponId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, bookingId);
            pstmt.executeUpdate();
        }
    }
}
