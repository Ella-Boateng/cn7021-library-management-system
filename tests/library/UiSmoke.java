package library;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;

/** Renders actual Swing panels using fictional records; does not touch the user's data. */
public final class UiSmoke {
    public static void main(String[] args) throws Exception {
        Path output = Paths.get(args[0]); Files.createDirectories(output);
        Path data = Files.createTempDirectory("library-ui-").resolve("library.db");
        Clock clock = Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC);
        var service = new LibraryService(new FileLibraryStore(data), clock);
        service.addBook("Software Engineering", "Ian Sommerville", 3);
        service.addBook("Effective Java", "Joshua Bloch", 2);
        service.addBook("Clean Code", "Robert C. Martin", 2);
        service.addBook("Database System Concepts", "Silberschatz, Korth and Sudarshan", 1);
        service.addMember("Alex Example", "alex@example.com");
        service.addMember("Sam Example", "sam@example.com");
        var past = new LibraryService(new FileLibraryStore(data), Clock.offset(clock, Duration.ofDays(-20)));
        past.issue("B0001", "M0001");
        var today = new LibraryService(new FileLibraryStore(data), clock);
        today.issue("B0002", "M0002");
        var returned = today.issue("B0003", "M0002"); today.returnLoan(returned.id());
        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                LibraryFrame frame = new LibraryFrame(today, data); frame.setVisible(true);
                JTabbedPane tabs = find(frame, JTabbedPane.class);
                String[] names = {"01-books.png", "02-members.png", "03-loans.png"};
                for (int i = 0; i < names.length; i++) {
                    tabs.setSelectedIndex(i); frame.validate();
                    BufferedImage img = new BufferedImage(frame.getContentPane().getWidth(), frame.getContentPane().getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = img.createGraphics(); frame.getContentPane().printAll(g); g.dispose();
                    ImageIO.write(img, "png", output.resolve(names[i]).toFile());
                }
                JTextField search = find(tabs.getComponentAt(0), JTextField.class);
                search.setText("effective");
                JTable table = find(tabs.getComponentAt(0), JTable.class);
                if (table.getRowCount() != 1 || !"Effective Java".equals(table.getValueAt(0,1)))
                    throw new AssertionError("UI search did not update");
                JTextField memberSearch = find(tabs.getComponentAt(1), JTextField.class);
                JTable memberTable = find(tabs.getComponentAt(1), JTable.class);
                for (String query : new String[]{"alex", "ALEX@EXAMPLE.COM", "M0001"}) {
                    memberSearch.setText(query);
                    if (memberTable.getRowCount() != 1 || !"M0001".equals(memberTable.getValueAt(0,0)))
                        throw new AssertionError("Member search failed for " + query);
                }
                memberSearch.setText("no-match");
                if (memberTable.getRowCount() != 0) throw new AssertionError("No-match member search");
                memberSearch.setText("");
                if (memberTable.getRowCount() != 2) throw new AssertionError("Cleared member search");
                System.out.println("UI01 | PASS | All three tabs render with realistic fictional records");
                System.out.println("UI02 | PASS | Book and member searches filter visible tables by text and ID");
                frame.dispose();
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        System.exit(0);
    }
    private static <T> T find(Component c, Class<T> kind) {
        if (kind.isInstance(c)) return kind.cast(c);
        if (c instanceof Container container)
            for (Component child : container.getComponents()) { T found = find(child, kind); if (found != null) return found; }
        return null;
    }
}
