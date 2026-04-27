package com.aiden.plugin.viewpdf.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBScrollPane;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class PdfViewerPanel implements Disposable {
    private static final float DPI = 144f;
    private static final Color DEFAULT_BACKGROUND = new Color(43, 45, 48);
    private static final int DEFAULT_PAGES_PER_BATCH = 50;
    private static final int NEXT_BATCH_THRESHOLD_PX = 400;
    private static final int SLIM_SCROLLBAR_THICKNESS = 6;

    private final JPanel root;
    private final JBScrollPane scrollPane;
    private final JPanel pagesPanel;
    private final JLabel messageLabel;

    private Color backgroundColor;
    private Color textColor = new Color(220, 220, 220);
    private MouseAdapter regionMouseListener;
    private final MouseAdapter dragPanListener;
    private boolean dragPanEnabled;
    private boolean panning;
    private @Nullable Point panAnchorOnScreen;
    private @Nullable Point panStartViewPosition;
    private int zoomPercent = 100;
    private @Nullable String currentPdfPath;
    private int pendingRestoreScrollValue;
    private @Nullable Function<String, Integer> readingPositionLoader;
    private @Nullable BiConsumer<String, Integer> readingPositionSaver;
    private boolean suppressAutoSave;
    private @Nullable Timer restoreTimer;
    private boolean hasRenderedDocument;
    private @Nullable String renderedPdfPath;
    private boolean renderedNightMode;
    private int renderedZoomPercent = 100;
    private @NotNull Color renderedTextColor = new Color(220, 220, 220);
    private @NotNull Color renderedBackgroundColor = DEFAULT_BACKGROUND;
    private int totalPageCount;
    private int renderedPageCount;
    private boolean loadingBatch;
    private boolean currentNightMode;
    private int pagesPerBatch = DEFAULT_PAGES_PER_BATCH;

    private final AtomicInteger loadSeq = new AtomicInteger(0);
    private SwingWorker<BatchRenderResult, Void> worker;

    public PdfViewerPanel() {
        root = new JPanel(new BorderLayout());

        messageLabel = new JLabel();
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        root.add(messageLabel, BorderLayout.CENTER);

        pagesPanel = new JPanel();
        pagesPanel.setLayout(new javax.swing.BoxLayout(pagesPanel, javax.swing.BoxLayout.Y_AXIS));
        scrollPane = new JBScrollPane(pagesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        installSlimScrollBars();
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            saveCurrentReadingPosition();
            maybeRequestNextBatch();
        });

        dragPanListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!dragPanEnabled || !javax.swing.SwingUtilities.isLeftMouseButton(e) || e.isControlDown()) {
                    resetPanState();
                    return;
                }
                panAnchorOnScreen = e.getLocationOnScreen();
                panStartViewPosition = scrollPane.getViewport().getViewPosition();
                panning = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                resetPanState();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!panning || panAnchorOnScreen == null) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                int dx = now.x - panAnchorOnScreen.x;
                int dy = now.y - panAnchorOnScreen.y;
                Point start = panStartViewPosition == null ? new Point(0, 0) : panStartViewPosition;
                Component view = scrollPane.getViewport().getView();
                if (view != null) {
                    Dimension viewSize = view.getPreferredSize();
                    Dimension extent = scrollPane.getViewport().getExtentSize();
                    int maxX = Math.max(0, viewSize.width - extent.width);
                    int maxY = Math.max(0, viewSize.height - extent.height);
                    int targetX = clamp(start.x - dx, 0, maxX);
                    int targetY = clamp(start.y - dy, 0, maxY);
                    Point current = scrollPane.getViewport().getViewPosition();
                    if (current.x != targetX || current.y != targetY) {
                        scrollPane.getViewport().setViewPosition(new Point(targetX, targetY));
                    }
                }
            }
        };
    }

    public @NotNull JComponent getComponent() {
        return root;
    }

    public @NotNull JScrollPane getScrollPane() {
        return scrollPane;
    }

    public void setBackgroundColor(@Nullable Color color) {
        backgroundColor = color;
        ApplicationManager.getApplication().invokeLater(this::applyBackgroundColor);
    }

    public void setRegionMouseListener(@NotNull MouseAdapter listener) {
        if (regionMouseListener != null) {
            removeMouseListenerRecursively(root, regionMouseListener);
        }
        regionMouseListener = listener;
        addMouseListenerRecursively(root, listener);
    }

    public void setDragPanEnabled(boolean enabled) {
        if (dragPanEnabled == enabled) {
            return;
        }
        dragPanEnabled = enabled;
        if (!enabled) {
            resetPanState();
        }
    }

    public boolean isPanning() {
        return panning;
    }

    public void setZoomPercent(int percent) {
        zoomPercent = Math.max(10, Math.min(500, percent));
    }

    public void setTextColor(@Nullable Color color) {
        textColor = color == null ? new Color(220, 220, 220) : color;
    }

    public void setRenderBatchPageCount(int pageCount) {
        pagesPerBatch = Math.max(1, pageCount);
    }

    public void setReadingPositionHandlers(
            @Nullable Function<String, Integer> loader,
            @Nullable BiConsumer<String, Integer> saver
    ) {
        readingPositionLoader = loader;
        readingPositionSaver = saver;
    }

    public void saveReadingPositionNow() {
        saveCurrentReadingPosition();
    }

    public void ensureLoaded(@Nullable String pdfPath, boolean nightModeEnabled) {
        String normalized = normalizePdfPath(pdfPath);
        if (!shouldReload(normalized, nightModeEnabled)) {
            return;
        }
        reload(pdfPath, nightModeEnabled);
    }

    public void reload(@Nullable String pdfPath, boolean nightModeEnabled) {
        int seq = loadSeq.incrementAndGet();
        cancelRestoreTimer();
        cancelWorker();
        saveCurrentReadingPosition();
        suppressAutoSave = true;
        currentPdfPath = normalizePdfPath(pdfPath);
        currentNightMode = nightModeEnabled;
        pendingRestoreScrollValue = getSavedReadingPosition(currentPdfPath);
        totalPageCount = 0;
        renderedPageCount = 0;
        loadingBatch = false;

        if (pdfPath == null) {
            showMessage("未选择 PDF，请在 Settings: XCode Tools 中选择。");
            suppressAutoSave = false;
            return;
        }

        File file = new File(pdfPath);
        if (!file.exists() || !file.isFile()) {
            showMessage("找不到 PDF 文件: " + pdfPath);
            suppressAutoSave = false;
            return;
        }

        showLoading();
        startRenderBatch(file, 0, seq, true);
    }

    private void startRenderBatch(@NotNull File file, int startPage, int seq, boolean firstBatch) {
        loadingBatch = true;
        worker = new SwingWorker<>() {
            @Override
            protected BatchRenderResult doInBackground() throws Exception {
                try (PDDocument document = Loader.loadPDF(file)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    int pageCount = document.getNumberOfPages();
                    int endPage = Math.min(pageCount, startPage + pagesPerBatch);
                    if (startPage >= endPage) {
                        return new BatchRenderResult(pageCount, startPage, firstBatch, List.of());
                    }
                    List<BufferedImage> batchImages = new ArrayList<>(Math.max(0, endPage - startPage));

                    for (int i = startPage; i < endPage; i++) {
                        if (isCancelled() || loadSeq.get() != seq) {
                            break;
                        }
                        float effectiveDpi = DPI * zoomPercent / 100.0f;
                        BufferedImage image = renderer.renderImageWithDPI(i, effectiveDpi, ImageType.RGB);
                        if (currentNightMode) {
                            image = toToneMap(
                                    image,
                                    backgroundColor == null ? DEFAULT_BACKGROUND : backgroundColor,
                                    textColor
                            );
                        }
                        batchImages.add(image);
                    }
                    int renderedUntil = startPage + batchImages.size();
                    return new BatchRenderResult(pageCount, renderedUntil, firstBatch, batchImages);
                }
            }

            @Override
            protected void done() {
                loadingBatch = false;
                worker = null;
                if (loadSeq.get() != seq) {
                    return;
                }
                try {
                    BatchRenderResult result = get();
                    if (!result.images().isEmpty()) {
                        ensureViewerShown();
                        for (BufferedImage image : result.images()) {
                            addPage(image);
                        }
                        pagesPanel.revalidate();
                        pagesPanel.repaint();
                    }
                    totalPageCount = result.totalPageCount();
                    renderedPageCount = Math.max(renderedPageCount, result.renderedUntilPageExclusive());
                    if (pagesPanel.getComponentCount() == 0 && result.firstBatch()) {
                        hasRenderedDocument = false;
                        showMessage("PDF 为空。");
                        pendingRestoreScrollValue = 0;
                        suppressAutoSave = false;
                    } else {
                        hasRenderedDocument = true;
                        renderedPdfPath = currentPdfPath;
                        renderedNightMode = currentNightMode;
                        renderedZoomPercent = zoomPercent;
                        renderedTextColor = textColor;
                        renderedBackgroundColor = backgroundColor == null ? DEFAULT_BACKGROUND : backgroundColor;
                        if (result.firstBatch()) {
                            restoreReadingPosition();
                        }
                    }
                    maybeRequestNextBatch();
                } catch (Exception e) {
                    hasRenderedDocument = false;
                    showMessage("加载 PDF 失败: " + e.getMessage());
                    suppressAutoSave = false;
                }
            }
        };

        worker.execute();
    }

    public void scrollByViewport(boolean down) {
        JScrollPane sp = scrollPane;
        int extent = sp.getViewport().getExtentSize().height;
        int delta = Math.max(1, (int) (extent * 0.9));
        int direction = down ? 1 : -1;
        int value = sp.getVerticalScrollBar().getValue() + direction * delta;
        sp.getVerticalScrollBar().setValue(value);
    }

    private int getSavedReadingPosition(@Nullable String pdfPath) {
        if (pdfPath == null || readingPositionLoader == null) {
            return 0;
        }
        Integer value = readingPositionLoader.apply(pdfPath);
        return value == null ? 0 : Math.max(0, value);
    }

    private void saveCurrentReadingPosition() {
        if (suppressAutoSave || currentPdfPath == null || readingPositionSaver == null) {
            return;
        }
        if (root.getComponentCount() == 0 || root.getComponent(0) != scrollPane || pagesPanel.getComponentCount() == 0) {
            return;
        }
        int value = Math.max(0, scrollPane.getVerticalScrollBar().getValue());
        readingPositionSaver.accept(currentPdfPath, value);
    }

    private void restoreReadingPosition() {
        int targetValue = Math.max(0, pendingRestoreScrollValue);
        pendingRestoreScrollValue = 0;
        cancelRestoreTimer();
        ApplicationManager.getApplication().invokeLater(() -> {
            final int[] retries = {0};
            restoreTimer = new Timer(80, e -> {
                var bar = scrollPane.getVerticalScrollBar();
                int maxScrollable = Math.max(0, bar.getMaximum() - bar.getVisibleAmount());
                int expected = Math.min(targetValue, maxScrollable);
                if (bar.getValue() != expected) {
                    bar.setValue(expected);
                }
                retries[0]++;
                boolean reached = Math.abs(bar.getValue() - expected) <= 1;
                boolean exhausted = retries[0] >= 10;
                if (reached || exhausted) {
                    ((Timer) e.getSource()).stop();
                    restoreTimer = null;
                    suppressAutoSave = false;
                    saveCurrentReadingPosition();
                }
            });
            restoreTimer.setInitialDelay(0);
            restoreTimer.start();
        });
    }

    private static @Nullable String normalizePdfPath(@Nullable String pdfPath) {
        if (pdfPath == null) {
            return null;
        }
        String trimmed = pdfPath.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void cancelWorker() {
        SwingWorker<BatchRenderResult, Void> w = worker;
        if (w != null) {
            w.cancel(true);
            worker = null;
        }
        loadingBatch = false;
    }

    private void cancelRestoreTimer() {
        Timer timer = restoreTimer;
        if (timer != null) {
            timer.stop();
            restoreTimer = null;
        }
    }

    private void showMessage(@NotNull String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            cancelWorker();
            hasRenderedDocument = false;
            pagesPanel.removeAll();
            root.removeAll();
            messageLabel.setText(message);
            applyBackgroundColor();
            root.add(messageLabel, BorderLayout.CENTER);
            root.revalidate();
            root.repaint();
        });
    }

    private boolean shouldReload(@Nullable String normalizedPdfPath, boolean nightModeEnabled) {
        if (!hasRenderedDocument) {
            return true;
        }
        if (!Objects.equals(renderedPdfPath, normalizedPdfPath)) {
            return true;
        }
        if (renderedNightMode != nightModeEnabled) {
            return true;
        }
        if (renderedZoomPercent != zoomPercent) {
            return true;
        }
        if (!renderedTextColor.equals(textColor)) {
            return true;
        }
        if (nightModeEnabled) {
            Color currentBg = backgroundColor == null ? DEFAULT_BACKGROUND : backgroundColor;
            return !renderedBackgroundColor.equals(currentBg);
        }
        return false;
    }

    private void maybeRequestNextBatch() {
        if (loadingBatch || worker != null) {
            return;
        }
        if (currentPdfPath == null || !hasRenderedDocument) {
            return;
        }
        if (totalPageCount <= 0 || renderedPageCount >= totalPageCount) {
            return;
        }
        JScrollPane sp = scrollPane;
        int value = sp.getVerticalScrollBar().getValue();
        int extent = sp.getViewport().getExtentSize().height;
        int max = sp.getVerticalScrollBar().getMaximum();
        if (value + extent < max - NEXT_BATCH_THRESHOLD_PX) {
            return;
        }
        int seq = loadSeq.get();
        File file = new File(currentPdfPath);
        if (!file.exists() || !file.isFile()) {
            return;
        }
        startRenderBatch(file, renderedPageCount, seq, false);
    }

    private void showLoading() {
        ApplicationManager.getApplication().invokeLater(() -> {
            pagesPanel.removeAll();
            root.removeAll();
            messageLabel.setText("正在加载 PDF...");
            applyBackgroundColor();
            root.add(messageLabel, BorderLayout.CENTER);
            root.revalidate();
            root.repaint();
        });
    }

    private void ensureViewerShown() {
        if (root.getComponentCount() == 1 && root.getComponent(0) == scrollPane) {
            return;
        }
        root.removeAll();
        applyBackgroundColor();
        root.add(scrollPane, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }

    private void installSlimScrollBars() {
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        JScrollBar hBar = scrollPane.getHorizontalScrollBar();
        vBar.setPreferredSize(new Dimension(SLIM_SCROLLBAR_THICKNESS, Integer.MAX_VALUE));
        hBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, SLIM_SCROLLBAR_THICKNESS));
        vBar.setUI(new SlimScrollBarUI());
        hBar.setUI(new SlimScrollBarUI());
        vBar.setOpaque(false);
        hBar.setOpaque(false);
    }

    private void applyBackgroundColor() {
        if (backgroundColor == null) {
            return;
        }
        root.setOpaque(true);
        root.setBackground(backgroundColor);

        messageLabel.setOpaque(true);
        messageLabel.setBackground(backgroundColor);
        messageLabel.setForeground(Color.WHITE);

        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(backgroundColor);
        pagesPanel.setOpaque(true);
        pagesPanel.setBackground(backgroundColor);
    }

    private void addPage(BufferedImage image) {
        JLabel label = new JLabel(new ImageIcon(image));
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        label.setOpaque(true);
        label.setBackground(backgroundColor == null ? Color.DARK_GRAY : backgroundColor);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        label.addMouseListener(dragPanListener);
        label.addMouseMotionListener(dragPanListener);
        if (regionMouseListener != null) {
            label.addMouseListener(regionMouseListener);
            label.addMouseMotionListener(regionMouseListener);
        }

        pagesPanel.add(label);
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(1, 12));
        spacer.addMouseListener(dragPanListener);
        spacer.addMouseMotionListener(dragPanListener);
        if (regionMouseListener != null) {
            spacer.addMouseListener(regionMouseListener);
            spacer.addMouseMotionListener(regionMouseListener);
        }
        pagesPanel.add(spacer);
    }

    private static void addMouseListenerRecursively(@NotNull Component component, @NotNull MouseAdapter listener) {
        component.addMouseListener(listener);
        component.addMouseMotionListener(listener);
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                addMouseListenerRecursively(child, listener);
            }
        }
    }

    private static void removeMouseListenerRecursively(@NotNull Component component, @NotNull MouseAdapter listener) {
        component.removeMouseListener(listener);
        component.removeMouseMotionListener(listener);
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                removeMouseListenerRecursively(child, listener);
            }
        }
    }

    private static BufferedImage toToneMap(BufferedImage input, @NotNull Color targetBackground, @NotNull Color targetText) {
        BufferedImage gray = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = gray.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        int br = targetBackground.getRed();
        int bg = targetBackground.getGreen();
        int bb = targetBackground.getBlue();
        int tr = targetText.getRed();
        int tg = targetText.getGreen();
        int tb = targetText.getBlue();

        int w = gray.getWidth();
        int h = gray.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = gray.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int gr = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int lum = (r * 30 + gr * 59 + b * 11) / 100;
                int inv = 255 - lum; // Darker source pixels should move closer to target text color.
                int outR = br + (inv * (tr - br)) / 255;
                int outG = bg + (inv * (tg - bg)) / 255;
                int outB = bb + (inv * (tb - bb)) / 255;
                int out = (outR << 16) | (outG << 8) | outB;
                gray.setRGB(x, y, out);
            }
        }
        return gray;
    }

    @Override
    public void dispose() {
        saveCurrentReadingPosition();
        resetPanState();
        cancelRestoreTimer();
        cancelWorker();
    }

    private void resetPanState() {
        panning = false;
        panAnchorOnScreen = null;
        panStartViewPosition = null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class SlimScrollBarUI extends BasicScrollBarUI {
        private static final Color THUMB = new Color(120, 120, 120, 140);
        private static final Color THUMB_HOVER = new Color(150, 150, 150, 170);

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle trackBounds) {
            // Keep track fully transparent for stealth appearance.
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(isThumbRollover() ? THUMB_HOVER : THUMB);
            g2.fillRoundRect(
                    thumbBounds.x,
                    thumbBounds.y,
                    thumbBounds.width,
                    thumbBounds.height,
                    6,
                    6
            );
            g2.dispose();
        }

        @Override
        protected javax.swing.JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected javax.swing.JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private static javax.swing.JButton createZeroButton() {
            javax.swing.JButton button = new javax.swing.JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
    }

    private record BatchRenderResult(
            int totalPageCount,
            int renderedUntilPageExclusive,
            boolean firstBatch,
            @NotNull List<BufferedImage> images
    ) {
    }
}
