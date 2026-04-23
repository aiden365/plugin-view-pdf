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
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class PdfViewerPanel implements Disposable {
    private static final float DPI = 144f;

    private final JPanel root;
    private final JBScrollPane scrollPane;
    private final JPanel pagesPanel;
    private final JLabel messageLabel;

    private Color backgroundColor;

    private final AtomicInteger loadSeq = new AtomicInteger(0);
    private SwingWorker<Void, PageUpdate> worker;

    public PdfViewerPanel() {
        root = new JPanel(new BorderLayout());

        messageLabel = new JLabel();
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        root.add(messageLabel, BorderLayout.CENTER);

        pagesPanel = new JPanel();
        pagesPanel.setLayout(new javax.swing.BoxLayout(pagesPanel, javax.swing.BoxLayout.Y_AXIS));
        scrollPane = new JBScrollPane(pagesPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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

    public void reload(@Nullable String pdfPath, boolean nightModeEnabled) {
        int seq = loadSeq.incrementAndGet();
        cancelWorker();

        if (pdfPath == null) {
            showMessage("未选择 PDF，请在 Settings: PDF Viewer 中选择。");
            return;
        }

        File file = new File(pdfPath);
        if (!file.exists() || !file.isFile()) {
            showMessage("找不到 PDF 文件: " + pdfPath);
            return;
        }

        showLoading();

        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (PDDocument document = Loader.loadPDF(file)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    int pageCount = document.getNumberOfPages();

                    for (int i = 0; i < pageCount; i++) {
                        if (isCancelled() || loadSeq.get() != seq) {
                            return null;
                        }
                        BufferedImage image = renderer.renderImageWithDPI(i, DPI, ImageType.RGB);
                        if (nightModeEnabled) {
                            image = toGrayInvert(image);
                        }
                        publish(new PageUpdate(i, image));
                    }
                }
                return null;
            }

            @Override
            protected void process(List<PageUpdate> chunks) {
                if (loadSeq.get() != seq) {
                    return;
                }
                ensureViewerShown();
                for (PageUpdate update : chunks) {
                    addPage(update.image);
                }
                pagesPanel.revalidate();
                pagesPanel.repaint();
            }

            @Override
            protected void done() {
                if (loadSeq.get() != seq) {
                    return;
                }
                try {
                    get();
                    if (pagesPanel.getComponentCount() == 0) {
                        showMessage("PDF 为空。");
                    }
                } catch (Exception e) {
                    showMessage("加载 PDF 失败: " + e.getMessage());
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

    private void cancelWorker() {
        SwingWorker<Void, PageUpdate> w = worker;
        if (w != null) {
            w.cancel(true);
            worker = null;
        }
    }

    private void showMessage(@NotNull String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            cancelWorker();
            pagesPanel.removeAll();
            root.removeAll();
            messageLabel.setText(message);
            applyBackgroundColor();
            root.add(messageLabel, BorderLayout.CENTER);
            root.revalidate();
            root.repaint();
        });
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
        label.setBackground(Color.DARK_GRAY);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));

        pagesPanel.add(label);
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(1, 12));
        pagesPanel.add(spacer);
    }

    private static BufferedImage toGrayInvert(BufferedImage input) {
        BufferedImage gray = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = gray.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        int w = gray.getWidth();
        int h = gray.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = gray.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int gr = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int lum = (r * 30 + gr * 59 + b * 11) / 100;
                int inv = 255 - lum;
                int out = (inv << 16) | (inv << 8) | inv;
                gray.setRGB(x, y, out);
            }
        }
        return gray;
    }

    @Override
    public void dispose() {
        cancelWorker();
    }

    private record PageUpdate(int pageIndex, BufferedImage image) {
    }
}
