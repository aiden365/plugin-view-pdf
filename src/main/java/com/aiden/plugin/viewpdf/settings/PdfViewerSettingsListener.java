package com.aiden.plugin.viewpdf.settings;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public interface PdfViewerSettingsListener {
    Topic<PdfViewerSettingsListener> TOPIC =
            Topic.create("PdfViewerSettingsListener", PdfViewerSettingsListener.class);

    void pdfPathChanged(@Nullable String newPdfPath);

    void nightModeChanged(boolean enabled);

    void pdfBackgroundChanged(@NotNull Color newBackgroundColor);

    void hoverSecondsChanged(int seconds);

    void zoomPercentChanged(int percent);

    void pdfTextColorChanged(@NotNull Color newTextColor);
}
