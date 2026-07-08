/**
 * Exception thrown when a coupon code is invalid, expired, or has reached usage limits.
 */
public class CouponInvalidException extends Exception {
    public CouponInvalidException(String message) {
        super(message);
    }
}
