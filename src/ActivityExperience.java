import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Represents Slot-based Experiences/Activities (workshops, escape rooms, theme parks).
 */
public class ActivityExperience extends Entertainment {
    private BigDecimal ticketPrice;

    public ActivityExperience(int id, String title, String genre, int durationMinutes, int venueId, String description, double ticketPrice) {
        super(id, title, "ACTIVITY", genre, durationMinutes, venueId, description);
        this.ticketPrice = BigDecimal.valueOf(ticketPrice);
    }

    @Override
    public BigDecimal getBasePrice() {
        return ticketPrice;
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
        sb.append("\"category\":\"ACTIVITY\",");
        sb.append("\"genre\":\"").append(escapeJson(getGenre())).append("\",");
        sb.append("\"durationMinutes\":").append(getDurationMinutes()).append(",");
        sb.append("\"description\":\"").append(escapeJson(getDescription())).append("\",");
        sb.append("\"showtimes\":").append(slotsArray.toString()).append(",");
        sb.append("\"price\":").append(getBasePrice()).append(",");
        sb.append("\"isSpecial\":false");
        sb.append("}");
        return sb.toString();
    }
}
