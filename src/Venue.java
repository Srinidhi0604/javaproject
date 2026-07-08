/**
 * Represents a Venue (e.g., Theatre, Restaurant, Stadium, Arena)
 * where entertainment catalog items are hosted.
 */
public class Venue {
    private int id;
    private String name;
    private String type; // THEATRE, RESTAURANT, STADIUM, ARENA
    private String city;
    private String address;
    private int ownerId;

    public Venue(int id, String name, String type, String city, String address, int ownerId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.city = city;
        this.address = address;
        this.ownerId = ownerId;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public int getOwnerId() {
        return ownerId;
    }
}
