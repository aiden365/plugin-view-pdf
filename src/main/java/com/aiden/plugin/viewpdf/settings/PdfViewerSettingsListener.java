package com.aiden.plugin.viewpdf.settings;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;

public interface PdfViewerSettingsListener {
    Topic<PdfViewerSettingsListener> TOPIC =
            Topic.create("PdfViewerSettingsListener", PdfViewerSettingsListener.class);

    void pdfPathChanged(@Nullable String newPdfPath);

    void nightModeChanged(boolean enabled);

    void pdfBackgroundChanged(@NotNull Color newBackgroundColor);

    void hoverSecondsChanged(int seconds);

    void zoomPercentChanged(int percent);

    void pdfTextColorChanged(@NotNull Color newTextColor);

    void treeBackgroundChanged(@NotNull Color newBackgroundColor);

    void treeTextColorChanged(@NotNull Color newTextColor);

    void treeFontSizeChanged(int size);

    void paneRatiosChanged(int leftPercent, int middlePercent, int rightPercent);

    void thirdPaneVisibilityChanged(boolean visible);

    void editorPopupSizeChanged(int width, int height);

    void editorPopupBorderVisibilityChanged(boolean visible);

    void editorPopupPdfBackgroundChanged(@NotNull Color newBackgroundColor);

    void editorPopupPdfTextColorChanged(@NotNull Color newTextColor);

    void renderBatchPageCountChanged(int pageCount);

    void wordPopupStyleChanged(int width, int height, int x, int y, int fontSize, @NotNull Color fontColor);

    default void wordPopupContentDisplayChanged(boolean showMeaning, boolean showSentence, boolean showSynonyms, int sentenceLimit) {
    }

    default void vocabularyBookListChanged() {
    }

    default void selectedVocabularyBookChanged(@NotNull String key) {
    }

    default void editorPopupOpacityChanged(int percent) {
    }

    default void wordPopupOpacityChanged(int percent) {
    }

    void wordSourceChanged(boolean builtinEnabled, @Nullable String customPath);

    void wordCategoryFiltersChanged(@NotNull List<String> difficulties, @NotNull List<String> themes, @NotNull List<String> sources);

    default void wordManagerPaneVisibilityChanged(boolean visible) {
    }

    default void wordManagerPaneWidthPercentChanged(int percent) {
    }

    default void wordHiddenStateChanged(@NotNull String bookKey) {
    }
}
