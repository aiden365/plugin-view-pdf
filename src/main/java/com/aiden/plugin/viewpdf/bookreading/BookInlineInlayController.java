package com.aiden.plugin.viewpdf.bookreading;

import com.aiden.plugin.viewpdf.PdfViewerKeys;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.Objects;

public final class BookInlineInlayController implements Disposable {
    private final Project project;
    private Editor editor;
    private Inlay<?> inlineInlay;
    private Inlay<?> blockInlay;
    private String lastFullText;
    private String tooltipText;
    private int lastOffset = -1;

    public static @NotNull BookInlineInlayController getOrCreate(@NotNull Project project) {
        BookInlineInlayController existing = project.getUserData(PdfViewerKeys.BOOK_INLINE_INLAY_CONTROLLER_KEY);
        if (existing != null) {
            return existing;
        }
        BookInlineInlayController created = new BookInlineInlayController(project);
        project.putUserData(PdfViewerKeys.BOOK_INLINE_INLAY_CONTROLLER_KEY, created);
        Disposer.register(project, created);
        return created;
    }

    private BookInlineInlayController(@NotNull Project project) {
        this.project = project;
    }

    public void moveAndShowLine(@NotNull Editor editor, int delta) {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        String bookId = settings.getCurrentReadingBookId();
        if (bookId == null) {
            clear();
            return;
        }
        PdfViewerSettings.BookData book = findBookById(settings.getBooks(), bookId);
        if (book == null || book.inlineContent == null || book.inlineContent.isBlank()) {
            clear();
            return;
        }
        int currentLine = settings.getBookReadLine(bookId);
        int targetLine = Math.max(1, currentLine + delta);
        String line = BookReadingTextUtil.readLine(book.inlineContent, targetLine);
        if (line == null) {
            clear();
            return;
        }
        settings.setBookReadLine(bookId, targetLine);
        showLine(editor, line);
    }

