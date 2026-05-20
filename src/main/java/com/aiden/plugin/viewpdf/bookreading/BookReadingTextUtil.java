package com.aiden.plugin.viewpdf.bookreading;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class BookReadingTextUtil {
    private BookReadingTextUtil() {
    }

    public static @Nullable String readLine(@Nullable String content, int lineNumber) {
        if (content == null || content.isBlank()) {
            return null;
        }
        int normalizedLine = Math.max(1, lineNumber);
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            int current = 1;
            while ((line = reader.readLine()) != null) {
                if (current == normalizedLine) {
                    return line;
                }
                current++;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static @NotNull String trimToWidth(@NotNull String text, int maxWidthPx, @NotNull java.awt.FontMetrics metrics) {
        if (maxWidthPx <= 0) {
            return "";
        }
        if (metrics.stringWidth(text) <= maxWidthPx) {
            return text;
        }
        String ellipsis = "…";
        int ellipsisWidth = metrics.stringWidth(ellipsis);
        if (ellipsisWidth >= maxWidthPx) {
            return ellipsis;
        }
        int left = 0;
        int right = text.length();
        int best = 0;
        int target = maxWidthPx - ellipsisWidth;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            String candidate = text.substring(0, mid);
            int w = metrics.stringWidth(candidate);
            if (w <= target) {
                best = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        if (best <= 0) {
            return ellipsis;
        }
        return text.substring(0, best) + ellipsis;
    }

    public static @NotNull List<String> wrapToWidth(@NotNull String text, int maxWidthPx, @NotNull java.awt.FontMetrics metrics) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }
        if (maxWidthPx <= 0) {
            lines.add(text);
            return lines;
        }
        String remaining = text;
        while (!remaining.isEmpty()) {
            if (metrics.stringWidth(remaining) <= maxWidthPx) {
                lines.add(remaining);
                break;
            }
            int left = 1;
            int right = remaining.length();
            int best = 1;
            while (left <= right) {
                int mid = (left + right) >>> 1;
                String candidate = remaining.substring(0, mid);
                int w = metrics.stringWidth(candidate);
                if (w <= maxWidthPx) {
                    best = mid;
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
            int cut = best;
            int lastSpace = -1;
            for (int i = cut - 1; i >= 0; i--) {
                char c = remaining.charAt(i);
                if (Character.isWhitespace(c)) {
                    lastSpace = i;
                    break;
                }
            }
            if (lastSpace > 0) {
                cut = lastSpace;
            }
            String part = remaining.substring(0, cut).stripTrailing();
            if (!part.isEmpty()) {
                lines.add(part);
            }
            remaining = remaining.substring(cut).stripLeading();
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }
}
