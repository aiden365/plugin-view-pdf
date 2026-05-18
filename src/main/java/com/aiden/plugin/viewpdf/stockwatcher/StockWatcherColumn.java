package com.aiden.plugin.viewpdf.stockwatcher;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum StockWatcherColumn {
    CODE("code", "代码"),
    NAME("name", "名称"),
    ACTIONS("actions", "操作"),
    PREV_CLOSE("prevClose", "昨收"),
    OPEN("open", "今开"),
    PRICE("price", "现价"),
    HIGH("high", "最高"),
    LOW("low", "最低"),
    BID1_PRICE("bid1Price", "买一价"),
    BID1_VOLUME("bid1Volume", "买一量"),
    ASK1_PRICE("ask1Price", "卖一价"),
    ASK1_VOLUME("ask1Volume", "卖一量"),
    VOLUME("volume", "成交量"),
    AMOUNT("amount", "成交额"),
    CHANGE("change", "涨跌额"),
    CHANGE_PCT("changePct", "涨跌幅%"),
    QUOTE_DATE("quoteDate", "行情日期"),
    QUOTE_TIME("quoteTime", "行情时间"),
    QUOTE_DATE_TIME("quoteDateTime", "行情时间戳"),
    LAST_REFRESH_TIME("lastRefreshTime", "最近刷新时间");

    private final String key;
    private final String label;

    StockWatcherColumn(@NotNull String key, @NotNull String label) {
        this.key = key;
        this.label = label;
    }

    public @NotNull String getKey() {
        return key;
    }

    public @NotNull String getLabel() {
        return label;
    }

    public static @NotNull List<String> getDefaultVisibleKeys() {
        return List.of(CODE.key, NAME.key, ACTIONS.key);
    }

    public static @NotNull Map<String, String> getKeyToLabelMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (StockWatcherColumn column : values()) {
            map.put(column.key, column.label);
        }
        return map;
    }
}
