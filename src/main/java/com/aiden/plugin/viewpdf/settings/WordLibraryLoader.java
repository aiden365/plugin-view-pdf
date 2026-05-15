package com.aiden.plugin.viewpdf.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WordLibraryLoader {
    private static final String VOCABULARY_RESOURCE_DIR = "vocabularies/";
    private static final String SOURCE_BUILTIN = "built-in";
    private static final String SOURCE_CUSTOM = "custom";
    private static final String BUILTIN_KEY_PREFIX = "builtin:";
    private static final String CUSTOM_KEY_PREFIX = "custom:";
    private static final String SYSTEM_MASTERED_KEY = "system:mastered";
    private static final List<String> BUILTIN_VOCABULARY_BOOKS = List.of(
            "CET4luan_2",
            "CET6_2",
            "KaoYan_2",
            "GRE_2",
            "IELTSluan_2",
            "TOEFL_2",
            "Level4luan_2",
            "Level8luan_2"
    );
    private static final Pattern HEAD_WORD_PATTERN = Pattern.compile("\"headWord\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern WORD_PATTERN = Pattern.compile("\"word\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern TRAN_CN_PATTERN = Pattern.compile("\"tranCn\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern US_PHONE_PATTERN = Pattern.compile("\"usphone\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern UK_PHONE_PATTERN = Pattern.compile("\"ukphone\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern SENTENCE_EN_PATTERN = Pattern.compile("\"sContent\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern TRANS_OBJECT_PATTERN = Pattern.compile("\\{[^{}]*\"tranCn\"\\s*:[^{}]*\\}");
    private static final Pattern POS_PATTERN = Pattern.compile("\"pos\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern SYNOS_BLOCK_PATTERN = Pattern.compile("\"synos\"\\s*:\\s*\\[(.*?)]\\s*(,|})");
    private static final Pattern SYNONYM_ITEM_PATTERN = Pattern.compile("\\{[^{}]*\"pos\"\\s*:[^{}]*\"hwds\"\\s*:\\s*\\[(.*?)]\\s*\\}", Pattern.DOTALL);
    private static final Pattern HWD_PATTERN = Pattern.compile("\"w\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private WordLibraryLoader() {
    }

    public static final class ValidationResult {
        public final boolean valid;
        public final int totalLines;
        public final int validLines;
        public final int invalidLines;
        public final @NotNull List<Integer> sampleErrorLines;
        public final @Nullable String firstErrorMessage;

        private ValidationResult(
                boolean valid,
                int totalLines,
                int validLines,
                int invalidLines,
                @NotNull List<Integer> sampleErrorLines,
                @Nullable String firstErrorMessage
        ) {
            this.valid = valid;
            this.totalLines = totalLines;
            this.validLines = validLines;
            this.invalidLines = invalidLines;
            this.sampleErrorLines = sampleErrorLines;
            this.firstErrorMessage = firstErrorMessage;
        }
    }

    public static @NotNull List<String> getBuiltinVocabularyBooks() {
        return BUILTIN_VOCABULARY_BOOKS;
    }

    public static @NotNull String getSystemMasteredBookKey() {
        return SYSTEM_MASTERED_KEY;
    }

    public static void reloadWordEntriesFromSettings(@NotNull PdfViewerSettings settings) {
        List<PdfViewerSettings.WordEntryData> merged = loadMergedWordEntries(settings);
        settings.setWordEntries(merged);
    }

    public static @NotNull List<PdfViewerSettings.WordEntryData> loadMergedWordEntries(@NotNull PdfViewerSettings settings) {
        String selectedKey = normalizeSelectedKey(settings.getSelectedVocabularyBookKey());
        List<PdfViewerSettings.WordEntryData> loaded;
        if (selectedKey.startsWith(BUILTIN_KEY_PREFIX)) {
            String bookId = selectedKey.substring(BUILTIN_KEY_PREFIX.length());
            loaded = loadVocabularyBookEntries(normalizeBuiltinBookId(bookId));
        } else if (selectedKey.startsWith(CUSTOM_KEY_PREFIX)) {
            String bookName = selectedKey.substring(CUSTOM_KEY_PREFIX.length());
            loaded = loadCustomBookEntriesByName(settings, bookName);
        } else if (SYSTEM_MASTERED_KEY.equals(selectedKey)) {
            loaded = loadMasteredEntries(settings);
        } else {
            loaded = loadVocabularyBookEntries(BUILTIN_VOCABULARY_BOOKS.get(0));
        }
        LinkedHashMap<String, PdfViewerSettings.WordEntryData> unique = new LinkedHashMap<>();
        for (PdfViewerSettings.WordEntryData entry : loaded) {
            String key = normalizeWordKey(entry.word);
            if (key != null) {
                unique.putIfAbsent(key, entry);
            }
        }
        return new ArrayList<>(unique.values());
    }

    public static @NotNull ValidationResult validateCustomJsonl(@NotNull String pathText) {
        Path path = Path.of(pathText);
        if (!Files.isRegularFile(path)) {
            return new ValidationResult(false, 0, 0, 1, List.of(0), "文件不存在或不是普通文件");
        }
        int total = 0;
        int valid = 0;
        int invalid = 0;
        String firstError = null;
        List<Integer> sampleLines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                total++;
                ParseResult parsed = parseCustomJsonLine(trimmed, "custom-validate", path.toString());
                if (parsed.error == null && normalizeWordKey(parsed.entry.word) != null && parsed.entry.meaning != null) {
                    valid++;
                } else {
                    invalid++;
                    if (firstError == null) {
                        firstError = parsed.error == null ? "缺少必要字段 word/trans" : parsed.error;
                    }
                    if (sampleLines.size() < 5) {
                        sampleLines.add(lineNo);
                    }
                }
            }
        } catch (IOException e) {
            return new ValidationResult(false, total, valid, invalid + 1, List.of(0), "文件读取失败: " + e.getMessage());
        }
        boolean ok = total > 0 && invalid == 0;
        if (total == 0 && firstError == null) {
            firstError = "文件内容为空";
        }
        return new ValidationResult(ok, total, valid, invalid, sampleLines, firstError);
    }

    private static @NotNull String normalizeSelectedKey(@Nullable String key) {
        if (key == null || key.isBlank()) {
            return BUILTIN_KEY_PREFIX + BUILTIN_VOCABULARY_BOOKS.get(0);
        }
        return key.trim();
    }

    private static @NotNull String normalizeBuiltinBookId(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return BUILTIN_VOCABULARY_BOOKS.get(0);
        }
        String normalized = value.trim();
        for (String supported : BUILTIN_VOCABULARY_BOOKS) {
            if (supported.equalsIgnoreCase(normalized)) {
                return supported;
            }
        }
        return BUILTIN_VOCABULARY_BOOKS.get(0);
    }

    private static @NotNull List<PdfViewerSettings.WordEntryData> loadCustomBookEntriesByName(
            @NotNull PdfViewerSettings settings,
            @NotNull String bookName
    ) {
        String targetName = bookName.trim().toLowerCase(Locale.ROOT);
        for (PdfViewerSettings.CustomVocabularyBookData book : settings.getCustomVocabularyBooks()) {
            if (book == null || book.name == null || book.jsonlPath == null) {
                continue;
            }
            if (book.name.trim().toLowerCase(Locale.ROOT).equals(targetName)) {
                return loadCustomJsonlEntries(book.name, book.jsonlPath);
            }
        }
        return List.of();
    }

    private static @NotNull List<PdfViewerSettings.WordEntryData> loadMasteredEntries(@NotNull PdfViewerSettings settings) {
        return MasteredWordLibrary.loadAll();
    }

    private static @NotNull List<PdfViewerSettings.WordEntryData> loadCustomJsonlEntries(
            @NotNull String bookName,
            @NotNull String pathText
    ) {
        Path path = Path.of(pathText);
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        List<PdfViewerSettings.WordEntryData> entries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                ParseResult parsed = parseCustomJsonLine(trimmed, bookName, path.toString());
                if (parsed.error == null && normalizeWordKey(parsed.entry.word) != null) {
                    entries.add(parsed.entry);
                }
            }
            return entries;
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static @Nullable String normalizeWordKey(@Nullable String word) {
        if (word == null) {
            return null;
        }
        String normalized = word.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static @NotNull List<PdfViewerSettings.WordEntryData> loadVocabularyBookEntries(@NotNull String bookId) {
        String resourcePath = VOCABULARY_RESOURCE_DIR + bookId + ".json";
        InputStream stream = WordLibraryLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            return List.of();
        }
        List<PdfViewerSettings.WordEntryData> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                PdfViewerSettings.WordEntryData entry = parseBuiltinVocabularyJsonLine(trimmed, bookId, resourcePath);
                if (normalizeWordKey(entry.word) != null) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static @NotNull PdfViewerSettings.WordEntryData parseBuiltinVocabularyJsonLine(
            @NotNull String jsonLine,
            @NotNull String bookId,
            @NotNull String sourceRef
    ) {
        PdfViewerSettings.WordEntryData entry = new PdfViewerSettings.WordEntryData();
        entry.word = extractFirstValue(jsonLine, HEAD_WORD_PATTERN);
        entry.meaning = extractMeaningSummary(jsonLine);
        entry.phonetic = extractPhonetic(jsonLine);
        entry.difficulty = bookId;
        entry.theme = "builtin-book";
        entry.source = SOURCE_BUILTIN;
        entry.sourceRef = sourceRef;
        entry.status = "new";
        entry.sentenceEnList = extractEnglishSentences(jsonLine);
        entry.synonymsByPos = extractSynonymsByPos(jsonLine);
        return entry;
    }

    private static final class ParseResult {
        private final PdfViewerSettings.WordEntryData entry;
        private final @Nullable String error;

        private ParseResult(@NotNull PdfViewerSettings.WordEntryData entry, @Nullable String error) {
            this.entry = entry;
            this.error = error;
        }
    }

    private static @NotNull ParseResult parseCustomJsonLine(
            @NotNull String jsonLine,
            @NotNull String bookName,
            @NotNull String sourceRef
    ) {
        PdfViewerSettings.WordEntryData entry = new PdfViewerSettings.WordEntryData();
        entry.word = extractFirstValue(jsonLine, WORD_PATTERN);
        if (entry.word == null || entry.word.isBlank()) {
            return new ParseResult(entry, "缺少 word 字段");
        }
        LinkedHashMap<String, List<String>> transByPos = extractTransByPos(jsonLine);
        if (transByPos.isEmpty()) {
            return new ParseResult(entry, "缺少 trans[].tranCn 字段");
        }
        entry.meaning = composeMeaning(transByPos);
        entry.phonetic = null;
        entry.difficulty = bookName;
        entry.theme = "custom-book";
        entry.source = SOURCE_CUSTOM;
        entry.sourceRef = sourceRef;
        entry.status = "new";
        entry.sentenceEnList = List.of();
        entry.synonymsByPos = List.of();
        return new ParseResult(entry, null);
    }

    private static @NotNull LinkedHashMap<String, List<String>> extractTransByPos(@NotNull String jsonLine) {
        Matcher transMatcher = TRANS_OBJECT_PATTERN.matcher(jsonLine);
        LinkedHashMap<String, List<String>> byPos = new LinkedHashMap<>();
        while (transMatcher.find()) {
            String transObj = transMatcher.group();
            String tranCn = extractFirstValue(transObj, TRAN_CN_PATTERN);
            if (tranCn == null || tranCn.isBlank()) {
                continue;
            }
            String pos = extractFirstValue(transObj, POS_PATTERN);
            String posKey = (pos == null || pos.isBlank()) ? "释义" : pos.trim();
            List<String> values = byPos.computeIfAbsent(posKey, ignored -> new ArrayList<>());
            for (String value : splitChineseMeaning(tranCn)) {
                if (!value.isBlank()) {
                    values.add(value.trim());
                }
            }
        }
        return byPos;
    }

    private static @NotNull String composeMeaning(@NotNull LinkedHashMap<String, List<String>> byPos) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : byPos.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append(entry.getKey()).append(".1 ").append(String.join("，", entry.getValue()));
        }
        return sb.toString();
    }

    private static @Nullable String extractFirstValue(@NotNull String source, @NotNull Pattern pattern) {
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJsonString(matcher.group(1));
    }

    private static @Nullable String unescapeJsonString(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch != '\\') {
                sb.append(ch);
                continue;
            }
            if (i + 1 >= raw.length()) {
                break;
            }
            char next = raw.charAt(++i);
            switch (next) {
                case '"':
                case '\\':
                case '/':
                    sb.append(next);
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'u':
                    if (i + 4 < raw.length()) {
                        String hex = raw.substring(i + 1, i + 5);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException ignored) {
                            sb.append("\\u").append(hex);
                            i += 4;
                        }
                    } else {
                        sb.append("\\u");
                    }
                    break;
                default:
                    sb.append(next);
                    break;
            }
        }
        String normalized = sb.toString().trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static @Nullable String extractPhonetic(@NotNull String jsonLine) {
        String us = extractFirstValue(jsonLine, US_PHONE_PATTERN);
        if (us != null) {
            return us;
        }
        return extractFirstValue(jsonLine, UK_PHONE_PATTERN);
    }

    private static @Nullable String extractMeaningSummary(@NotNull String jsonLine) {
        Matcher transMatcher = TRANS_OBJECT_PATTERN.matcher(jsonLine);
        LinkedHashMap<String, List<String>> byPos = new LinkedHashMap<>();
        while (transMatcher.find()) {
            String transObj = transMatcher.group();
            String tranCn = extractFirstValue(transObj, TRAN_CN_PATTERN);
            if (tranCn == null) {
                continue;
            }
            String pos = extractFirstValue(transObj, POS_PATTERN);
            String posKey = pos == null ? "释义" : pos.trim();
            List<String> meanings = byPos.computeIfAbsent(posKey, ignored -> new ArrayList<>());
            for (String part : splitChineseMeaning(tranCn)) {
                if (!part.isBlank()) {
                    meanings.add(part.trim());
                }
            }
        }
        if (byPos.isEmpty()) {
            String firstTran = extractFirstValue(jsonLine, TRAN_CN_PATTERN);
            return firstTran == null ? null : "释义.1 " + firstTran;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : byPos.entrySet()) {
            List<String> meanings = entry.getValue();
            if (meanings.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append(entry.getKey()).append(".1 ").append(String.join("，", meanings));
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static @NotNull List<String> splitChineseMeaning(@NotNull String meaning) {
        String normalized = meaning.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        String[] parts = normalized.split("[；;，,、]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String value = part == null ? "" : part.trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result.isEmpty() ? List.of(normalized) : result;
    }

    private static @NotNull List<String> extractEnglishSentences(@NotNull String jsonLine) {
        LinkedHashSet<String> sentences = new LinkedHashSet<>();
        Matcher sentenceMatcher = SENTENCE_EN_PATTERN.matcher(jsonLine);
        while (sentenceMatcher.find()) {
            String sentence = unescapeJsonString(sentenceMatcher.group(1));
            if (sentence != null && !sentence.isBlank()) {
                sentences.add(sentence.trim());
            }
        }
        return new ArrayList<>(sentences);
    }

    private static @NotNull List<PdfViewerSettings.WordSynonymGroupData> extractSynonymsByPos(@NotNull String jsonLine) {
        Matcher synosBlockMatcher = SYNOS_BLOCK_PATTERN.matcher(jsonLine);
        if (!synosBlockMatcher.find()) {
            return List.of();
        }
        String synosRaw = synosBlockMatcher.group(1);
        Matcher itemMatcher = SYNONYM_ITEM_PATTERN.matcher(synosRaw);
        LinkedHashMap<String, LinkedHashSet<String>> grouped = new LinkedHashMap<>();
        while (itemMatcher.find()) {
            String itemRaw = itemMatcher.group();
            String pos = extractFirstValue(itemRaw, POS_PATTERN);
            if (pos == null || pos.isBlank()) {
                continue;
            }
            LinkedHashSet<String> words = grouped.computeIfAbsent(pos.trim(), ignored -> new LinkedHashSet<>());
            Matcher hwdMatcher = HWD_PATTERN.matcher(itemRaw);
            while (hwdMatcher.find()) {
                String word = unescapeJsonString(hwdMatcher.group(1));
                if (word != null && !word.isBlank()) {
                    words.add(word.trim());
                }
            }
        }
        if (grouped.isEmpty()) {
            return List.of();
        }
        List<PdfViewerSettings.WordSynonymGroupData> groups = new ArrayList<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            PdfViewerSettings.WordSynonymGroupData group = new PdfViewerSettings.WordSynonymGroupData();
            group.pos = entry.getKey();
            group.words = new ArrayList<>(entry.getValue());
            groups.add(group);
        }
        return groups;
    }
}
