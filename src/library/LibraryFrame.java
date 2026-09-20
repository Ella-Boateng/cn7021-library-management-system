package library;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

/** Small, single-librarian desktop interface. All changes go through LibraryService. */
public final class LibraryFrame extends JFrame {
    private final LibraryService service;
    private final Path dataFile;
    private final JLabel metrics = new JLabel();
    private final JLabel status = new JLabel("Ready. Changes are saved automatically.");
    private final JTextField bookSearch = new JTextField(23), memberSearch = new JTextField(22);
    private final JCheckBox activeOnly = new JCheckBox("Active loans only", true);
    private final DefaultTableModel books = model("ID", "Title", "Author", "Total copies", "Available");
    private final DefaultTableModel members = model("ID", "Name", "Email", "Active loans");
    private final DefaultTableModel loans = model("Loan ID", "Book", "Member", "Issued", "Due", "Returned", "Status");
    private final JTable bookTable = table(books), memberTable = table(members), loanTable = table(loans);
    private final JTabbedPane tabs = new JTabbedPane();

    public LibraryFrame(LibraryService service, Path dataFile) {
        super("Library Management System");
        this.service = service; this.dataFile = dataFile;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1120, 740); setMinimumSize(new Dimension(950, 620)); setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(new Color(244, 247, 250)); root.setBorder(new EmptyBorder(22, 26, 18, 26));
        setContentPane(root);
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 6)); heading.setOpaque(false);
        JLabel title = new JLabel("Library Management"); title.setFont(new Font("SansSerif", Font.BOLD, 29));
        title.setForeground(new Color(24, 47, 68));
        heading.add(title); heading.add(new JLabel("Books, members and lending in one place"));
        header.add(heading, BorderLayout.WEST);
        JButton guide = button("Quick guide", this::guide); header.add(guide, BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout(0, 20)); top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        metrics.setFont(new Font("SansSerif", Font.BOLD, 15));
        metrics.setOpaque(true); metrics.setBackground(new Color(225, 235, 242));
        metrics.setBorder(new EmptyBorder(18, 18, 18, 18)); top.add(metrics, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        JPanel bookBar = bar();
        addSearch(bookBar, "Search books", bookSearch);
        bookBar.add(button("Add book", this::addBook)); bookBar.add(button("Add copies", this::addCopies));
        bookBar.add(button("Issue selected book", this::issue));
        tabs.addTab("  Books  ", page(bookBar, bookTable,
                "Search by title, author or ID. Select a book to add copies or issue it."));
        JPanel memberBar = bar(); addSearch(memberBar, "Search members", memberSearch);
        memberBar.add(button("Register member", this::addMember));
        tabs.addTab("  Members  ", page(memberBar, memberTable,
                "Each member needs a unique email address. A member may hold up to three books."));
        JPanel loanBar = bar(); loanBar.add(activeOnly);
        loanBar.add(button("Issue book", this::issue)); loanBar.add(button("Return selected loan", this::returnBook));
        tabs.addTab("  Loans and returns  ", page(loanBar, loanTable,
                "Loans last 14 days. Due today is still on time. Uncheck Active loans only to see returned books."));
        root.add(tabs, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false);
        status.setFont(new Font("SansSerif", Font.PLAIN, 12)); footer.add(status, BorderLayout.CENTER);
        footer.add(button("Back up records", this::backup), BorderLayout.EAST); root.add(footer, BorderLayout.SOUTH);
        bookSearch.getDocument().addDocumentListener(listener()); memberSearch.getDocument().addDocumentListener(listener());
        activeOnly.addActionListener(e -> refresh());
        new Timer(60_000, e -> refresh()).start();
        refresh();
    }
    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return getRowCount() == 0 || getValueAt(0, col) == null ? Object.class : getValueAt(0, col).getClass();
            }
        };
    }
    private static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model); t.setRowHeight(34); t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setFillsViewportHeight(true);
        t.setShowVerticalLines(false); t.setGridColor(new Color(226, 232, 238));
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setPreferredSize(new Dimension(0, 36)); return t;
    }
    private static JPanel bar() { return new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12)); }
    private static JPanel page(JPanel toolbar, JTable table, String hint) {
        JPanel panel = new JPanel(new BorderLayout(0, 10)); panel.setBorder(new EmptyBorder(4, 10, 10, 10));
        panel.add(toolbar, BorderLayout.NORTH); panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JLabel label = new JLabel(hint); label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(label, BorderLayout.SOUTH); return panel;
    }
    private static void addSearch(JPanel panel, String label, JTextField field) {
        JLabel l = new JLabel(label); l.setLabelFor(field); panel.add(l); panel.add(field);
    }
    private static JButton button(String title, Runnable action) {
        JButton b = new JButton(title); b.addActionListener(e -> action.run()); return b;
    }
    private DocumentListener listener() {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        };
    }
    void refresh() {
        books.setRowCount(0);
        for (Book b : service.searchBooks(bookSearch.getText()))
            books.addRow(new Object[]{b.id(), b.title(), b.author(), b.copies(), service.available(b.id())});
        members.setRowCount(0);
        String query = memberSearch.getText().strip().toLowerCase(Locale.ROOT);
        for (Member m : service.state().members())
            if ((m.id() + " " + m.name() + " " + m.email()).toLowerCase(Locale.ROOT).contains(query))
                members.addRow(new Object[]{m.id(), m.name(), m.email(), service.activeLoans(m.id())});
        loans.setRowCount(0);
        for (Loan l : service.state().loans()) {
            if (activeOnly.isSelected() && !l.active()) continue;
            loans.addRow(new Object[]{l.id(), service.book(l.bookId()).title(), service.member(l.memberId()).name(),
                    l.issued().toString(), l.due().toString(), l.returned() == null ? "—" : l.returned().toString(), l.status(service.today())});
        }
        long active = service.state().loans().stream().filter(Loan::active).count();
        long overdue = service.state().loans().stream().filter(l -> l.status(service.today()).equals("Overdue")).count();
        int total = service.state().books().stream().mapToInt(Book::copies).sum();
        metrics.setText(service.state().books().size() + " titles     •     " + (total - active)
                + " copies available     •     " + service.state().members().size() + " members     •     "
                + active + " active loans     •     " + overdue + " overdue");
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(310);
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(210);
        loanTable.getColumnModel().getColumn(1).setPreferredWidth(230);
        loanTable.getColumnModel().getColumn(2).setPreferredWidth(150);
    }
    @FunctionalInterface private interface Change { void run() throws IOException; }
    private boolean save(Change change, String message) {
        try { change.run(); refresh(); status.setText(message + " Saved."); return true; }
        catch (IllegalArgumentException | IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Please check", JOptionPane.WARNING_MESSAGE); return false;
        }
    }
    private static JPanel form(String[] labels, JComponent... fields) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 12, 12));
        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]); label.setLabelFor(fields[i]); panel.add(label); panel.add(fields[i]);
        }
        return panel;
    }
    private boolean confirm(String title, JPanel form) {
        return JOptionPane.showConfirmDialog(this, form, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }
    private void addBook() {
        JTextField title = new JTextField(24), author = new JTextField(24);
        JSpinner copies = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JPanel form = form(new String[]{"Title", "Author", "Number of copies"}, title, author, copies);
        while (confirm("Add a book", form)) {
            try { copies.commitEdit(); }
            catch (java.text.ParseException e) { JOptionPane.showMessageDialog(this, "Enter a whole number from 1 to 999."); continue; }
            if (save(() -> service.addBook(title.getText(), author.getText(), (Integer) copies.getValue()), "Book added.")) break;
        }
    }
    private String selectedId(JTable table) {
        return table.getSelectedRow() < 0 ? null : table.getValueAt(table.getSelectedRow(), 0).toString();
    }
    private void addCopies() {
        String id = selectedId(bookTable);
        if (id == null) { JOptionPane.showMessageDialog(this, "Select a book first."); return; }
        JSpinner count = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JPanel form = form(new String[]{"Additional copies"}, count);
        while (confirm("Add copies of " + service.book(id).title(), form)) {
            try { count.commitEdit(); }
            catch (java.text.ParseException e) { JOptionPane.showMessageDialog(this, "Enter a whole number."); continue; }
            if (save(() -> service.addCopies(id, (Integer) count.getValue()), "Copies added.")) break;
        }
    }
    private void addMember() {
        JTextField name = new JTextField(24), email = new JTextField(24);
        JPanel form = form(new String[]{"Member name", "Email"}, name, email);
        while (confirm("Register a member", form))
            if (save(() -> service.addMember(name.getText(), email.getText()), "Member registered.")) break;
    }
    private void issue() {
        var available = service.state().books().stream().filter(b -> service.available(b.id()) > 0).toArray(Book[]::new);
        if (available.length == 0 || service.state().members().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add an available book and register a member before issuing a loan."); return;
        }
        JComboBox<Book> book = new JComboBox<>(available);
        JComboBox<Member> member = new JComboBox<>(service.state().members().toArray(Member[]::new));
        String selected = selectedId(bookTable);
        if (selected != null && service.available(selected) > 0) book.setSelectedItem(service.book(selected));
        JLabel due = new JLabel(service.today().plusDays(LibraryService.LOAN_DAYS).toString() + " (14 days)");
        JPanel form = form(new String[]{"Book", "Member", "Due date"}, book, member, due);
        while (confirm("Issue a book", form))
            if (save(() -> service.issue(((Book) book.getSelectedItem()).id(), ((Member) member.getSelectedItem()).id()), "Book issued.")) {
                tabs.setSelectedIndex(2); break;
            }
    }
    private void returnBook() {
        String id = selectedId(loanTable);
        if (id == null) { JOptionPane.showMessageDialog(this, "Select a loan first."); return; }
        if (JOptionPane.showConfirmDialog(this, "Record the return for " + id + "?", "Return book", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            save(() -> service.returnLoan(id), "Book returned.");
    }
    private void backup() {
        if (!Files.exists(dataFile)) { JOptionPane.showMessageDialog(this, "Add a book or member before making your first backup."); return; }
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Save a backup of library records");
        chooser.setSelectedFile(new java.io.File("library-backup-" + service.today() + ".db"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = chooser.getSelectedFile().toPath().toAbsolutePath();
        try {
            if (target.normalize().equals(dataFile.toAbsolutePath().normalize())
                    || Files.exists(target) && Files.isSameFile(target, dataFile))
                throw new IOException("Choose a different file from the working library database.");
            if (Files.exists(target) && JOptionPane.showConfirmDialog(this, "Replace the existing backup?", "Confirm replacement",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            Files.copy(dataFile, target, StandardCopyOption.REPLACE_EXISTING);
            status.setText("Backup saved to " + target);
        } catch (IOException e) { JOptionPane.showMessageDialog(this, e.getMessage(), "Backup failed", JOptionPane.ERROR_MESSAGE); }
    }
    private void guide() {
        JOptionPane.showMessageDialog(this, "1. Add books in the Books tab.\n2. Register readers in the Members tab.\n"
                + "3. Select a book and choose Issue selected book.\n4. Select a loan in Loans and returns to record a return.\n\n"
                + "Loans last 14 days. Each member can borrow up to three different titles.\n"
                + "Changes save automatically. Use Back up records to keep a separate copy.\n\n"
                + "Records: " + dataFile.toAbsolutePath(), "Quick guide", JOptionPane.INFORMATION_MESSAGE);
    }
}
