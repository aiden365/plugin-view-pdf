package com.aiden.plugin.viewpdf.settings;

import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MasteredWordLibrary {
    private static final Object LOCK = new Object();
    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(null|\"((?:\\\\.|[^\"\\\\])*)\")");
    private static final Pattern SENTENCE_LIST_PATTERN = Pattern.compile("\"sentenceEnList\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern SYNONYM_GROUPS_PATTERN = Pattern.compile("\"synonymsByPos\"\\s*:\\s*\\[(.*)]\\s*\\}\\s*$", Pattern.DOTALL);
    private static final Pattern SYNONYM_GROUP_PATTERN = Pattern.compile("\\{[^{}]*\"pos\"\\s*:\\s*(null|\"((?:\\\\.|[^\"\\\\])*)\")[^{}]*\"words\"\\s*:\\s*\\[(.*?)]\\s*\\}", Pattern.DOTALL);
    private static final Pattern QUOTED_STRING_PATTERN = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");

    private MasteredWordLibrary() {
    }

    public static @NotNull Path getMasteredJsonlPath() {
        return Path.of(PathManager.getConfigPath(), "xcode-tools", "mastered.jsonl");
    }

    public static @NotNull List<PdfViewerSettings.WordEntryData> loadAll() {
        synchronized (LOCK) {
            return new ArrayList<>(loadAsMap().values());
        }
    }

    public static void upsert(@NotNull PdfViewerSettings.WordEntryData entry) {
        synchronized (LOCK) {
            String key = normalizeWordKey(entry.word);
            if (key == null) {
                return;
            }
            LinkedHashMap<String, PdfViewerSettings.WordEntryData> map = loadAsMap();
            map.put(key, normalizeEntry(entry));
            writeAll(map);
        }
    }

    public static void remove(@Nullable String word) {
        synchronized (LOCK) {
            String key = normalizeWordKey(word);
            if (key == null) {
                return;
            }
            LinkedHashMap<String, PdfViewerSettings.WordEntryData> map = loadAsMap();
            if (map.remove(key) == null) {
                return;
            }
            writeAll(map);
        }
    }

    private static @NotNull LinkedHashMap<String, PdfViewerSettings.WordEntryData> loadAsMap() {
        Path path = getMasteredJsonlPath();
        if (!Files.isRegularFile(path)) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, PdfViewerSettings.WordEntryData> map = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                PdfViewerSettings.WordEntryData parsed = parseLine(trimmed);
                String key = normalizeWordKey(parsed.word);
                if (key == null) {
                    continue;
                }
                map.put(key, normalizeEntry(parsed));
            }
        } catch (IOException ignored) {
            return new LinkedHashMap<>();
        }
        return map;
    }

    private static void writeAll(@NotNull Map<String, PdfViewerSettings.WordEntryData> map) {
        Path path = getMasteredJsonlPath();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ignored) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (PdfViewerSettings.WordEntryData entry : map.values()) {
                String json = toJsonLine(entry);
                if (json == null) {
                    continue;
                }
                writer.write(json);
                writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    private static @NotNull PdfViewerSettings.WordEntryData parseLine(@NotNull String jsonLine) {
        PdfViewerSettings.WordEntryData entry = new PdfViewerSettings.WordEntryData();
        entry.word = readStringField(jsonLine, "word");
        entry.meaning = readStringField(jsonLine, "meaning");
        entry.phonetic = readStringField(jsonLine, "phonetic");
        entry.difficulty = readStringField(jsonLine, "difficulty");
        entry.theme = readStringField(jsonLine, "theme");
        entry.source = readStringField(jsonLine, "source");
        entry.sourceRef = readStringField(jsonLine, "sourceRef");
        entry.status = readStringField(jsonLine, "status");
        entry.sentenceEnList = readStringArray(jsonLine, SENTENCE_LIST_PATTERN);
        entry.synonymsByPos = readSynonymGroups(jsonLine);
        return entry;
    }

    private static @Nullable String readStringField(@NotNull String jsonLine, @NotNull String key) {
        Pattern pattern = Pattern.compile(String.format(STRING_FIELD_PATTERN.pattern(), Pattern.quote(key)));
        Matcher m = pattern.matcher(jsonLine);
        if (!m.find()) {
            return null;
        }
        String raw = m.group(1);
        if ("null".equals(raw)) {
            return null;
        }
        String encoded = m.group(2);
        return encoded == null ? null : unescapeJson(encoded);
    }

    private static @NotNull List<String> readStringArray(@NotNull String jsonLine, @NotNull Pattern arrayPattern) {
        Matcher m = arrayPattern.matcher(jsonLine);
        if (!m.find()) {
            return List.of();
        }
        String body = m.group(1);
        if (body == null || body.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        Matcher item = QUOTED_STRING_PATTERN.matcher(body);
        while (item.find()) {
            String encoded = item.group(1);
            if (encoded == null) {
                continue;
            }
            String decoded = unescapeJson(encoded);
            if (!decoded.isBlank()) {
                values.add(decoded);
            }
        }
        return values;
    }

    private static @NotNull List<PdfViewerSettings.WordSynonymGroupData> readSynonymGroups(@NotNull String jsonLine) {
        Matcher listMatcher = SYNONYM_GROUPS_PATTERN.matcher(jsonLine);
        if (!listMatcher.find()) {
            return List.of();
        }
        String body = listMatcher.group(1);
        if (body == null || body.isBlank()) {
            return List.of();
        }
        List<PdfViewerSettings.WordSynonymGroupData> groups = new ArrayList<>();
        Matcher groupMatcher = SYNONYM_GROUP_PATTERN.matcher(body);
        while (groupMatcher.find()) {
            String rawPos = groupMatcher.group(1);
            String pos = "null".equals(rawPos) ? null : unescapeJson(groupMatcher.group(2) == null ? "" : groupMatcher.group(2));
            List<String> words = readWordsFromGroup(groupMatcher.group(3));
            if (pos == null || pos.isBlank() || words.isEmpty()) {
                continue;
            }
            PdfViewerSettings.WordSynonymGroupData g = new PdfViewerSettings.WordSynonymGroupData();
            g.pos = pos;
            g.words = words;
            groups.add(g);
        }
        return groups;
    }

    private static @NotNull List<String> readWordsFromGroup(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        Matcher item = QUOTED_STRING_PATTERN.matcher(body);
        while (item.find()) {
            String encoded = item.group(1);
            if (encoded == null) {
                continue;
            }
            String decoded = unescapeJson(encoded);
            if (!decoded.isBlank()) {
                values.add(decoded);
            }
        }
        return values;
    }

    private static @Nullable String toJsonLine(@NotNull PdfViewerSettings.WordEntryData entry) {
        String wordKey = normalizeWordKey(entry.word);
        if (wordKey == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendJsonField(sb, "word", wordKey);
        appendJsonField(sb, "meaning", normalizeNullableText(entry.meaning));
        appendJsonField(sb, "phonetic", normalizeNullableText(entry.phonetic));
        appendJsonField(sb, "difficulty", normalizeNullableText(entry.difficulty));
        appendJsonField(sb, "theme", normalizeNullableText(entry.theme));
        appendJsonField(sb, "source", normalizeNullableText(entry.source));
        appendJsonField(sb, "sourceRef", normalizeNullableText(entry.sourceRef));
        appendJsonField(sb, "status", normalizeNullableText(entry.status));
        sb.append(",\"sentenceEnList\":").append(toJsonArray(entry.sentenceEnList));
        sb.append(",\"synonymsByPos\":").append(toJsonSynonymGroups(entry.synonymsByPos));
        sb.append("}");
        return sb.toString();
    }

    private static void appendJsonField(@NotNull StringBuilder sb, @NotNull String key, @Nullable String value) {
        if (sb.length() > 1) {
            sb.append(",");
        }
        sb.append("\"").append(escapeJson(key)).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append("\"").append(escapeJson(value)).append("\"");
        }
    }

    private static @NotNull String toJsonArray(@Nullable List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String value : values) {
            String normalized = normalizeNullableText(value);
            if (normalized == null) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(escapeJson(normalized)).append("\"");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private static @NotNull String toJsonSynonymGroups(@Nullable List<PdfViewerSettings.WordSynonymGroupData> groups) {
        if (groups == null || groups.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (PdfViewerSettings.WordSynonymGroupData group : groups) {
            if (group == null) {
                continue;
            }
            String pos = normalizeNullableText(group.pos);
            if (pos == null) {
                continue;
            }
            List<String> words = group.words == null ? List.of() : group.words;
            if (!first) {
                sb.append(",");
            }
            sb.append("{\"pos\":\"").append(escapeJson(pos)).append("\",\"words\":").append(toJsonArray(words)).append("}");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private static @NotNull PdfViewerSettings.WordEntryData normalizeEntry(@NotNull PdfViewerSettings.WordEntryData entry) {
        PdfViewerSettings.WordEntryData copy = new PdfViewerSettings.WordEntryData();
        copy.word = normalizeWordKey(entry.word);
        copy.meaning = normalizeNullableText(entry.meaning);
        copy.phonetic = normalizeNullableText(entry.phonetic);
        copy.difficulty = normalizeNullableText(entry.difficulty);
        copy.theme = normalizeNullableText(entry.theme);
        copy.source = normalizeNullableText(entry.source);
        copy.sourceRef = normalizeNullableText(entry.sourceRef);
        copy.status = normalizeNullableText(entry.status);
        copy.sentenceEnList = normalizeStringList(entry.sentenceEnList == null ? List.of() : entry.sentenceEnList);
        copy.synonymsByPos = normalizeSynonymGroups(entry.synonymsByPos);
        return copy;
    }

    private static @Nullable String normalizeWordKey(@Nullable String word) {
        if (word == null) {
            return null;
        }
        String normalized = word.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static @Nullable String normalizeNullableText(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static @NotNull List<String> normalizeStringList(@NotNull List<String> values) {
        LinkedHashMap<String, Boolean> unique = new LinkedHashMap<>();
        for (String value : values) {
            String normalized = normalizeNullableText(value);
            if (normalized != null) {
                unique.put(normalized, Boolean.TRUE);
            }
        }
        return new ArrayList<>(unique.keySet());
    }

    private static @NotNull List<PdfViewerSettings.WordSynonymGroupData> normalizeSynonymGroups(@Nullable List<PdfViewerSettings.WordSynonymGroupData> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        List<PdfViewerSettings.WordSynonymGroupData> normalized = new ArrayList<>();
        for (PdfViewerSettings.WordSynonymGroupData group : groups) {
            if (group == null) {
                continue;
            }
            String pos = normalizeNullableText(group.pos);
            List<String> words = normalizeStringList(group.words == null ? List.of() : group.words);
            if (pos == null || words.isEmpty()) {
                continue;
            }
            PdfViewerSettings.WordSynonymGroupData copy = new PdfViewerSettings.WordSynonymGroupData();
            copy.pos = pos;
            copy.words = words;
            normalized.add(copy);
        }
        return normalized;
    }

    private static @NotNull String escapeJson(@NotNull String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static @NotNull String unescapeJson(@NotNull String text) {
        StringBuilder sb = new StringBuilder(text.length());
        boolean escaping = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!escaping) {
                if (c == '\\') {
                    escaping = true;
                } else {
                    sb.append(c);
                }
                continue;
            }
            escaping = false;
            if (c == 'n') {
                sb.append('\n');
            } else if (c == 'r') {
                sb.append('\r');
            } else if (c == 't') {
                sb.append('\t');
            } else {
                sb.append(c);
            }
        }
        if (escaping) {
            sb.append('\\');
        }
        return sb.toString();
    }
}

