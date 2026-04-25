package com.aiden.plugin.viewpdf.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private static final int DEFAULT_TREE_BG_R = 43;
    private static final int DEFAULT_TREE_BG_G = 45;
    private static final int DEFAULT_TREE_BG_B = 48;
    private static final int DEFAULT_TREE_TEXT_R = 220;
    private static final int DEFAULT_TREE_TEXT_G = 220;
    private static final int DEFAULT_TREE_TEXT_B = 220;
    private static final int DEFAULT_TREE_FONT_SIZE = 12;
    private static final int DEFAULT_HOVER_SECONDS = 10;
    private static final int DEFAULT_ZOOM_PERCENT = 100;
    private static final int DEFAULT_PANE_LEFT_PERCENT = 25;
    private static final int DEFAULT_PANE_MIDDLE_PERCENT = 45;
    private static final int DEFAULT_PANE_RIGHT_PERCENT = 30;
    private static final int DEFAULT_RENDER_BATCH_PAGE_COUNT = 50;
    private static final int DEFAULT_EDITOR_POPUP_WIDTH = 760;
    private static final int DEFAULT_EDITOR_POPUP_HEIGHT = 520;

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

    private static @Nullable String normalizePdfPathKey(@Nullable String pdfPath) {
        if (pdfPath == null) {
            return null;
        }
        String trimmed = pdfPath.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int clampPanePercent(int value) {
        return Math.max(5, Math.min(90, value));
    }

    private static int clampPopupSize(int value) {
        return Math.max(300, Math.min(2000, value));
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
}
