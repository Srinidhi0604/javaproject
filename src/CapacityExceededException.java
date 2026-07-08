/**
 * Exception thrown when a booking reservation exceeds remaining time slot capacities.
 */
public class CapacityExceededException extends Exception {
    public CapacityExceededException(String message) {
        super(message);
    }
}
