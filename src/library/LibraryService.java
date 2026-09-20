package library;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

/** Business rules shared by the user interface and automated tests. */
public final class LibraryService {
    public static final int LOAN_DAYS = 14;
    public static final int MAX_LOANS = 3;
    private final LibraryStore store;
    private final Clock clock;
    private LibraryState state;
    public LibraryService(LibraryStore store, Clock clock) throws IOException {
        this.store = store;
        this.clock = clock;
        state = store.load();
        validateState(state);
    }
    public LibraryState state() { return state; }
    public LocalDate today() { return LocalDate.now(clock); }
    private void commit(LibraryState next) throws IOException {
        validateState(next);
        store.save(next);
        state = next;
    }
    private static String text(String input, String label) {
        if (input == null || input.strip().isEmpty()) throw new IllegalArgumentException(label + " is required.");
        String value = input.strip();
        if (value.length() > 200) throw new IllegalArgumentException(label + " must be 200 characters or fewer.");
        if (value.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(label + " cannot contain control characters.");
        return value;
    }
    private static String email(String input) {
        String value = text(input, "Email").toLowerCase(Locale.ROOT);
        if (!value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Enter an email such as name@example.com.");
        return value;
    }
    private static String nextId(String prefix, List<String> ids) {
        int max = ids.stream().mapToInt(id -> Integer.parseInt(id.substring(1))).max().orElse(0);
        return prefix + String.format(Locale.ROOT, "%04d", Math.addExact(max, 1));
    }
    public Book addBook(String title, String author, int copies) throws IOException {
        title = text(title, "Title"); author = text(author, "Author");
        if (copies < 1 || copies > 999) throw new IllegalArgumentException("Copies must be between 1 and 999.");
        for (Book b : state.books())
            if (b.title().equalsIgnoreCase(title) && b.author().equalsIgnoreCase(author))
                throw new IllegalArgumentException("This title and author already exist. Use Add copies instead.");
        Book book = new Book(nextId("B", state.books().stream().map(Book::id).toList()), title, author, copies);
        var books = new ArrayList<>(state.books()); books.add(book);
        commit(new LibraryState(books, state.members(), state.loans()));
        return book;
    }
    public void addCopies(String bookId, int extra) throws IOException {
        Book book = book(bookId);
        if (extra < 1 || extra > 999 - book.copies())
            throw new IllegalArgumentException("Add at least one copy; total copies cannot exceed 999.");
        var books = state.books().stream().map(b -> b.id().equals(bookId)
                ? new Book(b.id(), b.title(), b.author(), b.copies() + extra) : b).toList();
        commit(new LibraryState(books, state.members(), state.loans()));
    }
    public Member addMember(String name, String address) throws IOException {
        name = text(name, "Member name"); String normalized = email(address);
        if (state.members().stream().anyMatch(m -> m.email().equalsIgnoreCase(normalized)))
            throw new IllegalArgumentException("A member with this email already exists.");
        Member member = new Member(nextId("M", state.members().stream().map(Member::id).toList()), name, normalized);
        var members = new ArrayList<>(state.members()); members.add(member);
        commit(new LibraryState(state.books(), members, state.loans()));
        return member;
    }
    public Book book(String id) {
        return state.books().stream().filter(b -> b.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
    }
    public Member member(String id) {
        return state.members().stream().filter(m -> m.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
    }
    public int available(String bookId) {
        return book(bookId).copies() - (int) state.loans().stream()
                .filter(l -> l.active() && l.bookId().equals(bookId)).count();
    }
    public long activeLoans(String memberId) {
        return state.loans().stream().filter(l -> l.active() && l.memberId().equals(memberId)).count();
    }
    public List<Book> searchBooks(String query) {
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return state.books().stream().filter(b -> (b.id() + " " + b.title() + " " + b.author())
                .toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    public Loan issue(String bookId, String memberId) throws IOException {
        book(bookId); member(memberId);
        if (available(bookId) < 1) throw new IllegalArgumentException("No copies of this book are available.");
        if (activeLoans(memberId) >= MAX_LOANS) throw new IllegalArgumentException("This member already has three active loans.");
        if (state.loans().stream().anyMatch(l -> l.active() && l.bookId().equals(bookId) && l.memberId().equals(memberId)))
            throw new IllegalArgumentException("This member already has this title on loan.");
        Loan loan = new Loan(nextId("L", state.loans().stream().map(Loan::id).toList()), bookId, memberId,
                today(), today().plusDays(LOAN_DAYS), null);
        var loans = new ArrayList<>(state.loans()); loans.add(loan);
        commit(new LibraryState(state.books(), state.members(), loans));
        return loan;
    }
    public void returnLoan(String id) throws IOException {
        Loan loan = state.loans().stream().filter(l -> l.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Loan not found."));
        if (!loan.active()) throw new IllegalArgumentException("This loan has already been returned.");
        if (today().isBefore(loan.issued())) throw new IllegalArgumentException("The computer date is before the issue date.");
        var loans = state.loans().stream().map(l -> l.id().equals(id)
                ? new Loan(l.id(), l.bookId(), l.memberId(), l.issued(), l.due(), today()) : l).toList();
        commit(new LibraryState(state.books(), state.members(), loans));
    }
    /** Checks persisted records too, so malformed data cannot silently change availability. */
    public static void validateState(LibraryState state) {
        Set<String> books = new HashSet<>(), members = new HashSet<>(), loans = new HashSet<>();
        Set<String> emails = new HashSet<>(), titles = new HashSet<>(), pairs = new HashSet<>();
        Map<String, Integer> bookLoans = new HashMap<>(), memberLoans = new HashMap<>();
        for (Book b : state.books()) {
            validId(b.id(), "B", books); text(b.title(), "Title"); text(b.author(), "Author");
            if (b.copies() < 1 || b.copies() > 999) throw new IllegalArgumentException("Invalid copy count");
            if (!titles.add(b.title().toLowerCase(Locale.ROOT) + "\t" + b.author().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Duplicate title and author");
        }
        for (Member m : state.members()) {
            validId(m.id(), "M", members); text(m.name(), "Member name");
            if (!emails.add(email(m.email()))) throw new IllegalArgumentException("Duplicate member email");
        }
        for (Loan l : state.loans()) {
            validId(l.id(), "L", loans);
            if (!books.contains(l.bookId()) || !members.contains(l.memberId()))
                throw new IllegalArgumentException("Loan references a missing book or member");
            if (l.issued() == null || l.due() == null || !l.due().equals(l.issued().plusDays(LOAN_DAYS))
                    || l.returned() != null && l.returned().isBefore(l.issued()))
                throw new IllegalArgumentException("Invalid loan dates");
            if (l.active()) {
                if (!pairs.add(l.bookId() + "\t" + l.memberId())) throw new IllegalArgumentException("Duplicate active loan");
                bookLoans.merge(l.bookId(), 1, Integer::sum);
                memberLoans.merge(l.memberId(), 1, Integer::sum);
            }
        }
        for (Book b : state.books()) if (bookLoans.getOrDefault(b.id(), 0) > b.copies())
            throw new IllegalArgumentException("More loans than copies");
        for (int n : memberLoans.values()) if (n > MAX_LOANS) throw new IllegalArgumentException("Member loan limit exceeded");
    }
    private static void validId(String id, String prefix, Set<String> seen) {
        if (id == null || !id.matches(prefix + "[0-9]{4,9}")
                || Integer.parseInt(id.substring(1)) < 1 || !seen.add(id))
            throw new IllegalArgumentException("Invalid or duplicate " + prefix + " ID");
    }
}
