package com.aiden.plugin.viewpdf.bookreading;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettingsListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;

public final class BookReadingStartupActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(project)
                .subscribe(PdfViewerSettingsListener.TOPIC, new PdfViewerSettingsListener() {
                    @Override
                    public void pdfPathChanged(@Nullable String newPdfPath) {
                    }

                    @Override
                    public void nightModeChanged(boolean enabled) {
                    }

                    @Override
                    public void pdfBackgroundChanged(@NotNull Color newBackgroundColor) {
                    }

                    @Override
                    public void hoverSecondsChanged(int seconds) {
                    }

                    @Override
                    public void zoomPercentChanged(int percent) {
                    }

                    @Override
                    public void pdfTextColorChanged(@NotNull Color newTextColor) {
                    }

                    @Override
                    public void treeBackgroundChanged(@NotNull Color newBackgroundColor) {
                    }

                    @Override
                    public void treeTextColorChanged(@NotNull Color newTextColor) {
                    }

                    @Override
                    public void treeFontSizeChanged(int size) {
                    }

                    @Override
                    public void paneRatiosChanged(int leftPercent, int middlePercent, int rightPercent) {
                    }

                    @Override
                    public void thirdPaneVisibilityChanged(boolean visible) {
                    }

                    @Override
                    public void editorPopupSizeChanged(int width, int height) {
                    }

                    @Override
                    public void editorPopupBorderVisibilityChanged(boolean visible) {
                    }

                    @Override
                    public void editorPopupPdfBackgroundChanged(@NotNull Color newBackgroundColor) {
                    }

                    @Override
                    public void editorPopupPdfTextColorChanged(@NotNull Color newTextColor) {
                    }

                    @Override
                    public void renderBatchPageCountChanged(int pageCount) {
                    }

                    @Override
                    public void wordPopupStyleChanged(int width, int height, int x, int y, int fontSize, @NotNull Color fontColor) {
                    }

                    @Override
                    public void wordSourceChanged(boolean builtinEnabled, @Nullable String customPath) {
                    }

                    @Override
                    public void wordCategoryFiltersChanged(@NotNull List<String> difficulties, @NotNull List<String> themes, @NotNull List<String> sources) {
                    }

                    @Override
                    public void currentReadingBookChanged(@Nullable String bookId) {
                        BookReadingNotifier.showFirstLineOnBookSelected(project, bookId);
                    }

                    @Override
                    public void bookReadPositionChanged(@NotNull String bookId, int lineNumber) {
                    }
                });
    }
}
