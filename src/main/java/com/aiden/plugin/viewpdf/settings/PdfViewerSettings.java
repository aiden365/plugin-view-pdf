package com.aiden.plugin.viewpdf.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@State(
        name = "PdfViewerSettings",
        storages = @Storage("pdf-viewer.xml")
)
public final class PdfViewerSettings implements PersistentStateComponent<PdfViewerSettings.StateData> {
    private static final int DEFAULT_BG_R = 43;
    private static final int DEFAULT_BG_G = 45;
    private static final int DEFAULT_BG_B = 48;
    private static final int DEFAULT_TEXT_R = 220;
    private static final int DEFAULT_TEXT_G = 220;
    private static final int DEFAULT_TEXT_B = 220;
    private static final int DEFAULT_TREE_BG_R = 30;
    private static final int DEFAULT_TREE_BG_G = 31;
    private static final int DEFAULT_TREE_BG_B = 34;
    private static final int DEFAULT_TREE_TEXT_R = 122;
    private static final int DEFAULT_TREE_TEXT_G = 126;
    private static final int DEFAULT_TREE_TEXT_B = 133;
    private static final int DEFAULT_POPUP_BG_R = 30;
    private static final int DEFAULT_POPUP_BG_G = 31;
    private static final int DEFAULT_POPUP_BG_B = 34;
    private static final int DEFAULT_POPUP_TEXT_R = 122;
    private static final int DEFAULT_POPUP_TEXT_G = 126;
    private static final int DEFAULT_POPUP_TEXT_B = 133;
    private static final int DEFAULT_TREE_FONT_SIZE = 12;
    private static final int DEFAULT_HOVER_SECONDS = -1;
    private static final int DEFAULT_ZOOM_PERCENT = 100;
    private static final int DEFAULT_PANE_LEFT_PERCENT = 25;
    private static final int DEFAULT_PANE_MIDDLE_PERCENT = 45;
    private static final int DEFAULT_PANE_RIGHT_PERCENT = 30;
    private static final int DEFAULT_RENDER_BATCH_PAGE_COUNT = 50;
    private static final int DEFAULT_EDITOR_POPUP_WIDTH = 760;
    private static final int DEFAULT_EDITOR_POPUP_HEIGHT = 520;
    private static final int DEFAULT_EDITOR_WORD_POPUP_WIDTH = 240;
    private static final int DEFAULT_EDITOR_WORD_POPUP_HEIGHT = 120;
    private static final int DEFAULT_EDITOR_WORD_POPUP_X = 36;
    private static final int DEFAULT_EDITOR_WORD_POPUP_Y = 36;
    private static final int DEFAULT_EDITOR_WORD_POPUP_BG_R = 38;
    private static final int DEFAULT_EDITOR_WORD_POPUP_BG_G = 40;
    private static final int DEFAULT_EDITOR_WORD_POPUP_BG_B = 46;
    private static final int DEFAULT_EDITOR_WORD_POPUP_FONT_R = 255;
    private static final int DEFAULT_EDITOR_WORD_POPUP_FONT_G = 255;
    private static final int DEFAULT_EDITOR_WORD_POPUP_FONT_B = 255;
    private static final int DEFAULT_WORD_POPUP_WIDTH = 360;
    private static final int DEFAULT_WORD_POPUP_HEIGHT = 220;
    private static final int DEFAULT_WORD_POPUP_X = 36;
    private static final int DEFAULT_WORD_POPUP_Y = 36;
    private static final int DEFAULT_WORD_POPUP_FONT_SIZE = 18;
    private static final int DEFAULT_WORD_POPUP_FONT_R = 235;
    private static final int DEFAULT_WORD_POPUP_FONT_G = 235;
    private static final int DEFAULT_WORD_POPUP_FONT_B = 235;
    private static final int DEFAULT_POPUP_OPACITY_PERCENT = 100;
    private static final int MIN_POPUP_OPACITY_PERCENT = 10;
    private static final int MAX_POPUP_OPACITY_PERCENT = 100;
    private static final boolean DEFAULT_WORD_MANAGER_PANE_VISIBLE = false;
    private static final int DEFAULT_WORD_MANAGER_PANE_WIDTH_PERCENT = 25;
    private static final boolean DEFAULT_BOOK_MANAGER_PANE_VISIBLE = false;
    private static final boolean DEFAULT_WORD_POPUP_SHOW_MEANING = false;
    private static final boolean DEFAULT_WORD_POPUP_SHOW_SENTENCE = false;
    private static final boolean DEFAULT_WORD_POPUP_SHOW_SYNONYMS = false;
    private static final int DEFAULT_WORD_POPUP_SENTENCE_LIMIT = 1;
    private static final String DEFAULT_WORD_BUILTIN_BOOK = "CET4luan_2";
    private static final String BUILTIN_KEY_PREFIX = "builtin:";
    private static final String CUSTOM_KEY_PREFIX = "custom:";
    private static final String BOOK_SOURCE_IMPORT = "import";
    private static final String BOOK_SOURCE_MANUAL = "manual";

    public static final class WordEntryData {
        public String word;
        public String meaning;
        public String phonetic;
        public String difficulty;
        public String theme;
        public String source;
        public String sourceRef;
        public String status;
        public List<String> sentenceEnList;
        public List<WordSynonymGroupData> synonymsByPos;
    }

    public static final class WordSynonymGroupData {
        public String pos;
        public List<String> words;
    }

    public static final class WordLearningStateData {
        public Boolean mastered;
        public Long lastReviewedAtEpochMillis;
        public Integer reviewCount;
    }

    public static final class CustomVocabularyBookData {
        public String name;
        public String jsonlPath;
        public Long createdAtEpochMillis;
    }

    public static final class BookData {
        public String id;
        public String name;
        public String sourceType;
        public String filePath;
        public String inlineContent;
        public Long createdAtEpochMillis;
    }

    public static final class StateData {
        public String pdfPath;
        public boolean nightModeEnabled;
        public Integer pdfBackgroundR;
        public Integer pdfBackgroundG;
        public Integer pdfBackgroundB;
        public Integer pdfTextR;
        public Integer pdfTextG;
        public Integer pdfTextB;
        public Integer treeBackgroundR;
        public Integer treeBackgroundG;
        public Integer treeBackgroundB;
        public Integer treeTextR;
        public Integer treeTextG;
        public Integer treeTextB;
        public Integer treeFontSize;
        public Integer autoShowPdfHoverSeconds;
        public Integer pdfZoomPercent;
        public Map<String, Integer> pdfReadPositions;
        public Integer paneLeftPercent;
        public Integer paneMiddlePercent;
        public Integer paneRightPercent;
        public Integer renderBatchPageCount;
        public Boolean thirdPaneVisible;
        public Integer editorPopupWidth;
        public Integer editorPopupHeight;
        public Boolean editorPopupBorderVisible;
        public Integer editorPopupPdfBackgroundR;
        public Integer editorPopupPdfBackgroundG;
        public Integer editorPopupPdfBackgroundB;
        public Integer editorPopupPdfTextR;
        public Integer editorPopupPdfTextG;
        public Integer editorPopupPdfTextB;
        public Integer editorPopupOpacityPercent;
        public Integer editorWordPopupBackgroundOpacityPercent;
        public Integer editorWordPopupTextOpacityPercent;
        public Integer editorWordPopupWidth;
        public Integer editorWordPopupHeight;
        public Integer editorWordPopupX;
        public Integer editorWordPopupY;
        public Integer editorWordPopupBgR;
        public Integer editorWordPopupBgG;
        public Integer editorWordPopupBgB;
        public Integer editorWordPopupFontR;
        public Integer editorWordPopupFontG;
        public Integer editorWordPopupFontB;
        public Integer wordPopupWidth;
        public Integer wordPopupHeight;
        public Integer wordPopupX;
        public Integer wordPopupY;
        public Integer wordPopupFontSize;
        public Integer wordPopupFontR;
        public Integer wordPopupFontG;
        public Integer wordPopupFontB;
        public Integer wordPopupOpacityPercent;
        public Boolean wordPopupShowMeaning;
        public Boolean wordPopupShowSentence;
        public Boolean wordPopupShowSynonyms;
        public Integer wordPopupSentenceLimit;
        public Boolean wordSourceBuiltinEnabled;
        public String wordBuiltinVocabularyBook;
        public List<CustomVocabularyBookData> customVocabularyBooks;
        public String selectedVocabularyBookKey;
        public String wordSourceCustomPath;
        public List<String> wordFilterDifficulties;
        public List<String> wordFilterThemes;
        public List<String> wordFilterSources;
        public List<WordEntryData> wordEntries;
        public Map<String, WordLearningStateData> wordLearningStates;
        public Boolean wordManagerPaneVisible;
        public Integer wordManagerPaneWidthPercent;
        public Boolean bookManagerPaneVisible;
        public Map<String, List<String>> hiddenWordsByVocabularyBookKey;
        public List<BookData> books;
        public Map<String, Integer> bookReadLineById;
        public String currentReadingBookId;
        public String bookReadingOutputMode;
    }

