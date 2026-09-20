package library;

public record Member(String id, String name, String email) {
    @Override public String toString() { return id + " — " + name; }
}
