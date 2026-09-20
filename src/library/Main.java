package library;

import javax.swing.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.Clock;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        Path directory = Paths.get(args.length > 0 ? args[0] : "data").toAbsolutePath();
        try {
            Files.createDirectories(directory);
            FileChannel channel = FileChannel.open(directory.resolve("library.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock;
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException e) { lock = null; }
            if (lock == null) {
                channel.close();
                throw new IOException("This library is already open. Use the existing window.");
            }
            final FileLock heldLock = lock;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { heldLock.release(); channel.close(); } catch (IOException ignored) { }
            }));
            Path file = directory.resolve("library.db");
            LibraryService service = new LibraryService(new FileLibraryStore(file), Clock.systemDefaultZone());
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("defaultFont", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
            SwingUtilities.invokeLater(() -> new LibraryFrame(service, file).setVisible(true));
        } catch (Exception e) {
            System.err.println("Cannot start library: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Cannot start the library.\n" + e.getMessage(), "Library startup", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
