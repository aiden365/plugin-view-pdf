package com.aiden.plugin.viewpdf.editorlookup;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class EditorWordLookupSession {
    private final Project project;
    private List<PdfViewerSettings.WordEntryData> activeWords = List.of();
    private int currentIndex = -1;

    public EditorWordLookupSession(@NotNull Project project) {
        this.project = project;
        refreshWordPool(false);
    }

    public synchronized @NotNull String getCurrentWordDisplayText() {
        refreshWordPool(true);
        PdfViewerSettings.WordEntryData current = getCurrentEntry();
        if (current == null || current.word == null || current.word.isBlank()) {
            return "暂无可学习单词";
        }
        return current.word;
    }

    public synchronized void moveNext() {
        refreshWordPool(true);
        if (activeWords.isEmpty()) {
            return;
        }
        int next = findNextIndexSkippingMastered(currentIndex);
        if (next < 0) {
            next = (currentIndex + 1 + activeWords.size()) % activeWords.size();
        }
        currentIndex = next;
    }

    public synchronized void movePrevious() {
        refreshWordPool(true);
        if (activeWords.isEmpty()) {
            return;
        }
        int fromIndex = currentIndex < 0 ? 0 : currentIndex;
        int prev = findPreviousIndexSkippingMastered(fromIndex);
        if (prev < 0) {
            prev = (fromIndex - 1 + activeWords.size()) % activeWords.size();
        }
        currentIndex = prev;
    }

    public synchronized void toggleLearn() {
        refreshWordPool(false);
        PdfViewerSettings.WordEntryData current = getCurrentEntry();
        if (current == null || current.word == null || current.word.isBlank()) {
            return;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        settings.toggleWordMastered(current);
        refreshWordPool(true);
    }

    private void refreshWordPool(boolean keepCurrentWord) {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        String bookKey = settings.getSelectedVocabularyBookKey();
        List<PdfViewerSettings.WordEntryData> source = settings.getWordEntries();
        List<PdfViewerSettings.WordEntryData> filtered = new ArrayList<>(source.size());

        for (PdfViewerSettings.WordEntryData entry : source) {
            if (entry == null || entry.word == null || entry.word.isBlank()) {
                continue;
            }
            if (settings.isWordHiddenInPopup(bookKey, entry.word)) {
                continue;
            }
            filtered.add(entry);
        }

        activeWords = List.copyOf(filtered);
        if (activeWords.isEmpty()) {
            currentIndex = -1;
            return;
        }

        if (keepCurrentWord) {
            String currentKey = normalizeWordKey(getCurrentWordText());
            if (currentKey != null) {
                for (int i = 0; i < activeWords.size(); i++) {
                    String entryKey = normalizeWordKey(activeWords.get(i).word);
                    if (Objects.equals(currentKey, entryKey)) {
                        currentIndex = i;
                        return;
                    }
                }
            }
        }

        int firstPending = findNextIndexSkippingMastered(-1);
        currentIndex = firstPending >= 0 ? firstPending : 0;
    }

    private int findNextIndexSkippingMastered(int fromIndex) {
        if (activeWords.isEmpty()) {
            return -1;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        int size = activeWords.size();
        for (int i = 1; i <= size; i++) {
            int candidate = (fromIndex + i + size) % size;
            PdfViewerSettings.WordEntryData entry = activeWords.get(candidate);
            if (!settings.isWordMastered(entry.word)) {
                return candidate;
            }
        }
        return -1;
    }

    private int findPreviousIndexSkippingMastered(int fromIndex) {
        if (activeWords.isEmpty()) {
            return -1;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        int size = activeWords.size();
        for (int i = 1; i <= size; i++) {
            int candidate = (fromIndex - i + size) % size;
            PdfViewerSettings.WordEntryData entry = activeWords.get(candidate);
            if (!settings.isWordMastered(entry.word)) {
                return candidate;
            }
        }
        return -1;
    }

    private @Nullable PdfViewerSettings.WordEntryData getCurrentEntry() {
        if (activeWords.isEmpty() || currentIndex < 0 || currentIndex >= activeWords.size()) {
            return null;
        }
        return activeWords.get(currentIndex);
    }

    private @Nullable String getCurrentWordText() {
        PdfViewerSettings.WordEntryData entry = getCurrentEntry();
        return entry == null ? null : entry.word;
    }

    private static @Nullable String normalizeWordKey(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
