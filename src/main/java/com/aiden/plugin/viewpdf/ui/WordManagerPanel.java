package com.aiden.plugin.viewpdf.ui;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettingsListener;
import com.aiden.plugin.viewpdf.settings.WordLibraryLoader;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class WordManagerPanel implements Disposable {
    private static final int PAGE_SIZE = 50;

    private final PdfViewerSettings settings;
    private final JPanel rootPanel;
    private final JComboBox<VocabularyBookOption> bookComboBox;
    private final JTextField searchField;
    private final JButton prevButton;
    private final JButton nextButton;
    private final JLabel pageLabel;
    private final JTable table;
    private final WordTableModel tableModel;
    private boolean updatingBookSelection;
    private int pageIndex;
    private List<PdfViewerSettings.WordEntryData> filtered = List.of();

    public WordManagerPanel() {
        settings = PdfViewerSettings.getInstance();
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.weightx = 0;
        topPanel.add(new JLabel("单词本"), gbc);

        bookComboBox = new JComboBox<>();
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        topPanel.add(bookComboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        topPanel.add(new JLabel("搜索"), gbc);

        searchField = new JTextField();
        gbc.gridx = 3;
        gbc.weightx = 0.8;
        topPanel.add(searchField, gbc);

        prevButton = new JButton("上一页");
        gbc.gridx = 4;
        gbc.weightx = 0;
        topPanel.add(prevButton, gbc);

        nextButton = new JButton("下一页");
        gbc.gridx = 5;
        gbc.weightx = 0;
        topPanel.add(nextButton, gbc);

        pageLabel = new JLabel(" ");
        gbc.gridx = 6;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        topPanel.add(pageLabel, gbc);

        rootPanel.add(topPanel, BorderLayout.NORTH);

        tableModel = new WordTableModel(settings);
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(1).setCellEditor(createVisibilityEditor());
        table.getColumnModel().getColumn(2).setCellRenderer(new EditButtonRenderer());
        table.getColumnModel().getColumn(2).setCellEditor(new EditButtonEditor());
        JScrollPane scrollPane = new JBScrollPane(table);
        rootPanel.add(scrollPane, BorderLayout.CENTER);

        refreshBookOptions(settings.getSelectedVocabularyBookKey());
        refreshTable();

        bookComboBox.addActionListener(e -> {
            if (updatingBookSelection) {
                return;
            }
            Object selected = bookComboBox.getSelectedItem();
            if (selected instanceof VocabularyBookOption option) {
                settings.setSelectedVocabularyBookKey(option.key);
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                resetAndRefresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resetAndRefresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resetAndRefresh();
            }

            private void resetAndRefresh() {
                pageIndex = 0;
                refreshTable();
            }
        });

        prevButton.addActionListener(e -> {
            if (pageIndex > 0) {
                pageIndex--;
                refreshTable();
            }
        });
        nextButton.addActionListener(e -> {
            int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (pageIndex + 1 < totalPages) {
                pageIndex++;
                refreshTable();
            }
        });

        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(this)
                .subscribe(PdfViewerSettingsListener.TOPIC, new PdfViewerSettingsListener() {
                    @Override
                    public void pdfPathChanged(@Nullable String newPdfPath) {
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
                    public void wordSourceChanged(boolean builtinEnabled, @Nullable String customPath) {
                    }

                    @Override
                    public void wordCategoryFiltersChanged(@NotNull List<String> difficulties, @NotNull List<String> themes, @NotNull List<String> sources) {
                    }

                    @Override
                    public void vocabularyBookListChanged() {
                        refreshBookOptions(settings.getSelectedVocabularyBookKey());
                    }

                    @Override
                    public void selectedVocabularyBookChanged(@NotNull String key) {
                        pageIndex = 0;
                        refreshBookOptions(key);
                        refreshTable();
                    }

                    @Override
                    public void wordHiddenStateChanged(@NotNull String bookKey) {
                        if (Objects.equals(settings.getSelectedVocabularyBookKey(), bookKey)) {
                            refreshTable();
                        }
                    }

                    @Override
                    public void masteredWordLibraryChanged() {
                        if (Objects.equals(settings.getSelectedVocabularyBookKey(), WordLibraryLoader.getSystemMasteredBookKey())) {
                            pageIndex = 0;
                            refreshTable();
                        }
                    }
                });
    }

    public @NotNull JComponent getComponent() {
        return rootPanel;
    }

    private void refreshBookOptions(@NotNull String selectedKey) {
        updatingBookSelection = true;
        try {
            bookComboBox.removeAllItems();
            for (VocabularyBookOption option : buildBookOptions()) {
                bookComboBox.addItem(option);
            }
            VocabularyBookOption matched = null;
            for (int i = 0; i < bookComboBox.getItemCount(); i++) {
                VocabularyBookOption option = bookComboBox.getItemAt(i);
                if (option != null && option.key.equals(selectedKey)) {
                    matched = option;
                    break;
                }
            }
            if (matched == null && bookComboBox.getItemCount() > 0) {
                matched = bookComboBox.getItemAt(0);
            }
            if (matched != null) {
                bookComboBox.setSelectedItem(matched);
            }
        } finally {
            updatingBookSelection = false;
        }
    }

    private @NotNull List<VocabularyBookOption> buildBookOptions() {
        List<VocabularyBookOption> options = new ArrayList<>();
        options.add(new VocabularyBookOption(WordLibraryLoader.getSystemMasteredBookKey(), "系统 - 已学会"));
        for (String builtin : WordLibraryLoader.getBuiltinVocabularyBooks()) {
            options.add(new VocabularyBookOption("builtin:" + builtin, "内置 - " + builtin));
        }
        for (PdfViewerSettings.CustomVocabularyBookData book : settings.getCustomVocabularyBooks()) {
            if (book == null || book.name == null || book.name.isBlank()) {
                continue;
            }
            options.add(new VocabularyBookOption("custom:" + book.name.trim(), "自定义 - " + book.name.trim()));
        }
        return options;
    }

    private void refreshTable() {
        String query = normalizeQuery(searchField.getText());
        List<PdfViewerSettings.WordEntryData> entries = settings.getWordEntries();
        if (query == null) {
            filtered = entries;
        } else {
            List<PdfViewerSettings.WordEntryData> next = new ArrayList<>();
            for (PdfViewerSettings.WordEntryData entry : entries) {
                if (entry == null || entry.word == null) {
                    continue;
                }
                String w = entry.word.toLowerCase(Locale.ROOT);
                if (w.contains(query)) {
                    next.add(entry);
                }
            }
            filtered = next;
        }
        int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (pageIndex >= totalPages) {
            pageIndex = Math.max(0, totalPages - 1);
        }
        int start = pageIndex * PAGE_SIZE;
        int end = Math.min(filtered.size(), start + PAGE_SIZE);
        List<PdfViewerSettings.WordEntryData> page = start >= end ? List.of() : filtered.subList(start, end);
        tableModel.setRows(settings.getSelectedVocabularyBookKey(), page);

        prevButton.setEnabled(pageIndex > 0);
        nextButton.setEnabled(pageIndex + 1 < totalPages);
        pageLabel.setText((pageIndex + 1) + "/" + totalPages);
    }

    private static @Nullable String normalizeQuery(@Nullable String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private @NotNull TableCellEditor createVisibilityEditor() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"是", "否"});
        return new DefaultCellEditor(combo);
    }

    private void showJsonPopup(@NotNull PdfViewerSettings.WordEntryData entry) {
        JTextArea textArea = new JTextArea(toJson(entry));
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JBScrollPane(textArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scroll, (JComponent) scroll)
                .setTitle(entry.word == null ? "单词" : entry.word)
                .setMovable(true)
                .setResizable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setCancelOnWindowDeactivation(true)
                .createPopup();
        popup.showInFocusCenter();
    }

    private static @NotNull String toJson(@NotNull PdfViewerSettings.WordEntryData entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        appendJsonField(sb, "word", entry.word, true);
        appendJsonField(sb, "meaning", entry.meaning, true);
        appendJsonField(sb, "phonetic", entry.phonetic, true);
        appendJsonField(sb, "difficulty", entry.difficulty, true);
        appendJsonField(sb, "theme", entry.theme, true);
        appendJsonField(sb, "source", entry.source, true);
        appendJsonField(sb, "sourceRef", entry.sourceRef, true);
        appendJsonField(sb, "status", entry.status, true);
        sb.append("  \"sentenceEnList\": ").append(toJsonArray(entry.sentenceEnList)).append(",\n");
        sb.append("  \"synonymsByPos\": ").append(toJsonSynonyms(entry.synonymsByPos)).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendJsonField(@NotNull StringBuilder sb, @NotNull String key, @Nullable String value, boolean comma) {
        sb.append("  \"").append(escapeJson(key)).append("\": ");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append("\"").append(escapeJson(value)).append("\"");
        }
        sb.append(comma ? ",\n" : "\n");
    }

    private static @NotNull String toJsonArray(@Nullable List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String v : values) {
            if (v == null) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            sb.append("\"").append(escapeJson(v)).append("\"");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private static @NotNull String toJsonSynonyms(@Nullable List<PdfViewerSettings.WordSynonymGroupData> groups) {
        if (groups == null || groups.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (PdfViewerSettings.WordSynonymGroupData g : groups) {
            if (g == null) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            sb.append("{\"pos\": ");
            sb.append(g.pos == null ? "null" : "\"" + escapeJson(g.pos) + "\"");
            sb.append(", \"words\": ").append(toJsonArray(g.words)).append("}");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private static @NotNull String escapeJson(@NotNull String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    @Override
    public void dispose() {
    }

    private static final class VocabularyBookOption {
        private final String key;
        private final String label;

        private VocabularyBookOption(@NotNull String key, @NotNull String label) {
            this.key = key;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class WordTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"单词", "显示", "操作"};

        private final PdfViewerSettings settings;
        private String bookKey;
        private List<PdfViewerSettings.WordEntryData> rows = List.of();

        private WordTableModel(@NotNull PdfViewerSettings settings) {
            this.settings = settings;
        }

        private void setRows(@NotNull String bookKey, @NotNull List<PdfViewerSettings.WordEntryData> rows) {
            this.bookKey = bookKey;
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
            return columnIndex == 1 || columnIndex == 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PdfViewerSettings.WordEntryData entry = rows.get(rowIndex);
            if (columnIndex == 0) {
                return entry.word == null ? "" : entry.word;
            }
            if (columnIndex == 1) {
                boolean hidden = settings.isWordHiddenInPopup(bookKey, entry.word);
                return hidden ? "否" : "是";
            }
            return "编辑";
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 1) {
                return;
            }
            PdfViewerSettings.WordEntryData entry = rows.get(rowIndex);
            String text = aValue == null ? "" : aValue.toString();
            boolean hidden = "否".equals(text);
            settings.setWordHiddenInPopup(bookKey, entry.word, hidden);
            fireTableRowsUpdated(rowIndex, rowIndex);
        }

        private @Nullable PdfViewerSettings.WordEntryData getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }
    }

    private final class EditButtonRenderer implements TableCellRenderer {
        private final JButton button = new JButton("编辑");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return button;
        }
    }

    private final class EditButtonEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("编辑");
        private int editingRow = -1;

        private EditButtonEditor() {
            button.addActionListener(e -> {
                int row = editingRow;
                SwingUtilities.invokeLater(() -> {
                    PdfViewerSettings.WordEntryData entry = tableModel.getRow(row);
                    if (entry != null) {
                        showJsonPopup(entry);
                    }
                });
                stopCellEditing();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "编辑";
        }
    }
}
