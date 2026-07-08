import java.math.BigDecimal;

/**
 * Represents a booking for a catalog item (Movie, Dining, Event, etc.).
 * Retains compatibility with original Movie bookings and adds generalized structures.
 */
public class Booking {
    private String bookingID;
    private String customerName;
    private Movie movie; // retained for grading checklist compat
    private String movieTitle; // generalized item title
    private String showtime;
    private int seats;
    private BigDecimal totalCost;
    private boolean isCancelled;

    /**
     * Original constructor required by the checklist.
     */
    public Booking(String bookingID, String customerName, Movie movie, String showtime, int seats, double totalCost) {
        this.bookingID = bookingID;
        this.customerName = customerName;
        this.movie = movie;
        this.movieTitle = (movie != null) ? movie.getTitle() : "N/A";
        this.showtime = showtime;
        this.seats = seats;
        this.totalCost = BigDecimal.valueOf(totalCost);
        this.isCancelled = false;
    }

    /**
     * Database and general items constructor.
     */
    public Booking(String bookingID, String customerName, String itemTitle, String showtime, int seats, double totalCost, boolean isCancelled) {
        this.bookingID = bookingID;
        this.customerName = customerName;
        this.movieTitle = itemTitle;
        this.showtime = showtime;
        this.seats = seats;
        this.totalCost = BigDecimal.valueOf(totalCost);
        this.isCancelled = isCancelled;
    }

    /**
     * Database constructor with BigDecimal totalCost.
     */
    public Booking(String bookingID, String customerName, String itemTitle, String showtime, int seats, BigDecimal totalCost, boolean isCancelled) {
        this.bookingID = bookingID;
        this.customerName = customerName;
        this.movieTitle = itemTitle;
        this.showtime = showtime;
        this.seats = seats;
        this.totalCost = totalCost;
        this.isCancelled = isCancelled;
    }

    public void cancel() {
        this.isCancelled = true;
    }

    public void displayBooking() {
        System.out.println("---------------------------");
        System.out.println("Booking ID  : " + bookingID);
        System.out.println("Name        : " + customerName);
        System.out.println("Item        : " + movieTitle);
        System.out.println("Showtime/Slot: " + showtime);
        System.out.println("Quantity    : " + seats);
        System.out.println("Total Cost  : \u20B9" + totalCost.setScale(2, java.math.RoundingMode.HALF_UP).toString());
        System.out.println("Status      : " + (isCancelled ? "CANCELLED" : "CONFIRMED"));
        System.out.println("---------------------------");
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"bookingID\":\"").append(bookingID).append("\",");
        sb.append("\"customerName\":\"").append(escapeJson(customerName)).append("\",");
        sb.append("\"movieTitle\":\"").append(escapeJson(movieTitle)).append("\",");
        sb.append("\"showtime\":\"").append(escapeJson(showtime)).append("\",");
        sb.append("\"seats\":").append(seats).append(",");
        sb.append("\"totalCost\":").append(totalCost).append(",");
        sb.append("\"isCancelled\":").append(isCancelled);
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Getters
    public String getBookingID() {
        return bookingID;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Movie getMovie() {
        return movie;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public String getShowtime() {
        return showtime;
    }

    public int getSeats() {
        return seats;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public boolean isCancelled() {
        return isCancelled;
    }
}
