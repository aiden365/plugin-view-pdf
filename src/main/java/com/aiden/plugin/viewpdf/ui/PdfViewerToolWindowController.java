package com.aiden.plugin.viewpdf.ui;

import org.jetbrains.annotations.NotNull;

public final class PdfViewerToolWindowController {
    private final @NotNull PdfViewerPanel pdfPanel;
    private final @NotNull StealthSplitPanel splitPanel;
    private boolean pdfVisible;

    public PdfViewerToolWindowController(@NotNull PdfViewerPanel pdfPanel, @NotNull StealthSplitPanel splitPanel) {
        this.pdfPanel = pdfPanel;
        this.splitPanel = splitPanel;
    }

    public @NotNull PdfViewerPanel getPdfPanel() {
        return pdfPanel;
    }

    public boolean isPdfVisible() {
        return pdfVisible;
    }

    public boolean isPdfToggleEnabled() {
        return splitPanel.isPdfToggleEnabled();
    }

    public void setPdfVisible(boolean pdfVisible) {
        this.pdfVisible = pdfVisible;
    }

    public void scrollPdfByViewport(boolean down) {
        pdfPanel.scrollByViewport(down);
    }

    public void closePdfView() {
        splitPanel.setPdfToggleEnabled(false);
        splitPanel.showDisguise();
        setPdfVisible(false);
    }

    public void togglePdfView() {
        boolean nextEnabled = !splitPanel.isPdfToggleEnabled();
        splitPanel.setPdfToggleEnabled(nextEnabled);
        if (nextEnabled) {
            splitPanel.showPdf();
            setPdfVisible(true);
        } else {
            splitPanel.showDisguise();
            setPdfVisible(false);
        }
    }
}
