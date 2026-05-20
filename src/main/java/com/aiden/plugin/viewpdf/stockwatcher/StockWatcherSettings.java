package com.aiden.plugin.viewpdf.stockwatcher;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@State(
        name = "StockWatcherSettings",
        storages = @Storage("stock-watcher.xml")
)
public final class StockWatcherSettings implements PersistentStateComponent<StockWatcherSettings.StateData> {
    private static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 5;
    private static final int DEFAULT_COOLDOWN_MINUTES = 5;
    private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("^(sh|sz|bj)\\d{6}$", Pattern.CASE_INSENSITIVE);

    public static final class StateData {
        public String stocks;
        public List<String> visibleColumns;
        public Integer refreshIntervalSeconds;
        public Integer cooldownMinutes;
        public Map<String, Double> perStockThresholdPct;
    }

    private StateData state = new StateData();

    public static @NotNull StockWatcherSettings getInstance() {
        return ApplicationManager.getApplication().getService(StockWatcherSettings.class);
    }

    @Override
    public @NotNull StateData getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull StateData state) {
        this.state = state;
        normalizeStateAfterLoad();
    }

    public @NotNull String getStocks() {
        String value = state.stocks;
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public void setStocks(@Nullable String stocksInput) {
        NormalizedStocks normalized = normalizeStocksInput(stocksInput);
        String next = normalized.normalizedText;
        if (Objects.equals(getStocks(), next)) {
            return;
        }
        state.stocks = next.isEmpty() ? null : next;
    }

    public @NotNull List<String> getStockCodes() {
        return normalizeStocksInput(getStocks()).codes;
    }

    public @NotNull List<String> getValidStockCodes() {
        List<String> all = getStockCodes();
        if (all.isEmpty()) {
            return List.of();
        }
        List<String> valid = new ArrayList<>();
        for (String code : all) {
            if (isValidStockCode(code)) {
                valid.add(code);
            }
        }
        return valid.isEmpty() ? List.of() : valid;
    }

    public @NotNull List<String> getInvalidStockCodes() {
        List<String> all = getStockCodes();
        if (all.isEmpty()) {
            return List.of();
        }
        List<String> invalid = new ArrayList<>();
        for (String code : all) {
            if (!isValidStockCode(code)) {
                invalid.add(code);
            }
        }
        return invalid.isEmpty() ? List.of() : invalid;
    }

    public @NotNull List<String> getVisibleColumns() {
        if (state.visibleColumns == null || state.visibleColumns.isEmpty()) {
            return StockWatcherColumn.getDefaultVisibleKeys();
        }
        return normalizeVisibleColumns(state.visibleColumns);
    }

    public void setVisibleColumns(@NotNull List<String> columns) {
        List<String> normalized = normalizeVisibleColumns(columns);
        if (getVisibleColumns().equals(normalized)) {
            return;
        }
        state.visibleColumns = normalized.equals(StockWatcherColumn.getDefaultVisibleKeys()) ? null : normalized;
    }

    public int getRefreshIntervalSeconds() {
        Integer value = state.refreshIntervalSeconds;
        if (value == null) {
            return DEFAULT_REFRESH_INTERVAL_SECONDS;
        }
        return clampRefreshIntervalSeconds(value);
    }

    public void setRefreshIntervalSeconds(int seconds) {
        int normalized = clampRefreshIntervalSeconds(seconds);
        if (getRefreshIntervalSeconds() == normalized) {
            return;
        }
        state.refreshIntervalSeconds = normalized;
    }

    public int getCooldownMinutes() {
        Integer value = state.cooldownMinutes;
        if (value == null) {
            return DEFAULT_COOLDOWN_MINUTES;
        }
        return clampCooldownMinutes(value);
    }

    public void setCooldownMinutes(int minutes) {
        int normalized = clampCooldownMinutes(minutes);
        if (getCooldownMinutes() == normalized) {
            return;
        }
        state.cooldownMinutes = normalized;
    }

    public @NotNull Map<String, Double> getPerStockThresholdPct() {
        Map<String, Double> map = state.perStockThresholdPct;
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Double> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            String code = normalizeNullableText(entry.getKey());
            if (code == null) {
                continue;
            }
            code = code.toLowerCase(Locale.ROOT);
            Double value = entry.getValue();
            if (value == null || !Double.isFinite(value) || value <= 0) {
                continue;
            }
            normalized.put(code, value);
        }
        return normalized.isEmpty() ? Map.of() : normalized;
    }

    public void setPerStockThresholdPct(@NotNull Map<String, Double> thresholds) {
        Map<String, Double> normalized = normalizeThresholdMap(thresholds);
        if (getPerStockThresholdPct().equals(normalized)) {
            return;
        }
        state.perStockThresholdPct = normalized.isEmpty() ? null : normalized;
    }

    public void setPerStockThresholdPct(@Nullable String code, @Nullable Double thresholdPct) {
        String key = normalizeNullableText(code);
        if (key == null) {
            return;
        }
        key = key.toLowerCase(Locale.ROOT);
        Map<String, Double> current = new LinkedHashMap<>(getPerStockThresholdPct());
        if (thresholdPct == null || !Double.isFinite(thresholdPct) || thresholdPct <= 0) {
            if (current.remove(key) == null) {
                return;
            }
        } else {
            Double previous = current.put(key, thresholdPct);
            if (previous != null && Double.compare(previous, thresholdPct) == 0) {
                return;
            }
        }
        state.perStockThresholdPct = current.isEmpty() ? null : current;
    }

    public static @NotNull NormalizedStocks normalizeStocksInput(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return new NormalizedStocks(List.of(), "");
        }
        String[] parts = input.split(",");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String part : parts) {
            String normalized = normalizeNullableText(part);
            if (normalized == null) {
                continue;
            }
            unique.add(normalized.toLowerCase(Locale.ROOT));
        }
        List<String> codes = unique.isEmpty() ? List.of() : new ArrayList<>(unique);
        String normalizedText = codes.isEmpty() ? "" : String.join(",", codes);
        return new NormalizedStocks(codes, normalizedText);
    }

    public static boolean isValidStockCode(@Nullable String code) {
        if (code == null) {
            return false;
        }
        String normalized = code.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        return STOCK_CODE_PATTERN.matcher(normalized).matches();
    }

    private void normalizeStateAfterLoad() {
        state.stocks = normalizeNullableText(state.stocks);
        List<String> columns = state.visibleColumns == null ? List.of() : normalizeVisibleColumns(state.visibleColumns);
        state.visibleColumns = columns.isEmpty() ? null : columns;
        state.refreshIntervalSeconds = clampRefreshIntervalSeconds(state.refreshIntervalSeconds == null ? DEFAULT_REFRESH_INTERVAL_SECONDS : state.refreshIntervalSeconds);
        state.cooldownMinutes = clampCooldownMinutes(state.cooldownMinutes == null ? DEFAULT_COOLDOWN_MINUTES : state.cooldownMinutes);
        Map<String, Double> thresholds = normalizeThresholdMap(state.perStockThresholdPct == null ? Map.of() : state.perStockThresholdPct);
        state.perStockThresholdPct = thresholds.isEmpty() ? null : thresholds;
    }

    private static int clampRefreshIntervalSeconds(int value) {
        return Math.max(1, Math.min(3600, value));
    }

    private static int clampCooldownMinutes(int value) {
        return Math.max(0, Math.min(24 * 60, value));
    }

    private static @Nullable String normalizeNullableText(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static @NotNull List<String> normalizeVisibleColumns(@Nullable List<String> values) {
        LinkedHashMap<String, Boolean> allowed = new LinkedHashMap<>();
        for (StockWatcherColumn col : StockWatcherColumn.values()) {
            allowed.put(col.getKey(), Boolean.TRUE);
        }
        LinkedHashMap<String, Boolean> unique = new LinkedHashMap<>();
        for (String def : StockWatcherColumn.getMandatoryKeys()) {
            unique.put(def, Boolean.TRUE);
        }
        if (values != null) {
            for (String value : values) {
                String normalized = normalizeNullableText(value);
                if (normalized == null) {
                    continue;
                }
                normalized = normalized.trim();
                if (!allowed.containsKey(normalized)) {
                    continue;
                }
                unique.put(normalized, Boolean.TRUE);
            }
        }
        return new ArrayList<>(unique.keySet());
    }

    private static @NotNull Map<String, Double> normalizeThresholdMap(@NotNull Map<String, Double> values) {
        LinkedHashMap<String, Double> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            String code = normalizeNullableText(entry.getKey());
            if (code == null) {
                continue;
            }
            code = code.toLowerCase(Locale.ROOT);
            Double value = entry.getValue();
            if (value == null || !Double.isFinite(value) || value <= 0) {
                continue;
            }
            normalized.put(code, value);
        }
        return normalized;
    }

    public static final class NormalizedStocks {
        public final @NotNull List<String> codes;
        public final @NotNull String normalizedText;

        private NormalizedStocks(@NotNull List<String> codes, @NotNull String normalizedText) {
            this.codes = codes;
            this.normalizedText = normalizedText;
        }
    }
}
