/**
 * Interface representing elements that can be booked and cancelled,
 * and whose seat availability can be queried.
 */
public interface Bookable {
    /**
     * Books a specific number of seats for a customer.
     * @param customerName Name of the customer.
     * @param seats Number of seats to book.
     * @return Unique booking ID.
     * @throws InvalidSeatException if the seat count is invalid or exceeds available seats.
     */
    String book(String customerName, int seats) throws InvalidSeatException;

    /**
     * Cancels a booking using its unique booking ID.
     * @param bookingID Unique identifier for the booking.
     * @return true if successfully cancelled.
     * @throws BookingNotFoundException if the booking ID is not found.
     * @throws AlreadyCancelledException if the booking was already cancelled.
     */
    boolean cancel(String bookingID) throws BookingNotFoundException, AlreadyCancelledException;

    /**
     * Gets the number of available seats.
     * @return number of available seats.
     */
    int getAvailableSeats();
}
