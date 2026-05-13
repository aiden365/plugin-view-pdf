package com.aiden.plugin.viewpdf;

import com.aiden.plugin.viewpdf.ui.PdfViewerToolWindowController;
import com.aiden.plugin.viewpdf.popup.EditorPdfPopupController;
import com.aiden.plugin.viewpdf.popup.WordPopupController;
import com.intellij.openapi.util.Key;

public final class PdfViewerKeys {
    public static final Key<PdfViewerToolWindowController> CONTROLLER_KEY = Key.create("PdfViewerToolWindowController");
    public static final Key<EditorPdfPopupController> EDITOR_POPUP_CONTROLLER_KEY = Key.create("EditorPdfPopupController");
    public static final Key<WordPopupController> WORD_POPUP_CONTROLLER_KEY = Key.create("WordPopupController");

    private PdfViewerKeys() {
    }
}
