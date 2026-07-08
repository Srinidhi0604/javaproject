/**
 * Exception thrown when a payment gateway mock simulation fails or is declined.
 */
public class PaymentFailedException extends Exception {
    public PaymentFailedException(String message) {
        super(message);
    }
}
