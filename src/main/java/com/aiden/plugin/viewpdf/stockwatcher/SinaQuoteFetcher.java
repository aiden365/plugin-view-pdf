package com.aiden.plugin.viewpdf.stockwatcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class SinaQuoteFetcher {
    private static final Charset FALLBACK_CHARSET = Charset.forName("GBK");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{2}:\\d{2}:\\d{2}");
    private static final DateTimeFormatter REFRESH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HttpClient httpClient;

    public SinaQuoteFetcher() {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
    }

    public SinaQuoteFetcher(@NotNull HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public @NotNull FetchResult fetchQuotes(@NotNull List<String> codes) {
        List<String> normalized = normalizeCodes(codes);
        LinkedHashMap<String, Quote> result = new LinkedHashMap<>();
        for (String code : normalized) {
            result.put(code, emptyQuote(code));
        }
        if (normalized.isEmpty()) {
            return new FetchResult(true, result, null, null);
        }
        try {
            URI uri = URI.create("https://hq.sinajs.cn/list=" + String.join(",", normalized));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "*/*")
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Referer", "https://finance.sina.com.cn")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                return new FetchResult(false, result, "HTTP " + statusCode, null);
            }
            Charset charset = resolveCharset(response.headers());
            String body = new String(response.body(), charset);
            String refreshTime = LocalDateTime.now().format(REFRESH_TIME_FORMATTER);
            for (Map.Entry<String, Quote> entry : parseResponseBody(body).entrySet()) {
                result.put(entry.getKey(), entry.getValue().withLastRefreshTime(refreshTime));
            }
            for (String code : normalized) {
                Quote quote = result.get(code);
                if (quote != null && quote.getLastRefreshTime() == null) {
                    result.put(code, quote.withLastRefreshTime(refreshTime));
                }
            }
            return new FetchResult(true, result, null, refreshTime);
        } catch (Exception e) {
            String summary = e.getClass().getSimpleName();
            String msg = e.getMessage();
            if (msg != null && !msg.isBlank()) {
                summary = summary + ": " + msg.trim();
            }
            return new FetchResult(false, result, summary, null);
        }
    }

    private static @NotNull Map<String, Quote> parseResponseBody(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        LinkedHashMap<String, Quote> map = new LinkedHashMap<>();
        String[] lines = body.split("\\r?\\n");
        for (String rawLine : lines) {
            if (rawLine == null) {
                continue;
            }
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            ParsedLine parsed = parseLine(line);
            if (parsed == null) {
                continue;
            }
            map.put(parsed.code, parsed.quote);
        }
        return map;
    }

    private static @Nullable ParsedLine parseLine(@NotNull String line) {
        int varIndex = line.indexOf("var hq_str_");
        if (varIndex < 0) {
            return null;
        }
        int eqIndex = line.indexOf('=', varIndex);
        if (eqIndex < 0) {
            return null;
        }
        String code = line.substring(varIndex + "var hq_str_".length(), eqIndex).trim();
        if (code.isEmpty()) {
            return null;
        }
        int firstQuote = line.indexOf('"', eqIndex);
        int lastQuote = line.lastIndexOf('"');
        if (firstQuote < 0 || lastQuote <= firstQuote) {
            return new ParsedLine(code, emptyQuote(code));
        }
        String payload = line.substring(firstQuote + 1, lastQuote);
        Quote quote = parsePayload(code, payload);
        return new ParsedLine(code, quote);
    }

    private static @NotNull Quote parsePayload(@NotNull String code, @Nullable String payload) {
        if (payload == null || payload.isBlank()) {
            return emptyQuote(code);
        }
        String[] fields = payload.split(",", -1);
        String name = getField(fields, 0);
        Double open = parseDouble(getField(fields, 1));
        Double prevClose = parseDouble(getField(fields, 2));
        Double price = parseDouble(getField(fields, 3));
        Double high = parseDouble(getField(fields, 4));
        Double low = parseDouble(getField(fields, 5));
        Double bid = parseDouble(getField(fields, 6));
        Double ask = parseDouble(getField(fields, 7));
        Long volume = parseLong(getField(fields, 8));
        Double amount = parseDouble(getField(fields, 9));

        Long bid1Volume = parseLong(getField(fields, 10));
        Double bid1Price = parseDouble(getField(fields, 11));
        Long ask1Volume = parseLong(getField(fields, 20));
        Double ask1Price = parseDouble(getField(fields, 21));

        if (bid1Price == null) {
            bid1Price = bid;
        }
        if (ask1Price == null) {
            ask1Price = ask;
        }

        String quoteDate = getDateField(fields);
        String quoteTime = getTimeField(fields);
        String quoteDateTime = (quoteDate == null || quoteTime == null) ? null : quoteDate + " " + quoteTime;

        Double change = null;
        Double changePct = null;
        if (price != null && prevClose != null && Double.isFinite(price) && Double.isFinite(prevClose) && prevClose != 0) {
            change = price - prevClose;
            changePct = change / prevClose * 100d;
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
                null
        );
    }

    private static @NotNull Quote emptyQuote(@NotNull String code) {
        return new Quote(code, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static @Nullable String getField(@NotNull String[] fields, int index) {
        if (index < 0 || index >= fields.length) {
            return null;
        }
        String value = fields[index];
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static @Nullable Double parseDouble(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static @Nullable Long parseLong(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static @Nullable String getDateField(@NotNull String[] fields) {
        String candidate = getField(fields, 30);
        if (candidate != null && DATE_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }
        for (int i = fields.length - 1; i >= 0; i--) {
            String value = getField(fields, i);
            if (value != null && DATE_PATTERN.matcher(value).matches()) {
                return value;
            }
        }
        return null;
    }

    private static @Nullable String getTimeField(@NotNull String[] fields) {
        String candidate = getField(fields, 31);
        if (candidate != null && TIME_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }
        for (int i = fields.length - 1; i >= 0; i--) {
            String value = getField(fields, i);
            if (value != null && TIME_PATTERN.matcher(value).matches()) {
                return value;
            }
        }
        return null;
    }

    private static @NotNull Charset resolveCharset(@NotNull HttpHeaders headers) {
        Optional<String> contentType = headers.firstValue("Content-Type");
        if (contentType.isEmpty()) {
            return FALLBACK_CHARSET;
        }
        String value = contentType.get();
        String[] parts = value.split(";");
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("charset=")) {
                continue;
            }
            String charsetName = trimmed.substring("charset=".length()).trim();
            if (charsetName.startsWith("\"") && charsetName.endsWith("\"") && charsetName.length() >= 2) {
                charsetName = charsetName.substring(1, charsetName.length() - 1);
            }
            if (charsetName.isEmpty()) {
                continue;
            }
            try {
                return Charset.forName(charsetName);
            } catch (Exception ignored) {
                return FALLBACK_CHARSET;
            }
        }
        return FALLBACK_CHARSET;
    }

    private static @NotNull List<String> normalizeCodes(@NotNull List<String> codes) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String code : codes) {
            if (code == null) {
                continue;
            }
            String trimmed = code.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                unique.add(trimmed);
            }
        }
        return unique.isEmpty() ? List.of() : List.copyOf(unique);
    }

    private static final class ParsedLine {
        private final @NotNull String code;
        private final @NotNull Quote quote;

        private ParsedLine(@NotNull String code, @NotNull Quote quote) {
            this.code = code;
            this.quote = quote;
        }
    }

    public static final class FetchResult {
        private final boolean success;
        private final @NotNull Map<String, Quote> quotes;
        private final @Nullable String errorSummary;
        private final @Nullable String refreshTime;

        private FetchResult(boolean success, @NotNull Map<String, Quote> quotes, @Nullable String errorSummary, @Nullable String refreshTime) {
            this.success = success;
            this.quotes = quotes;
            this.errorSummary = errorSummary;
            this.refreshTime = refreshTime;
        }

        public boolean isSuccess() {
            return success;
        }

        public @NotNull Map<String, Quote> getQuotes() {
            return quotes;
        }

        public @Nullable String getErrorSummary() {
            return errorSummary;
        }

        public @Nullable String getRefreshTime() {
            return refreshTime;
        }
    }
}
