package com.aiden.plugin.viewpdf;

import com.aiden.plugin.viewpdf.ui.PdfViewerToolWindowController;
import com.intellij.openapi.util.Key;

public final class PdfViewerKeys {
    public static final Key<PdfViewerToolWindowController> CONTROLLER_KEY = Key.create("PdfViewerToolWindowController");

    private PdfViewerKeys() {
    }
}