    public void showLine(@NotNull Editor editor, @NotNull String lineText) {
        int offset = editor.getCaretModel().getOffset();
        if (this.editor != editor) {
            detach();
            attach(editor);
        }
        if (inlineInlay != null && offset == lastOffset && Objects.equals(lastFullText, lineText)) {
            return;
        }
        clearInlay();
        FontMetrics metrics = editor.getContentComponent().getFontMetrics(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
        int maxWidth = resolveAvailableWidth(editor);
        List<String> wrapped = BookReadingTextUtil.wrapToWidth(lineText, maxWidth, metrics);
        String first = wrapped.isEmpty() ? "" : wrapped.get(0);
        List<String> rest = wrapped.size() <= 1 ? List.of() : wrapped.subList(1, wrapped.size());
        this.lastFullText = lineText;
        this.tooltipText = rest.isEmpty() ? null : lineText;
        this.lastOffset = offset;
        this.inlineInlay = editor.getInlayModel().addInlineElement(offset, true, new InlineTextRenderer(first));
        if (!rest.isEmpty()) {
            int line = editor.getDocument().getLineNumber(offset);
            int lineEndOffset = editor.getDocument().getLineEndOffset(line);
            int indentPx = editor.offsetToXY(offset).x;
            this.blockInlay = editor.getInlayModel().addBlockElement(
                    lineEndOffset,
                    true,
                    false,
                    0,
                    new BlockTextRenderer(rest, indentPx)
            );
        }
    }

    public void clear() {
        clearInlay();
    }

    @Override
    public void dispose() {
        detach();
    }

    private void attach(@NotNull Editor editor) {
        this.editor = editor;
        editor.getDocument().addDocumentListener(documentListener);
        editor.getCaretModel().addCaretListener(caretListener);
        editor.getContentComponent().addMouseMotionListener(mouseMotionListener);
    }

    private void detach() {
        if (editor != null) {
            editor.getDocument().removeDocumentListener(documentListener);
            editor.getCaretModel().removeCaretListener(caretListener);
            editor.getContentComponent().removeMouseMotionListener(mouseMotionListener);
            editor.getContentComponent().setToolTipText(null);
        }
        editor = null;
        clearInlay();
        lastFullText = null;
        tooltipText = null;
        lastOffset = -1;
    }

    private void clearInlay() {
        Inlay<?> currentInline = inlineInlay;
        Inlay<?> currentBlock = blockInlay;
        inlineInlay = null;
        blockInlay = null;
        tooltipText = null;
        if (currentInline != null) {
            Disposer.dispose(currentInline);
        }
        if (currentBlock != null) {
            Disposer.dispose(currentBlock);
        }
    }

    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            clearInlay();
        }
    };

    private final CaretListener caretListener = new CaretListener() {
        @Override
        public void caretPositionChanged(@NotNull CaretEvent event) {
            clearInlay();
        }
    };

    private final MouseMotionAdapter mouseMotionListener = new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            Editor currentEditor = editor;
            Inlay<?> currentInline = inlineInlay;
            Inlay<?> currentBlock = blockInlay;
            if (currentEditor == null || currentInline == null) {
                return;
            }
            Rectangle inlineBounds = currentInline.getBounds();
            Rectangle blockBounds = currentBlock == null ? null : currentBlock.getBounds();
            if (tooltipText != null && ((inlineBounds != null && inlineBounds.contains(e.getPoint()))
                    || (blockBounds != null && blockBounds.contains(e.getPoint())))) {
                currentEditor.getContentComponent().setToolTipText(tooltipText);
                return;
            }
            currentEditor.getContentComponent().setToolTipText(null);
        }
    };

    private static int resolveAvailableWidth(@NotNull Editor editor) {
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        VisualPosition visualPosition = editor.getCaretModel().getVisualPosition();
        java.awt.Point caretPoint = editor.visualPositionToXY(visualPosition);
        int available = visibleArea.width - caretPoint.x - 12;
        return Math.max(24, available);
    }

    private static @Nullable PdfViewerSettings.BookData findBookById(@NotNull List<PdfViewerSettings.BookData> books, @NotNull String bookId) {
        for (PdfViewerSettings.BookData book : books) {
            if (book == null || book.id == null) {
                continue;
            }
            if (bookId.equals(book.id)) {
                return book;
            }
        }
        return null;
    }

    private static final class InlineTextRenderer implements com.intellij.openapi.editor.EditorCustomElementRenderer {
        private final String text;

        private InlineTextRenderer(@NotNull String text) {
            this.text = text;
        }

        @Override
        public int calcWidthInPixels(@NotNull Inlay inlay) {
            Editor editor = inlay.getEditor();
            FontMetrics metrics = editor.getContentComponent().getFontMetrics(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
            return metrics.stringWidth(text);
        }

        @Override
        public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
            Editor editor = inlay.getEditor();
            FontMetrics metrics = editor.getContentComponent().getFontMetrics(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
            int x = targetRegion.x;
            int y = targetRegion.y + metrics.getAscent();
            Graphics2D g2 = (Graphics2D) g;
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            g2.setColor(editor.getColorsScheme().getDefaultForeground() == null ? JBColor.GRAY : editor.getColorsScheme().getDefaultForeground());
            g2.setFont(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
            g2.drawString(text, x, y);
            g2.setComposite(old);
        }
    }

    private static final class BlockTextRenderer implements com.intellij.openapi.editor.EditorCustomElementRenderer {
        private final List<String> lines;
        private final int indentPx;

        private BlockTextRenderer(@NotNull List<String> lines, int indentPx) {
            this.lines = List.copyOf(lines);
            this.indentPx = Math.max(0, indentPx);
        }

        @Override
        public int calcWidthInPixels(@NotNull Inlay inlay) {
            Editor editor = inlay.getEditor();
            FontMetrics metrics = editor.getContentComponent().getFontMetrics(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
            int max = 0;
            for (String line : lines) {
                if (line == null) {
                    continue;
                }
                max = Math.max(max, metrics.stringWidth(line));
            }
            return indentPx + max;
        }

        @Override
        public int calcHeightInPixels(@NotNull Inlay inlay) {
            Editor editor = inlay.getEditor();
            int lineHeight = editor.getLineHeight();
            return Math.max(lineHeight, lineHeight * lines.size());
        }

        @Override
        public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
            Editor editor = inlay.getEditor();
            FontMetrics metrics = editor.getContentComponent().getFontMetrics(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
            int lineHeight = editor.getLineHeight();
            Graphics2D g2 = (Graphics2D) g;
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            g2.setColor(editor.getColorsScheme().getDefaultForeground() == null ? JBColor.GRAY : editor.getColorsScheme().getDefaultForeground());
            g2.setFont(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
            int x = targetRegion.x + indentPx;
            int y = targetRegion.y + metrics.getAscent();
            for (int i = 0; i < lines.size(); i++) {
                String text = lines.get(i);
                if (text != null && !text.isEmpty()) {
                    g2.drawString(text, x, y + (i * lineHeight));
                }
            }
            g2.setComposite(old);
        }
    }
}
