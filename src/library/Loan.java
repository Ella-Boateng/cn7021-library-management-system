package library;

import java.time.LocalDate;

public record Loan(String id, String bookId, String memberId,
                   LocalDate issued, LocalDate due, LocalDate returned) {
    public boolean active() { return returned == null; }
    public String status(LocalDate today) {
        return !active() ? "Returned" : today.isAfter(due) ? "Overdue" : "On loan";
    }
}
