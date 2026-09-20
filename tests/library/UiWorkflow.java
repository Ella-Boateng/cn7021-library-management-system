package library;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/** Exercises real Swing buttons and modal dialogs, with disposable test data. */
public final class UiWorkflow {
    private static LibraryFrame frame;
    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action); SwingUtilities.invokeAndWait(task); return task.get();
    }
    private static <T> List<T> all(Component c, Class<T> kind) {
        var result = new ArrayList<T>(); if (kind.isInstance(c)) result.add(kind.cast(c));
        if (c instanceof Container container) for (Component child : container.getComponents()) result.addAll(all(child, kind));
        return result;
    }
    private static void click(Container container, String label) throws Exception {
        JButton button = edt(() -> all(container, JButton.class).stream().filter(b -> label.equals(b.getText())).findFirst().orElseThrow());
        SwingUtilities.invokeLater(button::doClick);
    }
    private static JDialog dialog(String title) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            JDialog d = edt(() -> {
                for (Window w : Window.getWindows()) if (w instanceof JDialog x && x.isVisible() && x.getTitle().equals(title)) return x;
                return null;
            });
            if (d != null) return d;
            Thread.sleep(30);
        }
        throw new AssertionError("Dialog did not open: " + title);
    }
    private static void settle() throws Exception { Thread.sleep(180); edt(() -> null); }
    private static void check(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
        System.out.println("PASS | " + description);
    }
    public static void main(String[] args) throws Exception {
        try {
            Path dir = Files.createTempDirectory("library-ui-workflow-"); Path data = dir.resolve("library.db");
            var service = new LibraryService(new FileLibraryStore(data), Clock.systemDefaultZone());
            edt(() -> { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                frame = new LibraryFrame(service, data); frame.setVisible(true); return null; });
            click(frame, "Add book"); JDialog add = dialog("Add a book"); click(add, "Cancel"); settle();
            check(service.state().books().isEmpty(), "UI03 Cancel leaves catalogue unchanged");
            click(frame, "Add book"); add = dialog("Add a book"); click(add, "OK");
            JDialog error = dialog("Please check"); click(error, "OK"); settle();
            check(service.state().books().isEmpty(), "UI04 Blank book form is rejected");
            add = dialog("Add a book"); final JDialog bookDialog = add;
            edt(() -> { var fields = all(bookDialog, JTextField.class); fields.get(0).setText("Demonstration Book"); fields.get(1).setText("Example Author"); return null; });
            click(add, "OK"); settle(); check(service.state().books().size() == 1, "UI05 Book form saves a new book");
            JTabbedPane tabs = edt(() -> all(frame, JTabbedPane.class).get(0));
            edt(() -> { tabs.setSelectedIndex(1); return null; });
            click(frame, "Register member"); JDialog member = dialog("Register a member");
            edt(() -> { var fields = all(member, JTextField.class); fields.get(0).setText("Example Reader"); fields.get(1).setText("demo@example.com"); return null; });
            click(member, "OK"); settle(); check(service.state().members().size() == 1, "UI06 Member form saves registration");
            edt(() -> { tabs.setSelectedIndex(0); all(tabs.getComponentAt(0), JTable.class).get(0).setRowSelectionInterval(0,0); return null; });
            click(frame, "Issue selected book"); JDialog issue = dialog("Issue a book"); click(issue, "OK"); settle();
            check(service.state().loans().size() == 1 && service.available("B0001") == 0, "UI07 Issue dialog creates a loan");
            edt(() -> { all(tabs.getComponentAt(2), JTable.class).get(0).setRowSelectionInterval(0,0); return null; });
            click(frame, "Return selected loan"); JDialog returns = dialog("Return book"); click(returns, "OK"); settle();
            check(service.available("B0001") == 1 && !service.state().loans().get(0).active(), "UI08 Return confirmation restores availability");
            edt(() -> { all(tabs.getComponentAt(2), JCheckBox.class).get(0).doClick(); return null; });
            check(edt(() -> all(tabs.getComponentAt(2), JTable.class).get(0).getRowCount()) == 1, "UI09 History filter shows returned loan");
            click(frame, "Back up records"); JDialog backup = dialog("Save a backup of library records");
            Path destination = dir.resolve("backup.db");
            edt(() -> { JFileChooser chooser = all(backup, JFileChooser.class).get(0); chooser.setSelectedFile(destination.toFile()); chooser.approveSelection(); return null; }); settle();
            check(new FileLibraryStore(destination).load().equals(service.state()), "UI10 Backup button creates a restorable copy");
            check(new LibraryService(new FileLibraryStore(data), Clock.systemDefaultZone()).state().equals(service.state()), "UI11 UI changes survive reloading the store");
            System.out.println("RESULT: 9 UI workflow checks passed; 0 failed.");
            edt(() -> { frame.dispose(); return null; }); System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace(); System.exit(1);
        }
    }
}
