package library;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Dependency-free behaviour tests; all files are created in temporary directories. */
public final class LibraryTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC);
    private static int passed, failed;
    @FunctionalInterface interface Check { void run() throws Exception; }
    static final class MemoryStore implements LibraryStore {
        LibraryState value = LibraryState.empty(); boolean failSave;
        public LibraryState load() { return value; }
        public void save(LibraryState next) throws IOException {
            if (failSave) throw new IOException("Simulated disk failure"); value = next;
        }
    }
    private static LibraryService fresh() throws IOException { return new LibraryService(new MemoryStore(), CLOCK); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void rejects(Check action) throws Exception {
        try { action.run(); } catch (IllegalArgumentException e) { return; }
        throw new AssertionError("Expected validation rejection");
    }
    private static void ioFailure(Check action) throws Exception {
        try { action.run(); } catch (IOException e) { return; }
        throw new AssertionError("Expected storage error");
    }
    private static void test(String id, String description, Check action) {
        try { action.run(); passed++; System.out.println(id + " | PASS | " + description); }
        catch (Exception | AssertionError e) { failed++; System.out.println(id + " | FAIL | " + description + " | " + e); }
    }
    private static Loan lend(LibraryService s) throws IOException {
        return s.issue(s.addBook("Software Engineering", "Ian Sommerville", 1).id(),
                s.addMember("Sample Reader", "reader@example.com").id());
    }
    public static void main(String[] args) throws Exception {
        System.out.println("Library Management System automated test evidence");
        System.out.println("Execution time: " + Instant.now());
        System.out.println("Java runtime: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        System.out.println("Business date fixed at 2026-09-19 for reproducible date assertions.");
        test("T01", "New store starts empty", () -> require(fresh().state().equals(LibraryState.empty()), "not empty"));
        test("T02", "Add book trims fields and assigns ID", () -> {
            var s = fresh(); var b = s.addBook("  Java  ", " Author ", 2);
            require(b.id().equals("B0001") && b.title().equals("Java") && s.available(b.id()) == 2, "book values");
        });
        test("T03", "Reject blank title and author", () -> {
            var s = fresh(); rejects(() -> s.addBook(" ", "A", 1)); rejects(() -> s.addBook("T", "", 1));
        });
        test("T04", "Reject invalid copy counts", () -> {
            var s = fresh(); rejects(() -> s.addBook("T", "A", 0)); rejects(() -> s.addBook("T", "A", -1)); rejects(() -> s.addBook("T", "A", 1000));
        });
        test("T05", "Reject duplicate book regardless of case", () -> {
            var s = fresh(); s.addBook("Java", "Author", 1); rejects(() -> s.addBook(" java ", "AUTHOR", 2));
        });
        test("T06", "Search title author and ID without case sensitivity", () -> {
            var s = fresh(); s.addBook("Java Basics", "Ada Writer", 1); s.addBook("Databases", "Ben", 1);
            require(s.searchBooks("JAVA").size() == 1 && s.searchBooks("ada").size() == 1
                    && s.searchBooks("B0002").size() == 1 && s.searchBooks("").size() == 2
                    && s.searchBooks("missing").isEmpty(), "search result");
        });
        test("T07", "Add copies updates availability with bounds", () -> {
            var s = fresh(); var b = s.addBook("T", "A", 1); s.addCopies(b.id(), 2);
            require(s.available(b.id()) == 3, "copies"); rejects(() -> s.addCopies(b.id(), 0)); rejects(() -> s.addCopies(b.id(), 999));
        });
        test("T08", "Register member and normalise email", () -> {
            var m = fresh().addMember(" Reader ", " Reader@Example.COM ");
            require(m.id().equals("M0001") && m.name().equals("Reader") && m.email().equals("reader@example.com"), "member values");
        });
        test("T09", "Reject blank name and malformed email", () -> {
            var s = fresh(); rejects(() -> s.addMember(" ", "a@example.com")); rejects(() -> s.addMember("A", "invalid"));
            rejects(() -> s.addMember("A", "a b@example.com"));
        });
        test("T10", "Reject duplicate email regardless of case", () -> {
            var s = fresh(); s.addMember("A", "a@example.com"); rejects(() -> s.addMember("B", "A@EXAMPLE.COM"));
        });
        test("T11", "Issue loan sets due date and reduces availability", () -> {
            var s = fresh(); Loan l = lend(s);
            require(l.issued().equals(LocalDate.of(2026,9,19)) && l.due().equals(LocalDate.of(2026,10,3))
                    && s.available(l.bookId()) == 0 && s.activeLoans(l.memberId()) == 1, "loan values");
        });
        test("T12", "Reject lending unavailable book", () -> {
            var s = fresh(); Loan l = lend(s); var m = s.addMember("Other", "other@example.com");
            rejects(() -> s.issue(l.bookId(), m.id())); require(s.state().loans().size() == 1, "changed after rejection");
        });
        test("T13", "Reject unknown book and member", () -> {
            var s = fresh(); Loan l = lend(s); rejects(() -> s.issue("B9999", l.memberId())); rejects(() -> s.issue(l.bookId(), "M9999"));
        });
        test("T14", "Reject a second active loan of the same title", () -> {
            var s = fresh(); Loan l = lend(s); s.addCopies(l.bookId(), 1); rejects(() -> s.issue(l.bookId(), l.memberId()));
        });
        test("T15", "Enforce three active loans per member", () -> {
            var s = fresh(); String m = s.addMember("A", "a@example.com").id();
            for (int i=0; i<3; i++) s.issue(s.addBook("Book " + i, "A", 1).id(), m);
            String fourth = s.addBook("Fourth", "A", 1).id(); rejects(() -> s.issue(fourth, m));
            require(s.activeLoans(m) == 3 && s.available(fourth) == 1, "limit changed state");
        });
        test("T16", "Return restores stock and preserves history", () -> {
            var s = fresh(); Loan l = lend(s); s.returnLoan(l.id());
            require(s.available(l.bookId()) == 1 && s.activeLoans(l.memberId()) == 0
                    && s.state().loans().get(0).returned().equals(s.today()), "return values");
        });
        test("T17", "Reject repeated return and unknown loan", () -> {
            var s = fresh(); Loan l = lend(s); s.returnLoan(l.id()); rejects(() -> s.returnLoan(l.id())); rejects(() -> s.returnLoan("L9999"));
            require(s.available(l.bookId()) == 1, "stock inflated");
        });
        test("T18", "Returned title can be borrowed again with a new ID", () -> {
            var s = fresh(); Loan first = lend(s); s.returnLoan(first.id()); var next = s.issue(first.bookId(), first.memberId());
            require(!first.id().equals(next.id()) && s.state().loans().size() == 2, "history lost");
        });
        test("T19", "Overdue starts the day after due date", () -> {
            Loan l = lend(fresh()); require(l.status(l.due()).equals("On loan") && l.status(l.due().plusDays(1)).equals("Overdue"), "boundary");
        });
        test("T20", "Returned loan stays returned after due date", () -> {
            var s = fresh(); Loan l = lend(s); s.returnLoan(l.id()); require(s.state().loans().get(0).status(l.due().plusDays(1)).equals("Returned"), "status");
        });
        test("T21", "Persist and reload books members and active loans", () -> {
            Path file = Files.createTempDirectory("library-test-").resolve("library.db");
            var s = new LibraryService(new FileLibraryStore(file), CLOCK); lend(s);
            var loaded = new LibraryService(new FileLibraryStore(file), CLOCK);
            require(loaded.state().equals(s.state()) && loaded.available("B0001") == 0, "round trip");
        });
        test("T22", "Reload return history and continue unique IDs", () -> {
            Path file = Files.createTempDirectory("library-test-").resolve("library.db");
            var s = new LibraryService(new FileLibraryStore(file), CLOCK); var l = lend(s); s.returnLoan(l.id());
            var loaded = new LibraryService(new FileLibraryStore(file), CLOCK);
            require(loaded.state().equals(s.state()) && loaded.issue(l.bookId(), l.memberId()).id().equals("L0002"), "reload ID");
        });
        test("T23", "Unicode punctuation and accents round trip", () -> {
            Path file = Files.createTempDirectory("library-test-").resolve("library.db");
            var s = new LibraryService(new FileLibraryStore(file), CLOCK); s.addBook("L'été, Java | 数据", "Émile O'Neil", 1);
            require(new FileLibraryStore(file).load().equals(s.state()), "unicode corruption");
        });
        test("T24", "Reject corrupt header without rewriting file", () -> {
            Path file = Files.createTempFile("library-corrupt-", ".db"); Files.writeString(file, "BROKEN");
            ioFailure(() -> new FileLibraryStore(file).load()); require(Files.readString(file).equals("BROKEN"), "file changed");
        });
        test("T25", "Reject invalid encoded record", () -> {
            Path file = Files.createTempFile("library-corrupt-", ".db"); Files.writeString(file, "LIBRARY\t1\nB\tB0001\t!\tQQ==\t1\n");
            ioFailure(() -> new FileLibraryStore(file).load());
        });
        test("T26", "Reject orphan loan reference", () -> {
            var s = fresh(); Loan l = lend(s);
            rejects(() -> LibraryService.validateState(new LibraryState(s.state().books(), List.of(), List.of(l))));
        });
        test("T27", "Reject duplicate persisted IDs", () -> {
            var s = fresh(); var b = s.addBook("T", "A", 1);
            rejects(() -> LibraryService.validateState(new LibraryState(List.of(b, b), List.of(), List.of())));
        });
        test("T28", "Failed save leaves service state unchanged", () -> {
            var store = new MemoryStore(); var s = new LibraryService(store, CLOCK); var l = lend(s);
            var original = s.state(); store.failSave = true; ioFailure(() -> s.returnLoan(l.id()));
            require(s.state().equals(original) && store.value.equals(original) && s.available(l.bookId()) == 0, "unsafe mutation");
        });
        test("T29", "Reject control characters and oversized text", () -> {
            var s = fresh(); rejects(() -> s.addBook("Line\nbreak", "A", 1)); rejects(() -> s.addMember("A".repeat(201), "a@example.com"));
        });
        test("T30", "Snapshot lists cannot be mutated by callers", () -> {
            var s = fresh(); var b = s.addBook("T", "A", 1);
            try { s.state().books().add(b); throw new AssertionError("list mutable"); }
            catch (UnsupportedOperationException expected) { }
        });
        test("T31", "Reject return when system date is before issue date", () -> {
            var store = new MemoryStore(); var s = new LibraryService(store, CLOCK); var l = lend(s);
            var earlier = new LibraryService(store, Clock.offset(CLOCK, Duration.ofDays(-1)));
            rejects(() -> earlier.returnLoan(l.id()));
        });
        test("T32", "Backup copy reloads to the identical state", () -> {
            Path directory = Files.createTempDirectory("library-backup-"); Path file = directory.resolve("library.db");
            var s = new LibraryService(new FileLibraryStore(file), CLOCK); lend(s);
            Path backup = directory.resolve("backup.db"); Files.copy(file, backup);
            require(new FileLibraryStore(backup).load().equals(s.state()), "backup content");
        });
        System.out.println("RESULT: " + passed + " passed; " + failed + " failed; " + (passed + failed) + " total.");
        if (failed > 0) System.exit(1);
    }
}
