package library;

import java.util.List;

/** Immutable snapshot: the service replaces this only after a successful save. */
public record LibraryState(List<Book> books, List<Member> members, List<Loan> loans) {
    public LibraryState {
        books = List.copyOf(books);
        members = List.copyOf(members);
        loans = List.copyOf(loans);
    }
    public static LibraryState empty() { return new LibraryState(List.of(), List.of(), List.of()); }
}
