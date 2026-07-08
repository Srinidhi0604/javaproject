import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Represents a Dining Offer / Restaurant reservation entry.
 */
public class DiningOffer extends Entertainment {
    private BigDecimal bookingFee;

    public DiningOffer(int id, String title, String cuisine, int durationMinutes, int venueId, String description, double bookingFee) {
        super(id, title, "DINING", cuisine, durationMinutes, venueId, description);
        this.bookingFee = BigDecimal.valueOf(bookingFee);
    }

    @Override
    public BigDecimal getBasePrice() {
        return bookingFee;
    }

    @Override
    public String toJson() {
        StringBuilder slotsArray = new StringBuilder("[");
        try {
            String slotsJson = ShowtimeOrSlotDAO.getSlotsJson(getId());
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"start_time\"\\s*:\\s*\"([^\"]+)\"").matcher(slotsJson);
            boolean first = true;
            while (m.find()) {
                if (!first) slotsArray.append(",");
                first = false;
                slotsArray.append("\"").append(escapeJson(m.group(1))).append("\"");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        slotsArray.append("]");

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(getId()).append(",");
        sb.append("\"title\":\"").append(escapeJson(getTitle())).append("\",");
        sb.append("\"category\":\"DINING\",");
        sb.append("\"genre\":\"").append(escapeJson(getGenre())).append("\","); // cuisine
        sb.append("\"durationMinutes\":").append(getDurationMinutes()).append(",");
        sb.append("\"description\":\"").append(escapeJson(getDescription())).append("\",");
        sb.append("\"showtimes\":").append(slotsArray.toString()).append(",");
        sb.append("\"price\":").append(getBasePrice()).append(",");
        sb.append("\"isSpecial\":false");
        sb.append("}");
        return sb.toString();
    }
}
