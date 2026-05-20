package com.aiden.plugin.viewpdf.ui;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettingsListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BookManagerPanel implements Disposable {
    private final PdfViewerSettings settings;
    private final Project project;
    private final JPanel rootPanel;
    private final JButton importButton;
    private final JButton addButton;
    private final JTextField searchField;
    private final JTable table;
    private final BookTableModel tableModel;

    public BookManagerPanel(@Nullable Project project) {
        settings = PdfViewerSettings.getInstance();
        this.project = project;
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createTitledBorder("图书管理"));

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        importButton = new JButton("导入图书");
        gbc.gridx = 0;
        gbc.weightx = 0;
        topPanel.add(importButton, gbc);

        addButton = new JButton("添加图书");
        gbc.gridx = 1;
        gbc.weightx = 0;
        topPanel.add(addButton, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        topPanel.add(new JLabel("搜索"), gbc);

        searchField = new JTextField();
        gbc.gridx = 3;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        topPanel.add(searchField, gbc);

        rootPanel.add(topPanel, BorderLayout.NORTH);

        tableModel = new BookTableModel(settings);
        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.getColumnModel().getColumn(1).setCellRenderer(new ActionCellRenderer());
        table.getColumnModel().getColumn(1).setCellEditor(new ActionCellEditor());
        JScrollPane scrollPane = new JBScrollPane(table);
        rootPanel.add(scrollPane, BorderLayout.CENTER);

        refreshTable();

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshTable();
            }
        });

        importButton.addActionListener(e -> importBook());
        addButton.addActionListener(e -> addManualBook());

        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(this)
                .subscribe(PdfViewerSettingsListener.TOPIC, new PdfViewerSettingsListener() {
                    @Override
                    public void pdfPathChanged(String newPdfPath) {
                    }

                    @Override
                    public void nightModeChanged(boolean enabled) {
                    }

                    @Override
                    public void pdfBackgroundChanged(@NotNull java.awt.Color newBackgroundColor) {
                    }

                    @Override
                    public void hoverSecondsChanged(int seconds) {
                    }

                    @Override
                    public void zoomPercentChanged(int percent) {
                    }

                    @Override
                    public void pdfTextColorChanged(@NotNull java.awt.Color newTextColor) {
                    }

                    @Override
                    public void treeBackgroundChanged(@NotNull java.awt.Color newBackgroundColor) {
                    }

                    @Override
                    public void treeTextColorChanged(@NotNull java.awt.Color newTextColor) {
                    }

                    @Override
                    public void treeFontSizeChanged(int size) {
                    }

                    @Override
                    public void paneRatiosChanged(int leftPercent, int middlePercent, int rightPercent) {
                    }

                    @Override
                    public void thirdPaneVisibilityChanged(boolean visible) {
                    }

                    @Override
                    public void editorPopupSizeChanged(int width, int height) {
                    }

                    @Override
                    public void editorPopupBorderVisibilityChanged(boolean visible) {
                    }

                    @Override
                    public void editorPopupPdfBackgroundChanged(@NotNull java.awt.Color newBackgroundColor) {
                    }

                    @Override
                    public void editorPopupPdfTextColorChanged(@NotNull java.awt.Color newTextColor) {
                    }

                    @Override
                    public void renderBatchPageCountChanged(int pageCount) {
                    }

                    @Override
                    public void wordPopupStyleChanged(int width, int height, int x, int y, int fontSize, @NotNull java.awt.Color fontColor) {
                    }

                    @Override
                    public void wordSourceChanged(boolean builtinEnabled, String customPath) {
                    }

                    @Override
                    public void wordCategoryFiltersChanged(@NotNull List<String> difficulties, @NotNull List<String> themes, @NotNull List<String> sources) {
                    }

                    @Override
                    public void bookLibraryChanged() {
                        refreshTable();
                    }

                    @Override
                    public void currentReadingBookChanged(@Nullable String bookId) {
                        refreshTable();
                    }
                });
    }

    public @NotNull JComponent getComponent() {
        return rootPanel;
    }

    private void refreshTable() {
        String query = normalizeQuery(searchField.getText());
        List<PdfViewerSettings.BookData> books = settings.getBooks();
        if (query == null) {
            tableModel.setRows(books);
            return;
        }
        List<PdfViewerSettings.BookData> filtered = new ArrayList<>();
        for (PdfViewerSettings.BookData book : books) {
            if (book == null || book.name == null) {
                continue;
            }
            if (book.name.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(book);
            }
        }
        tableModel.setRows(filtered);
    }

    private static @Nullable String normalizeQuery(@Nullable String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public void dispose() {
    }

    private void importBook() {
        VirtualFile file = chooseTxtFile(null);
        if (file == null) {
            return;
        }
        String name = file.getNameWithoutExtension();
        String bookId = settings.addImportedBook(name, file.getPath());
        if (bookId != null) {
            maybeSelectCurrentBook(bookId, true);
        }
    }

    private void addManualBook() {
        ManualBookFormResult result = showManualBookDialog("添加图书", "", "", true, true);
        if (result == null) {
            return;
        }
        String bookId = settings.addManualBook(result.name, result.content);
        if (bookId != null) {
            maybeSelectCurrentBook(bookId, true);
        }
    }

    private void editBook(@NotNull PdfViewerSettings.BookData book) {
        String sourceType = book.sourceType == null ? "" : book.sourceType.trim().toLowerCase(Locale.ROOT);
        boolean allowReimport = "import".equals(sourceType);
        BookEditResult result = showBookEditDialog(
                "编辑图书",
                book.name == null ? "" : book.name,
                book.inlineContent == null ? "" : book.inlineContent,
                allowReimport,
                book.filePath
        );
        if (result == null) {
            return;
        }
        boolean changed = settings.updateBook(book.id, result.name, result.content, result.filePath);
        if (changed) {
            maybeSelectCurrentBook(book.id, false);
        }
    }

    private void deleteBook(@NotNull PdfViewerSettings.BookData book) {
        String name = book.name == null ? "" : book.name;
        int choice = JOptionPane.showConfirmDialog(
                rootPanel,
                "确认删除图书：" + name + "？",
                "删除图书",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        settings.deleteBook(book.id);
    }

    private @Nullable VirtualFile chooseTxtFile(@Nullable String initialPath) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true,
                false,
                false,
                false,
                false,
                false
        ) {
            @Override
            public boolean isFileSelectable(VirtualFile file) {
                if (!super.isFileSelectable(file)) {
                    return false;
                }
                return "txt".equalsIgnoreCase(file.getExtension());
            }
        };
        descriptor.setTitle("选择 TXT 文件");
        VirtualFile toSelect = null;
        if (initialPath != null && !initialPath.isBlank()) {
            toSelect = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(initialPath);
        }
        return FileChooser.chooseFile(descriptor, rootPanel, project, toSelect);
    }

    private void maybeSelectCurrentBook(@NotNull String bookId, boolean defaultYes) {
        if (Objects.equals(settings.getCurrentReadingBookId(), bookId)) {
            return;
        }
        int defaultOption = defaultYes ? JOptionPane.YES_OPTION : JOptionPane.NO_OPTION;
        Object[] options = {"是", "否"};
        int choice = JOptionPane.showOptionDialog(
                rootPanel,
                "是否立即切换为当前阅读图书？",
                "切换当前图书",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[defaultOption == JOptionPane.YES_OPTION ? 0 : 1]
        );
        if (choice == JOptionPane.YES_OPTION) {
            settings.setCurrentReadingBookId(bookId);
        }
    }

    private static final class ManualBookFormResult {
        private final String name;
        private final String content;

        private ManualBookFormResult(@NotNull String name, @NotNull String content) {
            this.name = name;
            this.content = content;
        }
    }

    private @Nullable ManualBookFormResult showManualBookDialog(
            @NotNull String title,
            @NotNull String initialName,
            @NotNull String initialContent,
            boolean editableName,
            boolean allowEmptyContent
    ) {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nameRow.add(new JLabel("书名"));
        JTextField nameField = new JTextField(initialName, 24);
        nameField.setEditable(editableName);
        nameRow.add(nameField);
        panel.add(nameRow, BorderLayout.NORTH);

        JTextArea contentArea = new JTextArea(initialContent, 14, 48);
        JScrollPane scrollPane = new JBScrollPane(contentArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(rootPanel, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(rootPanel, "请输入书名", title, JOptionPane.WARNING_MESSAGE);
                continue;
            }
            String content = contentArea.getText() == null ? "" : contentArea.getText();
            if (!allowEmptyContent && content.trim().isEmpty()) {
                JOptionPane.showMessageDialog(rootPanel, "请输入内容", title, JOptionPane.WARNING_MESSAGE);
                continue;
            }
            return new ManualBookFormResult(name, content);
        }
    }

    private static final class BookEditResult {
        private final String name;
        private final String content;
        private final String filePath;

        private BookEditResult(@NotNull String name, @NotNull String content, @Nullable String filePath) {
            this.name = name;
            this.content = content;
            this.filePath = filePath;
        }
    }

    private @Nullable BookEditResult showBookEditDialog(
            @NotNull String title,
            @NotNull String initialName,
            @NotNull String initialContent,
            boolean allowReimport,
            @Nullable String initialFilePath
    ) {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        top.add(new JLabel("书名"), gbc);
        JTextField nameField = new JTextField(initialName, 24);
        gbc.gridx = 1;
        gbc.weightx = 1;
        top.add(nameField, gbc);

        final String[] selectedPath = {initialFilePath};
        JButton reimportButton = null;
        if (allowReimport) {
            reimportButton = new JButton("重新导入");
            gbc.gridx = 2;
            gbc.weightx = 0;
            gbc.insets = new Insets(0, 0, 0, 0);
            top.add(reimportButton, gbc);
        }

        panel.add(top, BorderLayout.NORTH);

        JTextArea contentArea = new JTextArea(initialContent, 14, 48);
        JScrollPane scrollPane = new JBScrollPane(contentArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        if (reimportButton != null) {
            reimportButton.addActionListener(e -> {
                VirtualFile file = chooseTxtFile(selectedPath[0]);
                if (file == null) {
                    return;
                }
                selectedPath[0] = file.getPath();
                try {
                    String text = readFileLineByLine(file);
                    contentArea.setText(text);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(rootPanel, "导入失败", title, JOptionPane.WARNING_MESSAGE);
                }
            });
        }

        while (true) {
            int choice = JOptionPane.showConfirmDialog(rootPanel, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(rootPanel, "请输入书名", title, JOptionPane.WARNING_MESSAGE);
                continue;
            }
            String content = contentArea.getText() == null ? "" : contentArea.getText();
            return new BookEditResult(name, content, selectedPath[0]);
        }
    }

    private static @NotNull String readFileLineByLine(@NotNull VirtualFile file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), file.getCharset()))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) {
                    sb.append('\n');
                }
                sb.append(line);
                first = false;
            }
        }
        return sb.toString();
    }

    private static final class BookTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"书名", "操作"};

        private final PdfViewerSettings settings;
        private List<PdfViewerSettings.BookData> rows = List.of();

        private BookTableModel(@NotNull PdfViewerSettings settings) {
            this.settings = settings;
        }

        private void setRows(@NotNull List<PdfViewerSettings.BookData> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PdfViewerSettings.BookData book = rows.get(rowIndex);
            if (columnIndex == 0) {
                return book.name == null ? "" : book.name;
            }
            return "";
        }

        private @Nullable PdfViewerSettings.BookData getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }
    }

    private final class ActionCellRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final JButton selectButton = new JButton();
        private final JButton editButton = new JButton("编辑");
        private final JButton deleteButton = new JButton("删除");

        private ActionCellRenderer() {
            panel.add(selectButton);
            panel.add(editButton);
            panel.add(deleteButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            PdfViewerSettings.BookData book = tableModel.getRow(row);
            String currentId = settings.getCurrentReadingBookId();
            boolean current = book != null && currentId != null && Objects.equals(currentId, book.id);
            selectButton.setText(current ? "阅读中" : "选择/阅读");
            selectButton.setEnabled(!current);
            editButton.setEnabled(book != null);
            deleteButton.setEnabled(book != null);
            return panel;
        }
    }

    private final class ActionCellEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final JButton selectButton = new JButton();
        private final JButton editButton = new JButton("编辑");
        private final JButton deleteButton = new JButton("删除");
        private int editingRow = -1;

        private ActionCellEditor() {
            panel.add(selectButton);
            panel.add(editButton);
            panel.add(deleteButton);

            selectButton.addActionListener(e -> {
                int row = editingRow;
                SwingUtilities.invokeLater(() -> {
                    PdfViewerSettings.BookData book = tableModel.getRow(row);
                    if (book != null) {
                        settings.setCurrentReadingBookId(book.id);
                    }
                });
                stopCellEditing();
            });

            editButton.addActionListener(e -> {
                int row = editingRow;
                SwingUtilities.invokeLater(() -> {
                    PdfViewerSettings.BookData book = tableModel.getRow(row);
                    if (book != null) {
                        editBook(book);
                    }
                });
                stopCellEditing();
            });

            deleteButton.addActionListener(e -> {
                int row = editingRow;
                SwingUtilities.invokeLater(() -> {
                    PdfViewerSettings.BookData book = tableModel.getRow(row);
                    if (book != null) {
                        deleteBook(book);
                    }
                });
                stopCellEditing();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row;
            PdfViewerSettings.BookData book = tableModel.getRow(row);
            String currentId = settings.getCurrentReadingBookId();
            boolean current = book != null && currentId != null && Objects.equals(currentId, book.id);
            selectButton.setText(current ? "阅读中" : "选择/阅读");
            selectButton.setEnabled(!current);
            editButton.setEnabled(book != null);
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }
}
