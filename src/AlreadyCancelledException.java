/**
 * Thrown when trying to cancel an already-cancelled booking.
 */
public class AlreadyCancelledException extends Exception {
    public AlreadyCancelledException(String message) {
        super(message);
    }
}
