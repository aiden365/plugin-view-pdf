package com.aiden.plugin.viewpdf.stockwatcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Quote {
    private final @NotNull String code;
    private final @Nullable String name;
    private final @Nullable Double open;
    private final @Nullable Double prevClose;
    private final @Nullable Double price;
    private final @Nullable Double high;
    private final @Nullable Double low;
    private final @Nullable Double bid;
    private final @Nullable Double ask;
    private final @Nullable Long volume;
    private final @Nullable Double amount;
    private final @Nullable Long bid1Volume;
    private final @Nullable Double bid1Price;
    private final @Nullable Long ask1Volume;
    private final @Nullable Double ask1Price;
    private final @Nullable Double change;
    private final @Nullable Double changePct;
    private final @Nullable String quoteDate;
    private final @Nullable String quoteTime;
    private final @Nullable String quoteDateTime;
    private final @Nullable String lastRefreshTime;

    public Quote(
            @NotNull String code,
            @Nullable String name,
            @Nullable Double open,
            @Nullable Double prevClose,
            @Nullable Double price,
            @Nullable Double high,
            @Nullable Double low,
            @Nullable Double bid,
            @Nullable Double ask,
            @Nullable Long volume,
            @Nullable Double amount,
            @Nullable Long bid1Volume,
            @Nullable Double bid1Price,
            @Nullable Long ask1Volume,
            @Nullable Double ask1Price,
            @Nullable Double change,
            @Nullable Double changePct,
            @Nullable String quoteDate,
            @Nullable String quoteTime,
            @Nullable String quoteDateTime,
            @Nullable String lastRefreshTime
    ) {
        this.code = code;
        this.name = name;
        this.open = open;
        this.prevClose = prevClose;
        this.price = price;
        this.high = high;
        this.low = low;
        this.bid = bid;
        this.ask = ask;
        this.volume = volume;
        this.amount = amount;
        this.bid1Volume = bid1Volume;
        this.bid1Price = bid1Price;
        this.ask1Volume = ask1Volume;
        this.ask1Price = ask1Price;
        this.change = change;
        this.changePct = changePct;
        this.quoteDate = quoteDate;
        this.quoteTime = quoteTime;
        this.quoteDateTime = quoteDateTime;
        this.lastRefreshTime = lastRefreshTime;
    }

    public @NotNull String getCode() {
        return code;
    }

    public @Nullable String getName() {
        return name;
    }

    public @Nullable Double getPrevClose() {
        return prevClose;
    }

    public @Nullable Double getOpen() {
        return open;
    }

    public @Nullable Double getPrice() {
        return price;
    }

    public @Nullable Double getHigh() {
        return high;
    }

    public @Nullable Double getLow() {
        return low;
    }

    public @Nullable Double getBid() {
        return bid;
    }

    public @Nullable Double getAsk() {
        return ask;
    }

    public @Nullable Double getBid1Price() {
        return bid1Price;
    }

    public @Nullable Long getBid1Volume() {
        return bid1Volume;
    }

    public @Nullable Double getAsk1Price() {
        return ask1Price;
    }

    public @Nullable Long getAsk1Volume() {
        return ask1Volume;
    }

    public @Nullable Long getVolume() {
        return volume;
    }

    public @Nullable Double getAmount() {
        return amount;
    }

    public @Nullable Double getChange() {
        return change;
    }

    public @Nullable Double getChangePct() {
        return changePct;
    }

    public @Nullable String getQuoteDate() {
        return quoteDate;
    }

    public @Nullable String getQuoteTime() {
        return quoteTime;
    }

    public @Nullable String getQuoteDateTime() {
        return quoteDateTime;
    }

    public @Nullable String getLastRefreshTime() {
        return lastRefreshTime;
    }

    public @NotNull Quote withLastRefreshTime(@Nullable String lastRefreshTime) {
        if (this.lastRefreshTime == null ? lastRefreshTime == null : this.lastRefreshTime.equals(lastRefreshTime)) {
            return this;
        }
        return new Quote(
                code,
                name,
                open,
                prevClose,
                price,
                high,
                low,
                bid,
                ask,
                volume,
                amount,
                bid1Volume,
                bid1Price,
                ask1Volume,
                ask1Price,
                change,
                changePct,
                quoteDate,
                quoteTime,
                quoteDateTime,
                lastRefreshTime
        );
    }
}
