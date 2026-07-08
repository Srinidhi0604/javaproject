/**
 * Thrown when the selected showtime index or identifier is invalid.
 */
public class ShowtimeNotFoundException extends Exception {
    public ShowtimeNotFoundException(String message) {
        super(message);
    }
}
