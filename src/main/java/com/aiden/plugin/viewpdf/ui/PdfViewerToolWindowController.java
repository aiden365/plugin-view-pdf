package com.aiden.plugin.viewpdf.ui;

import org.jetbrains.annotations.NotNull;

public final class PdfViewerToolWindowController {
    private final @NotNull PdfViewerPanel pdfPanel;
    private boolean pdfVisible;

    public PdfViewerToolWindowController(@NotNull PdfViewerPanel pdfPanel) {
        this.pdfPanel = pdfPanel;
    }

    public @NotNull PdfViewerPanel getPdfPanel() {
        return pdfPanel;
    }

    public boolean isPdfVisible() {
        return pdfVisible;
    }

    public void setPdfVisible(boolean pdfVisible) {
        this.pdfVisible = pdfVisible;
    }

    public void scrollPdfByViewport(boolean down) {
        pdfPanel.scrollByViewport(down);
    }
}

