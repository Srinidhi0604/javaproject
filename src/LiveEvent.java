import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Represents a Live Event (concert, stand-up comedy, theater plays) with tiered pricing structure.
 */
public class LiveEvent extends Entertainment {
    private BigDecimal basePrice; // default / Silver tier price

    public LiveEvent(int id, String title, String category, String genre, int durationMinutes, int venueId, String description, double basePrice) {
        super(id, title, category, genre, durationMinutes, venueId, description);
        this.basePrice = BigDecimal.valueOf(basePrice);
    }

    @Override
    public BigDecimal getBasePrice() {
        return basePrice;
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
        sb.append("\"category\":\"").append(getCategory()).append("\",");
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
