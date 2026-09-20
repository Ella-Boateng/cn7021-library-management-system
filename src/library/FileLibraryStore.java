package library;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Versioned UTF-8 snapshot, Base64 text fields and an atomic whole-file save. */
public final class FileLibraryStore implements LibraryStore {
    private static final String HEADER = "LIBRARY\t1";
    private final Path file;
    public FileLibraryStore(Path file) { this.file = file.toAbsolutePath(); }
    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
    @Override public LibraryState load() throws IOException {
        if (Files.notExists(file)) return LibraryState.empty();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.get(0)))
            throw new IOException("Unrecognised data file. Restore a valid backup; the file has not been changed.");
        var books = new ArrayList<Book>();
        var members = new ArrayList<Member>();
        var loans = new ArrayList<Loan>();
        int lineNumber = 1;
        try {
            for (String line : lines.subList(1, lines.size())) {
                lineNumber++;
                String[] f = line.split("\t", -1);
                switch (f[0]) {
                    case "B" -> {
                        if (f.length != 5) throw new IllegalArgumentException("Book field count");
                        books.add(new Book(f[1], decode(f[2]), decode(f[3]), Integer.parseInt(f[4])));
                    }
                    case "M" -> {
                        if (f.length != 4) throw new IllegalArgumentException("Member field count");
                        members.add(new Member(f[1], decode(f[2]), decode(f[3])));
                    }
                    case "L" -> {
                        if (f.length != 7) throw new IllegalArgumentException("Loan field count");
                        loans.add(new Loan(f[1], f[2], f[3], LocalDate.parse(f[4]), LocalDate.parse(f[5]),
                                f[6].isEmpty() ? null : LocalDate.parse(f[6])));
                    }
                    default -> throw new IllegalArgumentException("Unknown record type");
                }
            }
            var state = new LibraryState(books, members, loans);
            LibraryService.validateState(state);
            return state;
        } catch (RuntimeException e) {
            throw new IOException("Invalid library data near line " + lineNumber + ": " + e.getMessage()
                    + ". The original file has not been changed.", e);
        }
    }
    @Override public void save(LibraryState state) throws IOException {
        LibraryService.validateState(state);
        var lines = new ArrayList<String>();
        lines.add(HEADER);
        for (Book b : state.books())
            lines.add(String.join("\t", "B", b.id(), encode(b.title()), encode(b.author()), "" + b.copies()));
        for (Member m : state.members())
            lines.add(String.join("\t", "M", m.id(), encode(m.name()), encode(m.email())));
        for (Loan l : state.loans())
            lines.add(String.join("\t", "L", l.id(), l.bookId(), l.memberId(), l.issued().toString(),
                    l.due().toString(), l.returned() == null ? "" : l.returned().toString()));
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "library-", ".tmp");
        try {
            Files.write(temp, lines, StandardCharsets.UTF_8);
            // Refuse unsafe fallback: a failed atomic replacement leaves the old file intact.
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
