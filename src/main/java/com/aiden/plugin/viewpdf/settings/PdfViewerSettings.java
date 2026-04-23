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

    public static final class StateData {
        public String pdfPath;
        public boolean nightModeEnabled;
        public Integer pdfBackgroundR;
        public Integer pdfBackgroundG;
        public Integer pdfBackgroundB;
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

    private static int clampColorChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