    private StateData state = new StateData();

    public static PdfViewerSettings getInstance() {
        return ApplicationManager.getApplication().getService(PdfViewerSettings.class);
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

    public @Nullable String getPdfPath() {
        String path = state.pdfPath;
        if (path == null) {
            return null;
        }
        String trimmed = path.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public void setPdfPath(@Nullable String pdfPath) {
        String normalized = pdfPath == null ? null : pdfPath.trim();
        if (normalized != null && normalized.isEmpty()) {
            normalized = null;
        }
        if (Objects.equals(getPdfPath(), normalized)) {
            return;
        }
        state.pdfPath = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .pdfPathChanged(normalized);
    }

    public boolean isNightModeEnabled() {
        return state.nightModeEnabled;
    }

    public void setNightModeEnabled(boolean enabled) {
        if (state.nightModeEnabled == enabled) {
            return;
        }
        state.nightModeEnabled = enabled;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .nightModeChanged(enabled);
    }

    public int getPdfBackgroundR() {
        return state.pdfBackgroundR == null ? DEFAULT_BG_R : clampColorChannel(state.pdfBackgroundR);
    }

    public int getPdfBackgroundG() {
        return state.pdfBackgroundG == null ? DEFAULT_BG_G : clampColorChannel(state.pdfBackgroundG);
    }

    public int getPdfBackgroundB() {
        return state.pdfBackgroundB == null ? DEFAULT_BG_B : clampColorChannel(state.pdfBackgroundB);
    }

    public @NotNull Color getPdfBackgroundColor() {
        return new Color(getPdfBackgroundR(), getPdfBackgroundG(), getPdfBackgroundB());
    }

    public void setPdfBackgroundRgb(int r, int g, int b) {
        int nr = clampColorChannel(r);
        int ng = clampColorChannel(g);
        int nb = clampColorChannel(b);

        if (getPdfBackgroundR() == nr && getPdfBackgroundG() == ng && getPdfBackgroundB() == nb) {
            return;
        }

        state.pdfBackgroundR = nr;
        state.pdfBackgroundG = ng;
        state.pdfBackgroundB = nb;

        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .pdfBackgroundChanged(new Color(nr, ng, nb));
    }

    public int getPdfTextR() {
        return state.pdfTextR == null ? DEFAULT_TEXT_R : clampColorChannel(state.pdfTextR);
    }

    public int getPdfTextG() {
        return state.pdfTextG == null ? DEFAULT_TEXT_G : clampColorChannel(state.pdfTextG);
    }

    public int getPdfTextB() {
        return state.pdfTextB == null ? DEFAULT_TEXT_B : clampColorChannel(state.pdfTextB);
    }

    public @NotNull Color getPdfTextColor() {
        return new Color(getPdfTextR(), getPdfTextG(), getPdfTextB());
    }

    public void setPdfTextRgb(int r, int g, int b) {
        int nr = clampColorChannel(r);
        int ng = clampColorChannel(g);
        int nb = clampColorChannel(b);
        if (getPdfTextR() == nr && getPdfTextG() == ng && getPdfTextB() == nb) {
            return;
        }

        state.pdfTextR = nr;
        state.pdfTextG = ng;
        state.pdfTextB = nb;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .pdfTextColorChanged(new Color(nr, ng, nb));
    }

    public int getTreeBackgroundR() {
        return state.treeBackgroundR == null ? DEFAULT_TREE_BG_R : clampColorChannel(state.treeBackgroundR);
    }

    public int getTreeBackgroundG() {
        return state.treeBackgroundG == null ? DEFAULT_TREE_BG_G : clampColorChannel(state.treeBackgroundG);
    }

    public int getTreeBackgroundB() {
        return state.treeBackgroundB == null ? DEFAULT_TREE_BG_B : clampColorChannel(state.treeBackgroundB);
    }

    public @NotNull Color getTreeBackgroundColor() {
        return new Color(getTreeBackgroundR(), getTreeBackgroundG(), getTreeBackgroundB());
    }

    public void setTreeBackgroundRgb(int r, int g, int b) {
        int nr = clampColorChannel(r);
        int ng = clampColorChannel(g);
        int nb = clampColorChannel(b);
        if (getTreeBackgroundR() == nr && getTreeBackgroundG() == ng && getTreeBackgroundB() == nb) {
            return;
        }
        state.treeBackgroundR = nr;
        state.treeBackgroundG = ng;
        state.treeBackgroundB = nb;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .treeBackgroundChanged(new Color(nr, ng, nb));
    }

    public int getTreeTextR() {
        return state.treeTextR == null ? DEFAULT_TREE_TEXT_R : clampColorChannel(state.treeTextR);
    }

    public int getTreeTextG() {
        return state.treeTextG == null ? DEFAULT_TREE_TEXT_G : clampColorChannel(state.treeTextG);
    }

    public int getTreeTextB() {
        return state.treeTextB == null ? DEFAULT_TREE_TEXT_B : clampColorChannel(state.treeTextB);
    }

    public @NotNull Color getTreeTextColor() {
        return new Color(getTreeTextR(), getTreeTextG(), getTreeTextB());
    }

    public void setTreeTextRgb(int r, int g, int b) {
        int nr = clampColorChannel(r);
        int ng = clampColorChannel(g);
        int nb = clampColorChannel(b);
        if (getTreeTextR() == nr && getTreeTextG() == ng && getTreeTextB() == nb) {
            return;
        }
        state.treeTextR = nr;
        state.treeTextG = ng;
        state.treeTextB = nb;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .treeTextColorChanged(new Color(nr, ng, nb));
    }

    public int getTreeFontSize() {
        Integer value = state.treeFontSize;
        if (value == null) {
            return DEFAULT_TREE_FONT_SIZE;
        }
        return Math.max(8, Math.min(32, value));
    }

    public void setTreeFontSize(int size) {
        int normalized = Math.max(8, Math.min(32, size));
        if (getTreeFontSize() == normalized) {
            return;
        }
        state.treeFontSize = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .treeFontSizeChanged(normalized);
    }

    public int getAutoShowPdfHoverSeconds() {
        Integer value = state.autoShowPdfHoverSeconds;
        if (value == null) {
            return DEFAULT_HOVER_SECONDS;
        }
        return Math.max(-1, value);
    }

    public void setAutoShowPdfHoverSeconds(int seconds) {
        int normalized = Math.max(-1, seconds);
        if (getAutoShowPdfHoverSeconds() == normalized) {
            return;
        }
        state.autoShowPdfHoverSeconds = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .hoverSecondsChanged(normalized);
    }

    public int getPdfZoomPercent() {
        Integer value = state.pdfZoomPercent;
        if (value == null) {
            return DEFAULT_ZOOM_PERCENT;
        }
        return Math.max(10, Math.min(500, value));
    }

    public void setPdfZoomPercent(int percent) {
        int normalized = Math.max(10, Math.min(500, percent));
        if (getPdfZoomPercent() == normalized) {
            return;
        }
        state.pdfZoomPercent = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .zoomPercentChanged(normalized);
    }

    public int getPaneLeftPercent() {
        return state.paneLeftPercent == null ? DEFAULT_PANE_LEFT_PERCENT : clampPanePercent(state.paneLeftPercent);
    }

    public int getPaneMiddlePercent() {
        return state.paneMiddlePercent == null ? DEFAULT_PANE_MIDDLE_PERCENT : clampPanePercent(state.paneMiddlePercent);
    }

    public int getPaneRightPercent() {
        return state.paneRightPercent == null ? DEFAULT_PANE_RIGHT_PERCENT : clampPanePercent(state.paneRightPercent);
    }

    public void setPaneRatios(int leftPercent, int middlePercent, int rightPercent) {
        int[] normalized = normalizePaneRatios(leftPercent, middlePercent, rightPercent);
        if (getPaneLeftPercent() == normalized[0]
                && getPaneMiddlePercent() == normalized[1]
                && getPaneRightPercent() == normalized[2]) {
            return;
        }
        state.paneLeftPercent = normalized[0];
        state.paneMiddlePercent = normalized[1];
        state.paneRightPercent = normalized[2];
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .paneRatiosChanged(normalized[0], normalized[1], normalized[2]);
    }

    public boolean isThirdPaneVisible() {
        return state.thirdPaneVisible == null || state.thirdPaneVisible;
    }

    public void setThirdPaneVisible(boolean visible) {
        if (isThirdPaneVisible() == visible) {
            return;
        }
        state.thirdPaneVisible = visible;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .thirdPaneVisibilityChanged(visible);
    }

    public int getRenderBatchPageCount() {
        Integer value = state.renderBatchPageCount;
        if (value == null) {
            return DEFAULT_RENDER_BATCH_PAGE_COUNT;
        }
        return Math.max(1, value);
    }

    public void setRenderBatchPageCount(int pageCount) {
        int normalized = Math.max(1, pageCount);
        if (getRenderBatchPageCount() == normalized) {
            return;
        }
        state.renderBatchPageCount = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .renderBatchPageCountChanged(normalized);
    }

    public int getEditorPopupWidth() {
        Integer value = state.editorPopupWidth;
        if (value == null) {
            return DEFAULT_EDITOR_POPUP_WIDTH;
        }
        return clampPopupSize(value);
    }

    public int getEditorPopupHeight() {
        Integer value = state.editorPopupHeight;
        if (value == null) {
            return DEFAULT_EDITOR_POPUP_HEIGHT;
        }
        return clampPopupSize(value);
    }

    public void setEditorPopupSize(int width, int height) {
        int normalizedWidth = clampPopupSize(width);
        int normalizedHeight = clampPopupSize(height);
        if (getEditorPopupWidth() == normalizedWidth && getEditorPopupHeight() == normalizedHeight) {
            return;
        }
        state.editorPopupWidth = normalizedWidth;
        state.editorPopupHeight = normalizedHeight;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .editorPopupSizeChanged(normalizedWidth, normalizedHeight);
    }

    public boolean isEditorPopupBorderVisible() {
        return state.editorPopupBorderVisible == null || state.editorPopupBorderVisible;
    }

    public void setEditorPopupBorderVisible(boolean visible) {
        if (isEditorPopupBorderVisible() == visible) {
            return;
        }
        state.editorPopupBorderVisible = visible;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .editorPopupBorderVisibilityChanged(visible);
    }

    public int getEditorPopupPdfBackgroundR() {
        return state.editorPopupPdfBackgroundR == null ? DEFAULT_POPUP_BG_R : clampColorChannel(state.editorPopupPdfBackgroundR);
    }

    public int getEditorPopupPdfBackgroundG() {
        return state.editorPopupPdfBackgroundG == null ? DEFAULT_POPUP_BG_G : clampColorChannel(state.editorPopupPdfBackgroundG);
    }

    public int getEditorPopupPdfBackgroundB() {
        return state.editorPopupPdfBackgroundB == null ? DEFAULT_POPUP_BG_B : clampColorChannel(state.editorPopupPdfBackgroundB);
    }

    public @NotNull Color getEditorPopupPdfBackgroundColor() {
        return new Color(getEditorPopupPdfBackgroundR(), getEditorPopupPdfBackgroundG(), getEditorPopupPdfBackgroundB());
    }

    public void setEditorPopupPdfBackgroundRgb(int r, int g, int b) {
        int nr = clampColorChannel(r);
        int ng = clampColorChannel(g);
        int nb = clampColorChannel(b);
        if (getEditorPopupPdfBackgroundR() == nr
                && getEditorPopupPdfBackgroundG() == ng
                && getEditorPopupPdfBackgroundB() == nb) {
            return;
        }
        state.editorPopupPdfBackgroundR = nr;
        state.editorPopupPdfBackgroundG = ng;
        state.editorPopupPdfBackgroundB = nb;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .editorPopupPdfBackgroundChanged(new Color(nr, ng, nb));
    }

    public int getEditorPopupPdfTextR() {
        return state.editorPopupPdfTextR == null ? DEFAULT_POPUP_TEXT_R : clampColorChannel(state.editorPopupPdfTextR);
    }

    public int getEditorPopupPdfTextG() {
        return state.editorPopupPdfTextG == null ? DEFAULT_POPUP_TEXT_G : clampColorChannel(state.editorPopupPdfTextG);
    }

    public int getEditorPopupPdfTextB() {
        return state.editorPopupPdfTextB == null ? DEFAULT_POPUP_TEXT_B : clampColorChannel(state.editorPopupPdfTextB);
    }

    public @NotNull Color getEditorPopupPdfTextColor() {
        return new Color(getEditorPopupPdfTextR(), getEditorPopupPdfTextG(), getEditorPopupPdfTextB());
    }

    public void setEditorPopupPdfTextRgb(int r, int g, int b) {
        int nr = clampColorChannel(r);
        int ng = clampColorChannel(g);
        int nb = clampColorChannel(b);
        if (getEditorPopupPdfTextR() == nr
                && getEditorPopupPdfTextG() == ng
                && getEditorPopupPdfTextB() == nb) {
            return;
        }
        state.editorPopupPdfTextR = nr;
        state.editorPopupPdfTextG = ng;
        state.editorPopupPdfTextB = nb;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .editorPopupPdfTextColorChanged(new Color(nr, ng, nb));
    }

    public int getEditorPopupOpacityPercent() {
        Integer value = state.editorPopupOpacityPercent;
        if (value == null) {
            return DEFAULT_POPUP_OPACITY_PERCENT;
        }
        return clampPopupOpacityPercent(value);
    }

    public void setEditorPopupOpacityPercent(int percent) {
        int normalized = clampPopupOpacityPercent(percent);
        if (getEditorPopupOpacityPercent() == normalized) {
            return;
        }
        state.editorPopupOpacityPercent = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .editorPopupOpacityChanged(normalized);
    }

    public int getEditorWordPopupBackgroundOpacityPercent() {
        Integer value = state.editorWordPopupBackgroundOpacityPercent;
        if (value == null) {
            return DEFAULT_POPUP_OPACITY_PERCENT;
        }
        return clampPopupOpacityPercent(value);
    }

    public void setEditorWordPopupBackgroundOpacityPercent(int percent) {
        int normalized = clampPopupOpacityPercent(percent);
        if (getEditorWordPopupBackgroundOpacityPercent() == normalized) {
            return;
        }
        state.editorWordPopupBackgroundOpacityPercent = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .editorWordPopupBackgroundOpacityChanged(normalized);
    }

    public int getEditorWordPopupTextOpacityPercent() {
        Integer value = state.editorWordPopupTextOpacityPercent;
        if (value == null) {
            return DEFAULT_POPUP_OPACITY_PERCENT;
        }
        return clampPopupOpacityPercent(value);
    }

    public void setEditorWordPopupTextOpacityPercent(int percent) {
        int normalized = clampPopupOpacityPercent(percent);
        if (getEditorWordPopupTextOpacityPercent() == normalized) {
            return;
        }
        state.editorWordPopupTextOpacityPercent = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .editorWordPopupTextOpacityChanged(normalized);
    }

    public boolean isEditorWordPopupLocationConfigured() {
        return state.editorWordPopupX != null && state.editorWordPopupY != null;
    }

    public int getEditorWordPopupWidth() {
        Integer value = state.editorWordPopupWidth;
        if (value == null) {
            return DEFAULT_EDITOR_WORD_POPUP_WIDTH;
        }
        return clampWordPopupSize(value);
    }

    public int getEditorWordPopupHeight() {
        Integer value = state.editorWordPopupHeight;
        if (value == null) {
            return DEFAULT_EDITOR_WORD_POPUP_HEIGHT;
        }
        return clampWordPopupSize(value);
    }

    public int getEditorWordPopupX() {
        Integer value = state.editorWordPopupX;
        if (value == null) {
            return DEFAULT_EDITOR_WORD_POPUP_X;
        }
        return clampWordPopupCoordinate(value);
    }

    public int getEditorWordPopupY() {
        Integer value = state.editorWordPopupY;
        if (value == null) {
            return DEFAULT_EDITOR_WORD_POPUP_Y;
        }
        return clampWordPopupCoordinate(value);
    }

    public int getEditorWordPopupBackgroundR() {
        return state.editorWordPopupBgR == null ? DEFAULT_EDITOR_WORD_POPUP_BG_R : clampColorChannel(state.editorWordPopupBgR);
    }

    public int getEditorWordPopupBackgroundG() {
        return state.editorWordPopupBgG == null ? DEFAULT_EDITOR_WORD_POPUP_BG_G : clampColorChannel(state.editorWordPopupBgG);
    }

    public int getEditorWordPopupBackgroundB() {
        return state.editorWordPopupBgB == null ? DEFAULT_EDITOR_WORD_POPUP_BG_B : clampColorChannel(state.editorWordPopupBgB);
    }

    public @NotNull Color getEditorWordPopupBackgroundColor() {
        return new Color(getEditorWordPopupBackgroundR(), getEditorWordPopupBackgroundG(), getEditorWordPopupBackgroundB());
    }

    public int getEditorWordPopupFontR() {
        return state.editorWordPopupFontR == null ? DEFAULT_EDITOR_WORD_POPUP_FONT_R : clampColorChannel(state.editorWordPopupFontR);
    }

    public int getEditorWordPopupFontG() {
        return state.editorWordPopupFontG == null ? DEFAULT_EDITOR_WORD_POPUP_FONT_G : clampColorChannel(state.editorWordPopupFontG);
    }

    public int getEditorWordPopupFontB() {
        return state.editorWordPopupFontB == null ? DEFAULT_EDITOR_WORD_POPUP_FONT_B : clampColorChannel(state.editorWordPopupFontB);
    }

    public @NotNull Color getEditorWordPopupFontColor() {
        return new Color(getEditorWordPopupFontR(), getEditorWordPopupFontG(), getEditorWordPopupFontB());
    }

    public void setEditorWordPopupStyle(
            int width,
            int height,
            int x,
            int y,
            int bgR,
            int bgG,
            int bgB,
            int fontR,
            int fontG,
            int fontB
    ) {
        int normalizedWidth = clampWordPopupSize(width);
        int normalizedHeight = clampWordPopupSize(height);
        int normalizedX = clampWordPopupCoordinate(x);
        int normalizedY = clampWordPopupCoordinate(y);
        int normalizedBgR = clampColorChannel(bgR);
        int normalizedBgG = clampColorChannel(bgG);
        int normalizedBgB = clampColorChannel(bgB);
        int normalizedFontR = clampColorChannel(fontR);
        int normalizedFontG = clampColorChannel(fontG);
        int normalizedFontB = clampColorChannel(fontB);
        if (getEditorWordPopupWidth() == normalizedWidth
                && getEditorWordPopupHeight() == normalizedHeight
                && getEditorWordPopupX() == normalizedX
                && getEditorWordPopupY() == normalizedY
                && getEditorWordPopupBackgroundR() == normalizedBgR
                && getEditorWordPopupBackgroundG() == normalizedBgG
                && getEditorWordPopupBackgroundB() == normalizedBgB
                && getEditorWordPopupFontR() == normalizedFontR
                && getEditorWordPopupFontG() == normalizedFontG
                && getEditorWordPopupFontB() == normalizedFontB) {
            return;
        }
        state.editorWordPopupWidth = normalizedWidth;
        state.editorWordPopupHeight = normalizedHeight;
        state.editorWordPopupX = normalizedX;
        state.editorWordPopupY = normalizedY;
        state.editorWordPopupBgR = normalizedBgR;
        state.editorWordPopupBgG = normalizedBgG;
        state.editorWordPopupBgB = normalizedBgB;
        state.editorWordPopupFontR = normalizedFontR;
        state.editorWordPopupFontG = normalizedFontG;
        state.editorWordPopupFontB = normalizedFontB;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .editorWordPopupStyleChanged(
                        normalizedWidth,
                        normalizedHeight,
                        normalizedX,
                        normalizedY,
                        new Color(normalizedBgR, normalizedBgG, normalizedBgB),
                        new Color(normalizedFontR, normalizedFontG, normalizedFontB)
                );
    }

    public int getWordPopupWidth() {
        Integer value = state.wordPopupWidth;
        if (value == null) {
            return DEFAULT_WORD_POPUP_WIDTH;
        }
        return clampWordPopupSize(value);
    }

    public int getWordPopupHeight() {
        Integer value = state.wordPopupHeight;
        if (value == null) {
            return DEFAULT_WORD_POPUP_HEIGHT;
        }
        return clampWordPopupSize(value);
    }

    public int getWordPopupX() {
        Integer value = state.wordPopupX;
        if (value == null) {
            return DEFAULT_WORD_POPUP_X;
        }
        return clampWordPopupCoordinate(value);
    }

    public int getWordPopupY() {
        Integer value = state.wordPopupY;
        if (value == null) {
            return DEFAULT_WORD_POPUP_Y;
        }
        return clampWordPopupCoordinate(value);
    }

    public int getWordPopupFontSize() {
        Integer value = state.wordPopupFontSize;
        if (value == null) {
            return DEFAULT_WORD_POPUP_FONT_SIZE;
        }
        return clampWordPopupFontSize(value);
    }

    public int getWordPopupFontR() {
        return state.wordPopupFontR == null ? DEFAULT_WORD_POPUP_FONT_R : clampColorChannel(state.wordPopupFontR);
    }

    public int getWordPopupFontG() {
        return state.wordPopupFontG == null ? DEFAULT_WORD_POPUP_FONT_G : clampColorChannel(state.wordPopupFontG);
    }

    public int getWordPopupFontB() {
        return state.wordPopupFontB == null ? DEFAULT_WORD_POPUP_FONT_B : clampColorChannel(state.wordPopupFontB);
    }

    public @NotNull Color getWordPopupFontColor() {
        return new Color(getWordPopupFontR(), getWordPopupFontG(), getWordPopupFontB());
    }

    public boolean isWordPopupShowMeaning() {
        return state.wordPopupShowMeaning == null ? DEFAULT_WORD_POPUP_SHOW_MEANING : state.wordPopupShowMeaning;
    }

    public boolean isWordPopupShowSentence() {
        return state.wordPopupShowSentence == null ? DEFAULT_WORD_POPUP_SHOW_SENTENCE : state.wordPopupShowSentence;
    }

    public boolean isWordPopupShowSynonyms() {
        return state.wordPopupShowSynonyms == null ? DEFAULT_WORD_POPUP_SHOW_SYNONYMS : state.wordPopupShowSynonyms;
    }

    public int getWordPopupSentenceLimit() {
        Integer value = state.wordPopupSentenceLimit;
        if (value == null) {
            return DEFAULT_WORD_POPUP_SENTENCE_LIMIT;
        }
        return clampWordPopupSentenceLimit(value);
    }

    public void setWordPopupStyle(int width, int height, int x, int y, int fontSize, int fontR, int fontG, int fontB) {
        int normalizedWidth = clampWordPopupSize(width);
        int normalizedHeight = clampWordPopupSize(height);
        int normalizedX = clampWordPopupCoordinate(x);
        int normalizedY = clampWordPopupCoordinate(y);
        int normalizedFontSize = clampWordPopupFontSize(fontSize);
        int normalizedFontR = clampColorChannel(fontR);
        int normalizedFontG = clampColorChannel(fontG);
        int normalizedFontB = clampColorChannel(fontB);
        if (getWordPopupWidth() == normalizedWidth
                && getWordPopupHeight() == normalizedHeight
                && getWordPopupX() == normalizedX
                && getWordPopupY() == normalizedY
                && getWordPopupFontSize() == normalizedFontSize
                && getWordPopupFontR() == normalizedFontR
                && getWordPopupFontG() == normalizedFontG
                && getWordPopupFontB() == normalizedFontB) {
            return;
        }
        state.wordPopupWidth = normalizedWidth;
        state.wordPopupHeight = normalizedHeight;
        state.wordPopupX = normalizedX;
        state.wordPopupY = normalizedY;
        state.wordPopupFontSize = normalizedFontSize;
        state.wordPopupFontR = normalizedFontR;
        state.wordPopupFontG = normalizedFontG;
        state.wordPopupFontB = normalizedFontB;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .wordPopupStyleChanged(
                        normalizedWidth,
                        normalizedHeight,
                        normalizedX,
                        normalizedY,
                        normalizedFontSize,
                        new Color(normalizedFontR, normalizedFontG, normalizedFontB)
                );
    }

    public int getWordPopupOpacityPercent() {
        Integer value = state.wordPopupOpacityPercent;
        if (value == null) {
            return DEFAULT_POPUP_OPACITY_PERCENT;
        }
        return clampPopupOpacityPercent(value);
    }

    public void setWordPopupOpacityPercent(int percent) {
        int normalized = clampPopupOpacityPercent(percent);
        if (getWordPopupOpacityPercent() == normalized) {
            return;
        }
        state.wordPopupOpacityPercent = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .wordPopupOpacityChanged(normalized);
    }

    public void setWordPopupContentDisplay(boolean showMeaning, boolean showSentence, boolean showSynonyms, int sentenceLimit) {
        int normalizedSentenceLimit = clampWordPopupSentenceLimit(sentenceLimit);
        if (isWordPopupShowMeaning() == showMeaning
                && isWordPopupShowSentence() == showSentence
                && isWordPopupShowSynonyms() == showSynonyms
                && getWordPopupSentenceLimit() == normalizedSentenceLimit) {
            return;
        }
        state.wordPopupShowMeaning = showMeaning;
        state.wordPopupShowSentence = showSentence;
        state.wordPopupShowSynonyms = showSynonyms;
        state.wordPopupSentenceLimit = normalizedSentenceLimit;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .wordPopupContentDisplayChanged(showMeaning, showSentence, showSynonyms, normalizedSentenceLimit);
    }

    public boolean isWordSourceBuiltinEnabled() {
        return state.wordSourceBuiltinEnabled == null || state.wordSourceBuiltinEnabled;
    }

    public @NotNull String getWordBuiltinVocabularyBook() {
        String value = state.wordBuiltinVocabularyBook;
        if (value == null) {
            return DEFAULT_WORD_BUILTIN_BOOK;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? DEFAULT_WORD_BUILTIN_BOOK : normalized;
    }

    public @NotNull List<CustomVocabularyBookData> getCustomVocabularyBooks() {
        List<CustomVocabularyBookData> books = state.customVocabularyBooks;
        if (books == null || books.isEmpty()) {
            return List.of();
        }
        List<CustomVocabularyBookData> normalized = new ArrayList<>();
        for (CustomVocabularyBookData book : books) {
            if (book == null) {
                continue;
            }
            String name = normalizeNullableText(book.name);
            String jsonlPath = normalizeNullableText(book.jsonlPath);
            if (name == null || jsonlPath == null) {
                continue;
            }
            CustomVocabularyBookData copy = new CustomVocabularyBookData();
            copy.name = name;
            copy.jsonlPath = jsonlPath;
            copy.createdAtEpochMillis = book.createdAtEpochMillis;
            normalized.add(copy);
        }
        return normalized.isEmpty() ? List.of() : normalized;
    }

    public void setCustomVocabularyBooks(@NotNull List<CustomVocabularyBookData> books) {
        List<CustomVocabularyBookData> normalized = normalizeCustomBooks(books);
        if (getCustomVocabularyBooks().equals(normalized)) {
            return;
        }
        state.customVocabularyBooks = normalized.isEmpty() ? null : normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .vocabularyBookListChanged();
    }

    public void addCustomVocabularyBook(@NotNull String name, @NotNull String jsonlPath) {
        String normalizedName = normalizeNullableText(name);
        String normalizedPath = normalizeNullableText(jsonlPath);
        if (normalizedName == null || normalizedPath == null) {
            return;
        }
        if (containsCustomBookName(getCustomVocabularyBooks(), normalizedName)) {
            return;
        }
        List<CustomVocabularyBookData> next = new ArrayList<>(getCustomVocabularyBooks());
        CustomVocabularyBookData book = new CustomVocabularyBookData();
        book.name = normalizedName;
        book.jsonlPath = normalizedPath;
        book.createdAtEpochMillis = System.currentTimeMillis();
        next.add(book);
        setCustomVocabularyBooks(next);
    }

    public @NotNull String getSelectedVocabularyBookKey() {
        String selected = normalizeNullableText(state.selectedVocabularyBookKey);
        if (selected != null) {
            return selected;
        }
        // 兼容历史字段：如果存在旧的内置选择，则映射到新key
        return BUILTIN_KEY_PREFIX + getWordBuiltinVocabularyBook();
    }

    public void setSelectedVocabularyBookKey(@Nullable String key) {
        String normalized = normalizeNullableText(key);
        if (normalized == null) {
            normalized = BUILTIN_KEY_PREFIX + getWordBuiltinVocabularyBook();
        }
        if (Objects.equals(getSelectedVocabularyBookKey(), normalized)) {
            return;
        }
        state.selectedVocabularyBookKey = normalized;
        WordLibraryLoader.reloadWordEntriesFromSettings(this);
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .selectedVocabularyBookChanged(normalized);
    }

    public void setWordBuiltinVocabularyBook(@Nullable String bookId) {
        String normalized = bookId == null ? DEFAULT_WORD_BUILTIN_BOOK : bookId.trim();
        if (normalized.isEmpty()) {
            normalized = DEFAULT_WORD_BUILTIN_BOOK;
        }
        if (Objects.equals(getWordBuiltinVocabularyBook(), normalized)) {
            return;
        }
        state.wordBuiltinVocabularyBook = normalized;
        setSelectedVocabularyBookKey(BUILTIN_KEY_PREFIX + normalized);
    }

    public @Nullable String getWordSourceCustomPath() {
        String value = state.wordSourceCustomPath;
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public void setWordSourceConfig(boolean builtinEnabled, @Nullable String customPath) {
        String normalizedPath = customPath == null ? null : customPath.trim();
        if (normalizedPath != null && normalizedPath.isEmpty()) {
            normalizedPath = null;
        }
        if (isWordSourceBuiltinEnabled() == builtinEnabled && Objects.equals(getWordSourceCustomPath(), normalizedPath)) {
            return;
        }
        state.wordSourceBuiltinEnabled = builtinEnabled;
        state.wordSourceCustomPath = normalizedPath;
        WordLibraryLoader.reloadWordEntriesFromSettings(this);
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .wordSourceChanged(builtinEnabled, normalizedPath);
    }

    public @NotNull List<String> getWordFilterDifficulties() {
        return readNormalizedStringList(state.wordFilterDifficulties);
    }

    public @NotNull List<String> getWordFilterThemes() {
        return readNormalizedStringList(state.wordFilterThemes);
    }

    public @NotNull List<String> getWordFilterSources() {
        return readNormalizedStringList(state.wordFilterSources);
    }

    public void setWordCategoryFilters(@NotNull List<String> difficulties, @NotNull List<String> themes, @NotNull List<String> sources) {
        List<String> normalizedDifficulties = normalizeStringList(difficulties);
        List<String> normalizedThemes = normalizeStringList(themes);
        List<String> normalizedSources = normalizeStringList(sources);
        if (getWordFilterDifficulties().equals(normalizedDifficulties)
                && getWordFilterThemes().equals(normalizedThemes)
                && getWordFilterSources().equals(normalizedSources)) {
            return;
        }
        state.wordFilterDifficulties = normalizedDifficulties;
        state.wordFilterThemes = normalizedThemes;
        state.wordFilterSources = normalizedSources;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .wordCategoryFiltersChanged(normalizedDifficulties, normalizedThemes, normalizedSources);
    }

    public boolean isWordManagerPaneVisible() {
        Boolean value = state.wordManagerPaneVisible;
        return value == null ? DEFAULT_WORD_MANAGER_PANE_VISIBLE : value;
    }

    public void setWordManagerPaneVisible(boolean visible) {
        if (isWordManagerPaneVisible() == visible) {
            return;
        }
        state.wordManagerPaneVisible = visible;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .wordManagerPaneVisibilityChanged(visible);
    }

    public int getWordManagerPaneWidthPercent() {
        Integer value = state.wordManagerPaneWidthPercent;
        if (value == null) {
            return DEFAULT_WORD_MANAGER_PANE_WIDTH_PERCENT;
        }
        return clampPercent(value);
    }

    public void setWordManagerPaneWidthPercent(int percent) {
        int normalized = clampPercent(percent);
        if (getWordManagerPaneWidthPercent() == normalized) {
            return;
        }
        state.wordManagerPaneWidthPercent = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .wordManagerPaneWidthPercentChanged(normalized);
    }

    public boolean isBookManagerPaneVisible() {
        Boolean value = state.bookManagerPaneVisible;
        return value == null ? DEFAULT_BOOK_MANAGER_PANE_VISIBLE : value;
    }

    public void setBookManagerPaneVisible(boolean visible) {
        if (isBookManagerPaneVisible() == visible) {
            return;
        }
        state.bookManagerPaneVisible = visible;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .bookManagerPaneVisibilityChanged(visible);
    }

    public boolean isWordHiddenInPopup(@NotNull String bookKey, @Nullable String word) {
        String normalizedBookKey = normalizeNullableText(bookKey);
        String wordKey = normalizeWordKey(word);
        if (normalizedBookKey == null || wordKey == null) {
            return false;
        }
        Map<String, List<String>> map = state.hiddenWordsByVocabularyBookKey;
        if (map == null || map.isEmpty()) {
            return false;
        }
        List<String> hidden = map.get(normalizedBookKey);
        if (hidden == null || hidden.isEmpty()) {
            return false;
        }
        for (String candidate : hidden) {
            if (wordKey.equals(normalizeWordKey(candidate))) {
                return true;
            }
        }
        return false;
    }

    public void setWordHiddenInPopup(@NotNull String bookKey, @Nullable String word, boolean hidden) {
        String normalizedBookKey = normalizeNullableText(bookKey);
        String wordKey = normalizeWordKey(word);
        if (normalizedBookKey == null || wordKey == null) {
            return;
        }
        if (isWordHiddenInPopup(normalizedBookKey, wordKey) == hidden) {
            return;
        }
        if (state.hiddenWordsByVocabularyBookKey == null) {
            state.hiddenWordsByVocabularyBookKey = new LinkedHashMap<>();
        }
        List<String> current = state.hiddenWordsByVocabularyBookKey.get(normalizedBookKey);
        List<String> next = current == null ? new ArrayList<>() : new ArrayList<>(current);
        LinkedHashMap<String, Boolean> unique = new LinkedHashMap<>();
        for (String value : next) {
            String normalized = normalizeWordKey(value);
            if (normalized != null) {
                unique.put(normalized, Boolean.TRUE);
            }
        }
        boolean changed;
        if (hidden) {
            changed = unique.put(wordKey, Boolean.TRUE) == null;
        } else {
            changed = unique.remove(wordKey) != null;
        }
        if (!changed) {
            return;
        }
        List<String> normalizedList = new ArrayList<>(unique.keySet());
        if (normalizedList.isEmpty()) {
            state.hiddenWordsByVocabularyBookKey.remove(normalizedBookKey);
        } else {
            state.hiddenWordsByVocabularyBookKey.put(normalizedBookKey, normalizedList);
        }
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .wordHiddenStateChanged(normalizedBookKey);
    }

    public @NotNull List<WordEntryData> getWordEntries() {
        List<WordEntryData> entries = state.wordEntries;
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(entries);
    }

    public void setWordEntries(@NotNull List<WordEntryData> entries) {
        List<WordEntryData> normalized = new ArrayList<>();
        for (WordEntryData entry : entries) {
            if (entry == null) {
                continue;
            }
            String word = normalizeWordKey(entry.word);
            if (word == null) {
                continue;
            }
            WordEntryData copy = new WordEntryData();
            copy.word = word;
            copy.meaning = normalizeNullableText(entry.meaning);
            copy.phonetic = normalizeNullableText(entry.phonetic);
            copy.difficulty = normalizeNullableText(entry.difficulty);
            copy.theme = normalizeNullableText(entry.theme);
            copy.source = normalizeNullableText(entry.source);
            copy.sourceRef = normalizeNullableText(entry.sourceRef);
            copy.status = normalizeNullableText(entry.status);
            copy.sentenceEnList = normalizeStringList(entry.sentenceEnList == null ? List.of() : entry.sentenceEnList);
            copy.synonymsByPos = normalizeSynonymGroups(entry.synonymsByPos);
            normalized.add(copy);
        }
        state.wordEntries = normalized.isEmpty() ? null : normalized;
    }

    public @NotNull Map<String, WordLearningStateData> getWordLearningStates() {
        Map<String, WordLearningStateData> states = state.wordLearningStates;
        if (states == null || states.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(states);
    }

    public boolean isWordMastered(@Nullable String word) {
        String key = normalizeWordKey(word);
        if (key == null) {
            return false;
        }
        Map<String, WordLearningStateData> states = state.wordLearningStates;
        if (states == null) {
            return false;
        }
        WordLearningStateData learningState = states.get(key);
        return learningState != null && Boolean.TRUE.equals(learningState.mastered);
    }

    public void setWordMastered(@NotNull WordEntryData entry, boolean mastered) {
        if (entry.word == null || entry.word.isBlank()) {
            return;
        }
        String key = normalizeWordKey(entry.word);
        if (key == null) {
            return;
        }
        if (isWordMastered(key) == mastered) {
            return;
        }
        updateWordLearningState(key, mastered);
        if (mastered) {
            MasteredWordLibrary.upsert(entry);
        } else {
            MasteredWordLibrary.remove(entry.word);
        }
        if (Objects.equals(getSelectedVocabularyBookKey(), WordLibraryLoader.getSystemMasteredBookKey())) {
            WordLibraryLoader.reloadWordEntriesFromSettings(this);
            ApplicationManager.getApplication()
                    .getMessageBus()
                    .syncPublisher(PdfViewerSettingsListener.TOPIC)
                    .masteredWordLibraryChanged();
        }
    }

    public void setWordMastered(@Nullable String word, boolean mastered) {
        String key = normalizeWordKey(word);
        if (key == null) {
            return;
        }
        if (isWordMastered(key) == mastered) {
            return;
        }
        if (state.wordLearningStates == null) {
            state.wordLearningStates = new LinkedHashMap<>();
        }
        WordLearningStateData learningState = state.wordLearningStates.computeIfAbsent(key, unused -> new WordLearningStateData());
        learningState.mastered = mastered;
        long now = System.currentTimeMillis();
        learningState.lastReviewedAtEpochMillis = now;
        int previousCount = learningState.reviewCount == null ? 0 : Math.max(0, learningState.reviewCount);
        learningState.reviewCount = previousCount + 1;
        WordLibraryLoader.reloadWordEntriesFromSettings(this);
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .wordSourceChanged(isWordSourceBuiltinEnabled(), getWordSourceCustomPath());
    }

    public boolean toggleWordMastered(@NotNull WordEntryData entry) {
        boolean next = !isWordMastered(entry.word);
        setWordMastered(entry, next);
        return next;
    }

    public boolean toggleWordMastered(@Nullable String word) {
        boolean next = !isWordMastered(word);
        setWordMastered(word, next);
        return next;
    }

    private void updateWordLearningState(@NotNull String key, boolean mastered) {
        if (state.wordLearningStates == null) {
            state.wordLearningStates = new LinkedHashMap<>();
        }
        WordLearningStateData learningState = state.wordLearningStates.computeIfAbsent(key, unused -> new WordLearningStateData());
        learningState.mastered = mastered;
        long now = System.currentTimeMillis();
        learningState.lastReviewedAtEpochMillis = now;
        int previousCount = learningState.reviewCount == null ? 0 : Math.max(0, learningState.reviewCount);
        learningState.reviewCount = previousCount + 1;
    }

    public int getPdfReadPosition(@Nullable String pdfPath) {
        String key = normalizePdfPathKey(pdfPath);
        if (key == null) {
            return 0;
        }
        Map<String, Integer> positions = state.pdfReadPositions;
        if (positions == null) {
            return 0;
        }
        Integer value = positions.get(key);
        return value == null ? 0 : Math.max(0, value);
    }

    public void setPdfReadPosition(@Nullable String pdfPath, int position) {
        String key = normalizePdfPathKey(pdfPath);
        if (key == null) {
            return;
        }
        int normalized = Math.max(0, position);
        if (getPdfReadPosition(key) == normalized) {
            return;
        }
        if (state.pdfReadPositions == null) {
            state.pdfReadPositions = new HashMap<>();
        }
        state.pdfReadPositions.put(key, normalized);
    }

    public @NotNull List<BookData> getBooks() {
        List<BookData> normalized = normalizeBookLibrary(state.books);
        return normalized.isEmpty() ? List.of() : normalized;
    }

    public void setBooks(@NotNull List<BookData> books) {
        List<BookData> next = normalizeBookLibrary(books);
        List<BookData> current = normalizeBookLibrary(state.books);
        if (areBookLibrariesEqual(current, next)) {
            return;
        }
        state.books = next.isEmpty() ? null : next;
        String previousCurrentBookId = normalizeNullableText(state.currentReadingBookId);
        String nextCurrentBookId = previousCurrentBookId;
        if (nextCurrentBookId != null && !containsBookId(next, nextCurrentBookId)) {
            nextCurrentBookId = null;
            state.currentReadingBookId = null;
        }
        if (state.bookReadLineById != null && !state.bookReadLineById.isEmpty()) {
            LinkedHashMap<String, Integer> pruned = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : state.bookReadLineById.entrySet()) {
                String bookId = normalizeNullableText(entry.getKey());
                if (bookId == null || !containsBookId(next, bookId)) {
                    continue;
                }
                Integer lineNumber = entry.getValue();
                int normalizedLine = lineNumber == null ? 1 : Math.max(1, lineNumber);
                pruned.put(bookId, normalizedLine);
            }
            state.bookReadLineById = pruned.isEmpty() ? null : pruned;
        }
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .bookLibraryChanged();
        if (!Objects.equals(previousCurrentBookId, nextCurrentBookId)) {
            ApplicationManager.getApplication()
                    .getMessageBus()
                    .syncPublisher(PdfViewerSettingsListener.TOPIC)
                    .currentReadingBookChanged(nextCurrentBookId);
        }
    }

    public @Nullable String addImportedBook(@NotNull String name, @NotNull String filePath) {
        String normalizedName = normalizeNullableText(name);
        String normalizedPath = normalizeNullableText(filePath);
        if (normalizedName == null || normalizedPath == null) {
            return null;
        }
        List<BookData> current = new ArrayList<>(getBooks());
        String uniqueName = resolveUniqueBookName(current, normalizedName);
        BookData book = new BookData();
        book.id = UUID.randomUUID().toString();
        book.name = uniqueName;
        book.sourceType = BOOK_SOURCE_IMPORT;
        book.filePath = normalizedPath;
        book.inlineContent = safeReadTextFile(normalizedPath);
        book.createdAtEpochMillis = System.currentTimeMillis();
        current.add(book);
        setBooks(current);
        return book.id;
    }

    public @Nullable String addManualBook(@NotNull String name, @Nullable String inlineContent) {
        String normalizedName = normalizeNullableText(name);
        if (normalizedName == null) {
            return null;
        }
        List<BookData> current = new ArrayList<>(getBooks());
        String uniqueName = resolveUniqueBookName(current, normalizedName);
        BookData book = new BookData();
        book.id = UUID.randomUUID().toString();
        book.name = uniqueName;
        book.sourceType = BOOK_SOURCE_MANUAL;
        book.inlineContent = inlineContent;
        book.createdAtEpochMillis = System.currentTimeMillis();
        current.add(book);
        setBooks(current);
        return book.id;
    }

    public boolean reimportBook(@NotNull String bookId, @NotNull String filePath) {
        String normalizedId = normalizeNullableText(bookId);
        String normalizedPath = normalizeNullableText(filePath);
        if (normalizedId == null || normalizedPath == null) {
            return false;
        }
        List<BookData> current = new ArrayList<>(getBooks());
        boolean changed = false;
        for (BookData book : current) {
            if (book == null || book.id == null) {
                continue;
            }
            if (!normalizedId.equals(book.id)) {
                continue;
            }
            if (!BOOK_SOURCE_IMPORT.equals(normalizeBookSourceType(book.sourceType))) {
                return false;
            }
            String nextContent = safeReadTextFile(normalizedPath);
            if (Objects.equals(normalizeNullableText(book.filePath), normalizedPath)
                    && Objects.equals(book.inlineContent, nextContent)) {
                return false;
            }
            book.filePath = normalizedPath;
            book.inlineContent = nextContent;
            changed = true;
            break;
        }
        if (!changed) {
            return false;
        }
        setBooks(current);
        return true;
    }

    public boolean updateManualBookContent(@NotNull String bookId, @Nullable String inlineContent) {
        String normalizedId = normalizeNullableText(bookId);
        if (normalizedId == null) {
            return false;
        }
        List<BookData> current = new ArrayList<>(getBooks());
        boolean changed = false;
        for (BookData book : current) {
            if (book == null || book.id == null) {
                continue;
            }
            if (!normalizedId.equals(book.id)) {
                continue;
            }
            if (!BOOK_SOURCE_MANUAL.equals(normalizeBookSourceType(book.sourceType))) {
                return false;
            }
            if (Objects.equals(book.inlineContent, inlineContent)) {
                return false;
            }
            book.inlineContent = inlineContent;
            changed = true;
            break;
        }
        if (!changed) {
            return false;
        }
        setBooks(current);
        return true;
    }

    public boolean updateBook(@NotNull String bookId, @NotNull String name, @Nullable String inlineContent, @Nullable String filePath) {
        String normalizedId = normalizeNullableText(bookId);
        String normalizedName = normalizeNullableText(name);
        String normalizedPath = normalizeNullableText(filePath);
        if (normalizedId == null || normalizedName == null) {
            return false;
        }
        List<BookData> current = new ArrayList<>(getBooks());
        BookData target = null;
        for (BookData book : current) {
            if (book == null || book.id == null) {
                continue;
            }
            if (normalizedId.equals(book.id)) {
                target = book;
                break;
            }
        }
        if (target == null) {
            return false;
        }
        List<BookData> others = new ArrayList<>();
        for (BookData book : current) {
            if (book == null || book.id == null) {
                continue;
            }
            if (normalizedId.equals(book.id)) {
                continue;
            }
            others.add(book);
        }
        String uniqueName = Objects.equals(normalizeNullableText(target.name), normalizedName)
                ? normalizedName
                : resolveUniqueBookName(others, normalizedName);
        String sourceType = normalizeBookSourceType(target.sourceType);
        boolean changed = false;
        if (!Objects.equals(target.name, uniqueName)) {
            target.name = uniqueName;
            changed = true;
        }
        if (!Objects.equals(target.inlineContent, inlineContent)) {
            target.inlineContent = inlineContent;
            changed = true;
        }
        if (BOOK_SOURCE_IMPORT.equals(sourceType) && normalizedPath != null && !Objects.equals(normalizeNullableText(target.filePath), normalizedPath)) {
            target.filePath = normalizedPath;
            changed = true;
        }
        if (!changed) {
            return false;
        }
        setBooks(current);
        return true;
    }

    public boolean deleteBook(@NotNull String bookId) {
        String normalizedId = normalizeNullableText(bookId);
        if (normalizedId == null) {
            return false;
        }
        List<BookData> current = new ArrayList<>(getBooks());
        boolean removed = current.removeIf(book -> book != null && normalizedId.equals(book.id));
        if (!removed) {
            return false;
        }
        setBooks(current);
        return true;
    }

    public @Nullable String getCurrentReadingBookId() {
        String value = normalizeNullableText(state.currentReadingBookId);
        if (value == null) {
            return null;
        }
        if (!containsBookId(getBooks(), value)) {
            return null;
        }
        return value;
    }

    public void setCurrentReadingBookId(@Nullable String bookId) {
        String normalized = normalizeNullableText(bookId);
        if (normalized != null && !containsBookId(getBooks(), normalized)) {
            normalized = null;
        }
        if (Objects.equals(getCurrentReadingBookId(), normalized)) {
            return;
        }
        state.currentReadingBookId = normalized;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .currentReadingBookChanged(normalized);
    }

    public int getBookReadLine(@Nullable String bookId) {
        String normalized = normalizeNullableText(bookId);
        if (normalized == null) {
            return 1;
        }
        Map<String, Integer> map = state.bookReadLineById;
        if (map == null || map.isEmpty()) {
            return 1;
        }
        Integer value = map.get(normalized);
        return value == null ? 1 : Math.max(1, value);
    }

    public void setBookReadLine(@NotNull String bookId, int lineNumber) {
        String normalizedBookId = normalizeNullableText(bookId);
        if (normalizedBookId == null) {
            return;
        }
        if (!containsBookId(getBooks(), normalizedBookId)) {
            return;
        }
        int normalizedLine = Math.max(1, lineNumber);
        if (getBookReadLine(normalizedBookId) == normalizedLine) {
            return;
        }
        if (state.bookReadLineById == null) {
            state.bookReadLineById = new LinkedHashMap<>();
        }
        state.bookReadLineById.put(normalizedBookId, normalizedLine);
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .bookReadPositionChanged(normalizedBookId, normalizedLine);
    }

    public @NotNull String getBookReadingOutputMode() {
        String value = normalizeNullableText(state.bookReadingOutputMode);
        if (value == null) {
            return "notification";
        }
        return "inline".equalsIgnoreCase(value) ? "inline" : "notification";
    }

    public void setBookReadingOutputMode(@Nullable String mode) {
        String normalized = normalizeNullableText(mode);
        String resolved = "inline".equalsIgnoreCase(normalized) ? "inline" : "notification";
        if (Objects.equals(getBookReadingOutputMode(), resolved)) {
            return;
        }
        state.bookReadingOutputMode = resolved;
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(PdfViewerSettingsListener.TOPIC)
                .bookReadingOutputModeChanged(resolved);
    }

    private static @Nullable String normalizePdfPathKey(@Nullable String pdfPath) {
        if (pdfPath == null) {
            return null;
        }
        String trimmed = pdfPath.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void normalizeStateAfterLoad() {
        List<BookData> normalizedBooks = normalizeBookLibrary(state.books);
        state.books = normalizedBooks.isEmpty() ? null : normalizedBooks;

        if (state.bookReadLineById != null && !state.bookReadLineById.isEmpty()) {
            LinkedHashMap<String, Integer> normalizedMap = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : state.bookReadLineById.entrySet()) {
                String bookId = normalizeNullableText(entry.getKey());
                if (bookId == null || !containsBookId(normalizedBooks, bookId)) {
                    continue;
                }
                Integer lineNumber = entry.getValue();
                int normalizedLine = lineNumber == null ? 1 : Math.max(1, lineNumber);
                normalizedMap.put(bookId, normalizedLine);
            }
            state.bookReadLineById = normalizedMap.isEmpty() ? null : normalizedMap;
        }

        String currentBookId = normalizeNullableText(state.currentReadingBookId);
        if (currentBookId != null && !containsBookId(normalizedBooks, currentBookId)) {
            state.currentReadingBookId = null;
        }
    }

    private static @NotNull List<BookData> normalizeBookLibrary(@Nullable List<BookData> books) {
        if (books == null || books.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, BookData> uniqueById = new LinkedHashMap<>();
        List<BookData> normalizedList = new ArrayList<>();
        for (BookData book : books) {
            if (book == null) {
                continue;
            }
            String id = normalizeNullableText(book.id);
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            if (uniqueById.containsKey(id)) {
                continue;
            }
            String name = normalizeNullableText(book.name);
            if (name == null) {
                continue;
            }
            String uniqueName = resolveUniqueBookName(normalizedList, name);
            BookData copy = new BookData();
            copy.id = id;
            copy.name = uniqueName;
            copy.sourceType = normalizeBookSourceType(book.sourceType);
            copy.filePath = normalizeNullableText(book.filePath);
            copy.inlineContent = book.inlineContent;
            copy.createdAtEpochMillis = book.createdAtEpochMillis;
            uniqueById.put(id, copy);
            normalizedList.add(copy);
        }
        return normalizedList.isEmpty() ? List.of() : normalizedList;
    }

    private static @NotNull String normalizeBookSourceType(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return BOOK_SOURCE_MANUAL;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("导入".equals(normalized) || "import".equals(normalized)) {
            return BOOK_SOURCE_IMPORT;
        }
        if ("手动".equals(normalized) || "manual".equals(normalized)) {
            return BOOK_SOURCE_MANUAL;
        }
        return BOOK_SOURCE_MANUAL;
    }

    private static boolean containsBookId(@NotNull List<BookData> books, @NotNull String bookId) {
        for (BookData book : books) {
            if (book == null || book.id == null) {
                continue;
            }
            if (bookId.equals(book.id)) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull String resolveUniqueBookName(@NotNull List<BookData> existingBooks, @NotNull String desiredName) {
        String base = desiredName.trim();
        if (!containsBookName(existingBooks, base)) {
            return base;
        }
        int index = 2;
        while (true) {
            String candidate = base + " (" + index + ")";
            if (!containsBookName(existingBooks, candidate)) {
                return candidate;
            }
            index++;
        }
    }

    private static boolean containsBookName(@NotNull List<BookData> existingBooks, @NotNull String name) {
        String target = name.trim().toLowerCase(Locale.ROOT);
        for (BookData existing : existingBooks) {
            if (existing == null || existing.name == null) {
                continue;
            }
            String current = existing.name.trim().toLowerCase(Locale.ROOT);
            if (current.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean areBookLibrariesEqual(@NotNull List<BookData> left, @NotNull List<BookData> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            BookData a = left.get(i);
            BookData b = right.get(i);
            if (!areBooksEqual(a, b)) {
                return false;
            }
        }
        return true;
    }

    private static boolean areBooksEqual(@Nullable BookData a, @Nullable BookData b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(a.id, b.id)
                && Objects.equals(a.name, b.name)
                && Objects.equals(a.sourceType, b.sourceType)
                && Objects.equals(a.filePath, b.filePath)
                && Objects.equals(a.inlineContent, b.inlineContent)
                && Objects.equals(a.createdAtEpochMillis, b.createdAtEpochMillis);
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

    private static int clampPercent(int percent) {
        return Math.max(10, Math.min(60, percent));
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

    private static @NotNull List<String> readNormalizedStringList(@Nullable List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return normalizeStringList(values);
    }

    private static @NotNull List<WordSynonymGroupData> normalizeSynonymGroups(@Nullable List<WordSynonymGroupData> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        List<WordSynonymGroupData> normalized = new ArrayList<>();
        for (WordSynonymGroupData group : groups) {
            if (group == null) {
                continue;
            }
            String pos = normalizeNullableText(group.pos);
            List<String> words = normalizeStringList(group.words == null ? List.of() : group.words);
            if (pos == null || words.isEmpty()) {
                continue;
            }
            WordSynonymGroupData copy = new WordSynonymGroupData();
            copy.pos = pos;
            copy.words = words;
            normalized.add(copy);
        }
        return normalized.isEmpty() ? List.of() : normalized;
    }

    private static @NotNull List<CustomVocabularyBookData> normalizeCustomBooks(@NotNull List<CustomVocabularyBookData> books) {
        LinkedHashMap<String, CustomVocabularyBookData> unique = new LinkedHashMap<>();
        for (CustomVocabularyBookData book : books) {
            if (book == null) {
                continue;
            }
            String name = normalizeNullableText(book.name);
            String path = normalizeNullableText(book.jsonlPath);
            if (name == null || path == null) {
                continue;
            }
            if (containsCustomBookName(unique.values(), name)) {
                continue;
            }
            CustomVocabularyBookData copy = new CustomVocabularyBookData();
            copy.name = name;
            copy.jsonlPath = path;
            copy.createdAtEpochMillis = book.createdAtEpochMillis;
            unique.put(name, copy);
        }
        return unique.isEmpty() ? List.of() : new ArrayList<>(unique.values());
    }

    private static boolean containsCustomBookName(@NotNull Iterable<CustomVocabularyBookData> books, @NotNull String name) {
        String target = name.trim().toLowerCase(Locale.ROOT);
        for (CustomVocabularyBookData existing : books) {
            if (existing == null || existing.name == null) {
                continue;
            }
            String current = existing.name.trim().toLowerCase(Locale.ROOT);
            if (current.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static int clampPanePercent(int value) {
        return Math.max(5, Math.min(90, value));
    }

    private static int clampPopupSize(int value) {
        return Math.max(1, Math.min(2000, value));
    }

    private static int clampWordPopupSize(int value) {
        return Math.max(1, Math.min(2000, value));
    }

    private static int clampWordPopupCoordinate(int value) {
        return Math.max(-5000, Math.min(5000, value));
    }

    private static int clampWordPopupFontSize(int value) {
        return Math.max(8, Math.min(72, value));
    }

    private static int clampWordPopupSentenceLimit(int value) {
        return Math.max(1, Math.min(5, value));
    }

    private static int clampPopupOpacityPercent(int value) {
        return Math.max(MIN_POPUP_OPACITY_PERCENT, Math.min(MAX_POPUP_OPACITY_PERCENT, value));
    }

    private static int[] normalizePaneRatios(int leftPercent, int middlePercent, int rightPercent) {
        int l = clampPanePercent(leftPercent);
        int m = clampPanePercent(middlePercent);
        int r = clampPanePercent(rightPercent);
        int sum = l + m + r;
        if (sum <= 0) {
            return new int[]{DEFAULT_PANE_LEFT_PERCENT, DEFAULT_PANE_MIDDLE_PERCENT, DEFAULT_PANE_RIGHT_PERCENT};
        }
        int nl = clampPanePercent((int) Math.round(l * 100.0 / sum));
        int nm = clampPanePercent((int) Math.round(m * 100.0 / sum));
        int nr = 100 - nl - nm;
        if (nr < 5) {
            nr = 5;
            if (nm > 5) {
                nm = Math.max(5, nm - 1);
            } else {
                nl = Math.max(5, nl - 1);
            }
        } else if (nr > 90) {
            nr = 90;
        }

        int fixed = nl + nm + nr;
        if (fixed != 100) {
            int delta = 100 - fixed;
            if (nm + delta >= 5 && nm + delta <= 90) {
                nm += delta;
            } else {
                nl = clampPanePercent(nl + delta);
            }
            nr = 100 - nl - nm;
        }
        return new int[]{clampPanePercent(nl), clampPanePercent(nm), clampPanePercent(nr)};
    }

    private static int clampColorChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static @Nullable String safeReadTextFile(@NotNull String filePath) {
        try {
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
            if (file != null && file.isValid()) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), file.getCharset()))) {
                    String line;
                    boolean first = true;
                    while ((line = reader.readLine()) != null) {
                        if (!first) {
                            sb.append('\n');
                        }
                        sb.append(line);
                        first = false;
                    }
                }
                return sb.toString();
            }
            Path path = Paths.get(filePath);
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }
}
