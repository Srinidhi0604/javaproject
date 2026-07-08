/**
 * Abstract class representing any form of entertainment or catalog item.
 * Implements the Displayable interface.
 */
public abstract class Entertainment implements Displayable {
    private int id;
    private String title;
    private String category; // MOVIE, DINING, EVENT, PLAY, ACTIVITY
    private String genre;    // or cuisine
    private int durationMinutes;
    private int venueId;
    private String description;

    /**
     * Original constructor required by the checklist.
     */
    protected Entertainment(String title, String genre, int durationMinutes) {
        this.title = title;
        this.genre = genre;
        this.durationMinutes = durationMinutes;
    }

    /**
     * Extended constructor to support database identity and attributes.
     */
    protected Entertainment(int id, String title, String category, String genre, int durationMinutes, int venueId, String description) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.genre = genre;
        this.durationMinutes = durationMinutes;
        this.venueId = venueId;
        this.description = description;
    }

    /**
     * Abstract method to get the base price.
     */
    public abstract java.math.BigDecimal getBasePrice();

    /**
     * Abstract method to get JSON representation.
     */
    public abstract String toJson();

    @Override
    public void displayInfo() {
        System.out.println("Category: " + category);
        System.out.println("Title: " + title);
        System.out.println("Genre/Cuisine: " + genre);
        System.out.println("Duration: " + durationMinutes + " mins");
    }

    /**
     * Escapes control characters for JSON serialization.
     */
    protected String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getGenre() {
        return genre;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getVenueId() {
        return venueId;
    }

    public String getDescription() {
        return description;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setVenueId(int venueId) {
        this.venueId = venueId;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
