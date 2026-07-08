/**
 * Thrown when a booking ID does not exist in the system.
 */
public class BookingNotFoundException extends Exception {
    public BookingNotFoundException(String message) {
        super(message);
    }
}
