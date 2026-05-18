package com.aiden.plugin.viewpdf.stockwatcher;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class StockWatcherSettingsTest {
    @Test
    public void normalizeStocksInput_returnsEmptyForNullOrBlank() {
        StockWatcherSettings.NormalizedStocks normalizedNull = StockWatcherSettings.normalizeStocksInput(null);
        Assert.assertEquals(List.of(), normalizedNull.codes);
        Assert.assertEquals("", normalizedNull.normalizedText);

        StockWatcherSettings.NormalizedStocks normalizedBlank = StockWatcherSettings.normalizeStocksInput("   ");
        Assert.assertEquals(List.of(), normalizedBlank.codes);
        Assert.assertEquals("", normalizedBlank.normalizedText);
    }

    @Test
    public void normalizeStocksInput_trimsLowercasesAndDeduplicates() {
        StockWatcherSettings.NormalizedStocks normalized = StockWatcherSettings.normalizeStocksInput(" sh600000, SZ000001 , ,bj123456,sh600000 ");
        Assert.assertEquals(List.of("sh600000", "sz000001", "bj123456"), normalized.codes);
        Assert.assertEquals("sh600000,sz000001,bj123456", normalized.normalizedText);
    }
}
