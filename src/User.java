
public abstract class User {
    private final int id;
    private final String name;

    protected User(int id, String name) {
        if (id < 0) {
            throw new IllegalArgumentException("ID cannot be negative.");
        }
        this.id = id;
        this.name = requireText("Name", name);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

   
    public abstract void showProfile();

   
    public static String requireText(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        if (value.contains("|") || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException(label + " cannot contain '|' or line breaks.");
        }
        return value.trim();
    }
}