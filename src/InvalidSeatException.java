/**
 * Thrown when requested seats exceed available seats or is <= 0.
 */
public class InvalidSeatException extends Exception {
    public InvalidSeatException(String message) {
        super(message);
    }
}
