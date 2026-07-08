import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for 'coupons' table.
 */
public class CouponDAO {

    /**
     * Gets coupon database ID from code within an active transaction.
     */
    public static int getCouponIdByCode(Connection conn, String code) throws SQLException {
        String sql = "SELECT id FROM coupons WHERE code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    /**
     * Retrieves full coupon details.
     */
    public static String getCouponDetailsJson(String code) throws SQLException, CouponInvalidException {
        String sql = "SELECT * FROM coupons WHERE code = ?";
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             PreparedStatement pstmt = holder.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                String result = JSONMapper.mapResultSetToSingleObject(rs);
                if ("{}".equals(result)) {
                    throw new CouponInvalidException("Coupon code '" + code + "' is invalid.");
                }
                return result;
            }
        }
    }

    /**
     * Checks if a user has exceeded coupon redemption limits.
     */
    public static void validateCouponUsage(int userId, String code) throws SQLException, CouponInvalidException {
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection()) {
            Connection conn = holder.getConnection();
            
            // Check if coupon exists
            String sql = "SELECT id, max_redemptions, per_user_limit FROM coupons WHERE code = ?";
            int couponId = -1;
            int maxRedemptions = 0;
            int perUserLimit = 0;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, code);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        couponId = rs.getInt("id");
                        maxRedemptions = rs.getInt("max_redemptions");
                        perUserLimit = rs.getInt("per_user_limit");
                    } else {
                        throw new CouponInvalidException("Invalid coupon code.");
                    }
                }
            }

            // Check overall usage limits
            String totalUsageSql = "SELECT COUNT(*) FROM coupon_redemptions WHERE coupon_id = ?";
            int totalRedeemed = 0;
            try (PreparedStatement pstmt = conn.prepareStatement(totalUsageSql)) {
                pstmt.setInt(1, couponId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) totalRedeemed = rs.getInt(1);
                }
            }

            if (totalRedeemed >= maxRedemptions) {
                throw new CouponInvalidException("Coupon code usage limit has been reached.");
            }

            // Check per-user limit
            String userUsageSql = "SELECT COUNT(*) FROM coupon_redemptions WHERE coupon_id = ? AND user_id = ?";
            int userRedeemed = 0;
            try (PreparedStatement pstmt = conn.prepareStatement(userUsageSql)) {
                pstmt.setInt(1, couponId);
                pstmt.setInt(2, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) userRedeemed = rs.getInt(1);
                }
            }

            if (userRedeemed >= perUserLimit) {
                throw new CouponInvalidException("You have reached the maximum redemption limit for this coupon code.");
            }
        }
    }
}
