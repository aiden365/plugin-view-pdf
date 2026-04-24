package com.aiden.plugin.viewpdf.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
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
    private static final int DEFAULT_HOVER_SECONDS = 10;
    private static final int DEFAULT_ZOOM_PERCENT = 100;

    public static final class StateData {
        public String pdfPath;
        public boolean nightModeEnabled;
        public Integer pdfBackgroundR;
        public Integer pdfBackgroundG;
        public Integer pdfBackgroundB;
        public Integer pdfTextR;
        public Integer pdfTextG;
        public Integer pdfTextB;
        public Integer autoShowPdfHoverSeconds;
        public Integer pdfZoomPercent;
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

    public int getAutoShowPdfHoverSeconds() {
        Integer value = state.autoShowPdfHoverSeconds;
        if (value == null) {
            return DEFAULT_HOVER_SECONDS;
        }
        return Math.max(1, value);
    }

    public void setAutoShowPdfHoverSeconds(int seconds) {
        int normalized = Math.max(1, seconds);
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

    private static int clampColorChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
