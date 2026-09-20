package library;

/** One catalogue entry; availability is calculated from its active loans. */
public record Book(String id, String title, String author, int copies) {
    @Override public String toString() { return id + " — " + title; }
}
