package com.aiden.plugin.viewpdf.stockwatcher.ui;

import com.aiden.plugin.viewpdf.stockwatcher.Quote;
import com.aiden.plugin.viewpdf.stockwatcher.SinaQuoteFetcher;
import com.aiden.plugin.viewpdf.stockwatcher.StockWatcherColumn;
import com.aiden.plugin.viewpdf.stockwatcher.StockWatcherSettings;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class StockWatcherSwatchPanel implements Disposable {
    public static final String NOTIFICATION_GROUP_ID = "XTools.StockWatcher";
    private static final String CARD_EMPTY = "empty";
    private static final String CARD_MAIN = "main";
    private static final long MAX_BACKOFF_SECONDS = 300;

    private final Project project;
    private final StockWatcherSettings settings;
    private final JPanel rootPanel;
    private final JPanel cards;
    private final CardLayout cardLayout;
    private final JTable table;
    private final JLabel statusLabel;
    private final SwatchTableModel tableModel;

    private final SinaQuoteFetcher quoteFetcher;
    private final ScheduledExecutorService pollingExecutor;
    private final Object scheduleLock = new Object();
    private volatile ScheduledFuture<?> scheduledFuture;
    private volatile boolean disposed;
    private volatile int consecutiveFailures;
    private volatile String lastSuccessRefreshTime;
    private volatile String lastErrorSummary;
    private final Map<String, Long> lastNotifyMillisByCode = new ConcurrentHashMap<>();

    public StockWatcherSwatchPanel(@NotNull Project project) {
        this.project = project;
        settings = StockWatcherSettings.getInstance();
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createTitledBorder("Swatch"));

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.add(createCenteredLabelPanel("暂无自选股票"), CARD_EMPTY);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        statusLabel = new JLabel("最近刷新: 未成功");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        tableModel = new SwatchTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(26);
        mainPanel.add(new JBScrollPane(table), BorderLayout.CENTER);

        cards.add(mainPanel, CARD_MAIN);
        rootPanel.add(cards, BorderLayout.CENTER);

        quoteFetcher = new SinaQuoteFetcher();
        pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "stock-watcher-polling");
            t.setDaemon(true);
            return t;
        });

        refreshFromSettings(false);
        scheduleNextPoll(0);
    }

    public @NotNull JComponent getComponent() {
        return rootPanel;
    }

    public void refreshFromSettings(boolean dataAvailable) {
        List<String> codes = settings.getStockCodes();
        if (codes.isEmpty()) {
            cardLayout.show(cards, CARD_EMPTY);
            return;
        }
        cardLayout.show(cards, CARD_MAIN);
        List<String> nextColumnKeys = buildColumnKeys(settings.getVisibleColumns());
        tableModel.setModelData(nextColumnKeys, codes);
        updateStatusLabelText();
        SwingUtilities.invokeLater(this::installActionColumn);
    }

    private void installActionColumn() {
        int actionsIndex = tableModel.getColumnIndex(StockWatcherColumn.ACTIONS.getKey());
        if (actionsIndex < 0 || actionsIndex >= table.getColumnModel().getColumnCount()) {
            return;
        }
        table.getColumnModel().getColumn(actionsIndex).setCellRenderer(new ActionCellRenderer());
        table.getColumnModel().getColumn(actionsIndex).setCellEditor(new ActionCellEditor());
    }

    private static @NotNull List<String> buildColumnKeys(@NotNull List<String> visibleColumns) {
        List<String> keys = new ArrayList<>();
        keys.add(StockWatcherColumn.CODE.getKey());
        keys.add(StockWatcherColumn.NAME.getKey());
        for (String key : visibleColumns) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (StockWatcherColumn.CODE.getKey().equals(key)
                    || StockWatcherColumn.NAME.getKey().equals(key)
                    || StockWatcherColumn.ACTIONS.getKey().equals(key)) {
                continue;
            }
            keys.add(key);
        }
        keys.add(StockWatcherColumn.ACTIONS.getKey());
        LinkedHashMap<String, Boolean> unique = new LinkedHashMap<>();
        for (String key : keys) {
            unique.put(key, Boolean.TRUE);
        }
        return new ArrayList<>(unique.keySet());
    }

    private static @NotNull JPanel createCenteredLabelPanel(@NotNull String text) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void dispose() {
        disposed = true;
        synchronized (scheduleLock) {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
                scheduledFuture = null;
            }
        }
        pollingExecutor.shutdownNow();
    }

    private void pollOnceSafe() {
        if (disposed) {
            return;
        }
        try {
            List<String> codes = settings.getValidStockCodes();
            if (codes.isEmpty()) {
                SwingUtilities.invokeLater(() -> refreshFromSettings(false));
                scheduleNextPoll(settings.getRefreshIntervalSeconds());
                return;
            }

            SinaQuoteFetcher.FetchResult result = quoteFetcher.fetchQuotes(codes);
            if (result.isSuccess()) {
                consecutiveFailures = 0;
                lastErrorSummary = null;
                lastSuccessRefreshTime = result.getRefreshTime();

                List<String> allCodes = settings.getStockCodes();
                Map<String, Quote> displayQuotes = new LinkedHashMap<>();
                Map<String, Quote> fetchedQuotes = result.getQuotes();
                for (String code : allCodes) {
                    Quote quote = fetchedQuotes.get(code);
                    if (quote == null) {
                        displayQuotes.put(code, emptyQuote(code));
                    } else {
                        displayQuotes.put(code, quote);
                    }
                }

                checkThresholdAndNotify(displayQuotes);

                SwingUtilities.invokeLater(() -> {
                    refreshFromSettings(true);
                    tableModel.setQuotes(displayQuotes);
                    updateStatusLabelText();
                });

                scheduleNextPoll(settings.getRefreshIntervalSeconds());
                return;
            }

            consecutiveFailures = Math.max(1, consecutiveFailures + 1);
            lastErrorSummary = result.getErrorSummary();
            SwingUtilities.invokeLater(this::updateStatusLabelText);
            long nextDelay = computeBackoffSeconds(settings.getRefreshIntervalSeconds(), consecutiveFailures);
            scheduleNextPoll(nextDelay);
        } catch (Exception e) {
            consecutiveFailures = Math.max(1, consecutiveFailures + 1);
            lastErrorSummary = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : (": " + e.getMessage().trim()));
            SwingUtilities.invokeLater(this::updateStatusLabelText);
            long nextDelay = computeBackoffSeconds(settings.getRefreshIntervalSeconds(), consecutiveFailures);
            scheduleNextPoll(nextDelay);
        }
    }

    private void scheduleNextPoll(long delaySeconds) {
        if (disposed) {
            return;
        }
        long delay = Math.max(0, delaySeconds);
        synchronized (scheduleLock) {
            if (disposed) {
                return;
            }
            scheduledFuture = pollingExecutor.schedule(this::pollOnceSafe, delay, TimeUnit.SECONDS);
        }
    }

    private static long computeBackoffSeconds(int baseSeconds, int failures) {
        long delay = Math.max(1, baseSeconds);
        int attempts = Math.max(1, failures);
        for (int i = 1; i < attempts; i++) {
            if (delay >= MAX_BACKOFF_SECONDS) {
                return MAX_BACKOFF_SECONDS;
            }
            delay = Math.min(MAX_BACKOFF_SECONDS, delay * 2);
        }
        return Math.min(MAX_BACKOFF_SECONDS, delay);
    }

    private void updateStatusLabelText() {
        StringBuilder sb = new StringBuilder();
        sb.append("最近刷新: ");
        if (lastSuccessRefreshTime == null || lastSuccessRefreshTime.isBlank()) {
            sb.append("未成功");
        } else {
            sb.append(lastSuccessRefreshTime);
        }
        String error = lastErrorSummary;
        if (error != null && !error.isBlank()) {
            sb.append("    错误: ").append(trimText(error.trim(), 160));
            if (consecutiveFailures > 0) {
                sb.append(" (连续失败 ").append(consecutiveFailures).append(" 次)");
            }
        }
        statusLabel.setText(sb.toString());
    }

    private static @NotNull String trimText(@NotNull String value, int maxLen) {
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLen));
    }

    private static @NotNull Quote emptyQuote(@NotNull String code) {
        return new Quote(code, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private void checkThresholdAndNotify(@NotNull Map<String, Quote> quotes) {
        Map<String, Double> thresholds = settings.getPerStockThresholdPct();
        if (thresholds.isEmpty()) {
            return;
        }
        for (String code : new ArrayList<>(lastNotifyMillisByCode.keySet())) {
            if (!thresholds.containsKey(code)) {
                lastNotifyMillisByCode.remove(code);
            }
        }

        long now = System.currentTimeMillis();
        int cooldownMinutes = settings.getCooldownMinutes();
        long cooldownMillis = Math.max(0, cooldownMinutes) * 60_000L;

        for (Map.Entry<String, Double> entry : thresholds.entrySet()) {
            String code = entry.getKey();
            Double threshold = entry.getValue();
            if (code == null || code.isBlank() || threshold == null || !Double.isFinite(threshold) || threshold <= 0) {
                continue;
            }
            Quote quote = quotes.get(code);
            if (quote == null) {
                continue;
            }
            Double changePct = quote.getChangePct();
            if (changePct == null || !Double.isFinite(changePct)) {
                continue;
            }
            if (Math.abs(changePct) < threshold) {
                continue;
            }
            if (cooldownMillis > 0) {
                Long last = lastNotifyMillisByCode.get(code);
                if (last != null && now - last < cooldownMillis) {
                    continue;
                }
            }
            lastNotifyMillisByCode.put(code, now);
            notifyThresholdTriggered(quote, threshold);
        }
    }

    private void notifyThresholdTriggered(@NotNull Quote quote, double threshold) {
        if (project.isDisposed()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("代码: ").append(escape(quote.getCode()));
        String name = quote.getName();
        if (name != null && !name.isBlank()) {
            sb.append("<br>名称: ").append(escape(name.trim()));
        } else {
            sb.append("<br>名称: ");
        }
        sb.append("<br>现价: ").append(formatDouble(quote.getPrice()));
        sb.append("<br>涨跌幅: ").append(formatDouble(quote.getChangePct())).append("%");
        sb.append("<br>阈值: ").append(String.format(Locale.ROOT, "%.2f", threshold)).append("%");
        String quoteDateTime = quote.getQuoteDateTime();
        sb.append("<br>行情时间: ").append(escape(quoteDateTime == null ? "" : quoteDateTime));
        String lastRefreshTime = quote.getLastRefreshTime();
        sb.append("<br>刷新时间: ").append(escape(lastRefreshTime == null ? "" : lastRefreshTime));
        String content = sb.toString();

        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP_ID)
                    .createNotification(content, NotificationType.INFORMATION)
                    .notify(project);
        });
    }

    private static @NotNull String escape(@NotNull String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static @NotNull String formatDouble(@Nullable Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "";
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class SwatchTableModel extends AbstractTableModel {
        private final Map<String, String> keyToLabel = StockWatcherColumn.getKeyToLabelMap();
        private List<String> columnKeys = StockWatcherColumn.getDefaultVisibleKeys();
        private List<String> rows = List.of();
        private Map<String, Quote> quotes = Map.of();

        private void setModelData(@NotNull List<String> columnKeys, @NotNull List<String> rows) {
            List<String> nextCols = List.copyOf(columnKeys);
            List<String> nextRows = List.copyOf(rows);
            if (this.columnKeys.equals(nextCols) && this.rows.equals(nextRows)) {
                return;
            }
            this.columnKeys = nextCols;
            this.rows = nextRows;
            fireTableStructureChanged();
        }

        private void setQuotes(@NotNull Map<String, Quote> quotes) {
            this.quotes = Map.copyOf(quotes);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columnKeys.size();
        }

        @Override
        public String getColumnName(int column) {
            String key = columnKeys.get(column);
            String label = keyToLabel.get(key);
            return label == null ? key : label;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return StockWatcherColumn.ACTIONS.getKey().equals(columnKeys.get(columnIndex));
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String key = columnKeys.get(columnIndex);
            String code = rows.get(rowIndex);
            Quote quote = quotes.get(code);

            if (StockWatcherColumn.CODE.getKey().equals(key)) {
                return code;
            }
            if (StockWatcherColumn.ACTIONS.getKey().equals(key)) {
                return "";
            }
            if (quote == null) {
                return "";
            }
            if (StockWatcherColumn.NAME.getKey().equals(key)) {
                return nullToEmpty(quote.getName());
            }
            if (StockWatcherColumn.PREV_CLOSE.getKey().equals(key)) {
                return formatDouble(quote.getPrevClose());
            }
            if (StockWatcherColumn.OPEN.getKey().equals(key)) {
                return formatDouble(quote.getOpen());
            }
            if (StockWatcherColumn.PRICE.getKey().equals(key)) {
                return formatDouble(quote.getPrice());
            }
            if (StockWatcherColumn.HIGH.getKey().equals(key)) {
                return formatDouble(quote.getHigh());
            }
            if (StockWatcherColumn.LOW.getKey().equals(key)) {
                return formatDouble(quote.getLow());
            }
            if (StockWatcherColumn.BID1_PRICE.getKey().equals(key)) {
                return formatDouble(quote.getBid1Price());
            }
            if (StockWatcherColumn.BID1_VOLUME.getKey().equals(key)) {
                return formatLong(quote.getBid1Volume());
            }
            if (StockWatcherColumn.ASK1_PRICE.getKey().equals(key)) {
                return formatDouble(quote.getAsk1Price());
            }
            if (StockWatcherColumn.ASK1_VOLUME.getKey().equals(key)) {
                return formatLong(quote.getAsk1Volume());
            }
            if (StockWatcherColumn.VOLUME.getKey().equals(key)) {
                return formatLong(quote.getVolume());
            }
            if (StockWatcherColumn.AMOUNT.getKey().equals(key)) {
                return formatDouble(quote.getAmount());
            }
            if (StockWatcherColumn.CHANGE.getKey().equals(key)) {
                return formatDouble(quote.getChange());
            }
            if (StockWatcherColumn.CHANGE_PCT.getKey().equals(key)) {
                return formatDouble(quote.getChangePct());
            }
            if (StockWatcherColumn.QUOTE_DATE.getKey().equals(key)) {
                return nullToEmpty(quote.getQuoteDate());
            }
            if (StockWatcherColumn.QUOTE_TIME.getKey().equals(key)) {
                return nullToEmpty(quote.getQuoteTime());
            }
            if (StockWatcherColumn.QUOTE_DATE_TIME.getKey().equals(key)) {
                return nullToEmpty(quote.getQuoteDateTime());
            }
            if (StockWatcherColumn.LAST_REFRESH_TIME.getKey().equals(key)) {
                return nullToEmpty(quote.getLastRefreshTime());
            }
            return "";
        }

        private static @NotNull String nullToEmpty(@Nullable String value) {
            return value == null ? "" : value;
        }

        private static @NotNull String formatDouble(@Nullable Double value) {
            if (value == null || !Double.isFinite(value)) {
                return "";
            }
            return String.format(Locale.ROOT, "%.2f", value);
        }

        private static @NotNull String formatLong(@Nullable Long value) {
            if (value == null) {
                return "";
            }
            return Long.toString(value);
        }

        private int getColumnIndex(@NotNull String key) {
            for (int i = 0; i < columnKeys.size(); i++) {
                if (key.equals(columnKeys.get(i))) {
                    return i;
                }
            }
            return -1;
        }

        private @Nullable String getRowCode(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }
    }

    private static final class ActionCellRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final JButton notifyButton = new JButton("通知");

        private ActionCellRenderer() {
            notifyButton.setEnabled(true);
            panel.add(notifyButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return panel;
        }
    }

    private final class ActionCellEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final JButton notifyButton = new JButton("通知");
        private int editingRow = -1;

        private ActionCellEditor() {
            notifyButton.setEnabled(true);
            panel.add(notifyButton);
            notifyButton.addActionListener(e -> {
                int row = editingRow;
                String code = tableModel.getRowCode(row);
                if (code != null) {
                    showThresholdDialog(code);
                }
                stopCellEditing();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    private void showThresholdDialog(@NotNull String code) {
        String normalizedCode = code.trim().toLowerCase(Locale.ROOT);
        Double current = settings.getPerStockThresholdPct().get(normalizedCode);
        String initial = current == null ? "" : String.format(Locale.ROOT, "%.2f", current);
        String input = Messages.showInputDialog(
                project,
                "设置单股涨跌幅阈值（%）。留空或输入 <= 0 将关闭该股票的通知。",
                "通知阈值 - " + normalizedCode,
                null,
                initial,
                null
        );
        if (input == null) {
            return;
        }
        ThresholdParseResult parsed = parseThresholdInput(input);
        if (!parsed.valid) {
            Messages.showErrorDialog(project, "阈值格式不正确，请输入数字（例如 3 或 3.5）。", "通知阈值");
            return;
        }
        if (parsed.value == null) {
            settings.setPerStockThresholdPct(normalizedCode, null);
            lastNotifyMillisByCode.remove(normalizedCode);
            return;
        }
        settings.setPerStockThresholdPct(normalizedCode, parsed.value);
        lastNotifyMillisByCode.remove(normalizedCode);
    }

    private static @NotNull ThresholdParseResult parseThresholdInput(@NotNull String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return ThresholdParseResult.remove();
        }
        if (trimmed.endsWith("%")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        try {
            double parsed = Double.parseDouble(trimmed);
            if (!Double.isFinite(parsed) || parsed <= 0) {
                return ThresholdParseResult.remove();
            }
            return ThresholdParseResult.of(parsed);
        } catch (NumberFormatException ignored) {
            return ThresholdParseResult.invalid();
        }
    }

    private static final class ThresholdParseResult {
        private final boolean valid;
        private final @Nullable Double value;

        private ThresholdParseResult(boolean valid, @Nullable Double value) {
            this.valid = valid;
            this.value = value;
        }

        private static @NotNull ThresholdParseResult of(double value) {
            return new ThresholdParseResult(true, value);
        }

        private static @NotNull ThresholdParseResult remove() {
            return new ThresholdParseResult(true, null);
        }

        private static @NotNull ThresholdParseResult invalid() {
            return new ThresholdParseResult(false, null);
        }
    }
}
