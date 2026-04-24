package com.aiden.plugin.viewpdf.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class StealthSplitPanel implements Disposable {
    private static final String CARD_DISGUISE = "disguise";
    private static final String CARD_PDF = "pdf";

    private final JSplitPane splitPane;
    private final JPanel rightCards;
    private final CardLayout cardLayout;

    private final ProjectTreePanel projectTreePanel;
    private final DisguisePanel disguisePanel;
    private final PdfViewerPanel pdfViewerPanel;

    private Color pdfBackgroundColor = new Color(43, 45, 48);
    private boolean pdfToggleEnabled;
    private int hoverSeconds = 10;
    private final Timer hoverTimer;
    private final Timer pdfLeaveWatchTimer;
    private boolean pointerInsidePdfSinceShown;
    private Runnable autoShowPdfCallback;
    private Runnable onPdfShownCallback;
    private Runnable onDisguiseShownCallback;

    public StealthSplitPanel(@NotNull Project project) {
        disguisePanel = new DisguisePanel(project);
        projectTreePanel = new ProjectTreePanel(project, disguisePanel, vf -> showDisguise());
        pdfViewerPanel = new PdfViewerPanel();

        cardLayout = new CardLayout();
        rightCards = new JPanel(cardLayout);
        rightCards.add(disguisePanel.getComponent(), CARD_DISGUISE);
        rightCards.add(pdfViewerPanel.getComponent(), CARD_PDF);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectTreePanel.getComponent(), rightCards);
        splitPane.setResizeWeight(0.25);
        splitPane.setDividerSize(6);
        splitPane.setMinimumSize(new Dimension(200, 200));
        hoverTimer = new Timer(hoverSeconds * 1000, e -> {
            if (pdfToggleEnabled && !isPdfShown()) {
                showPdf();
                if (autoShowPdfCallback != null) {
                    autoShowPdfCallback.run();
                }
            }
        });
        hoverTimer.setRepeats(false);
        pdfLeaveWatchTimer = new Timer(150, e -> {
            if (!isPdfShown()) {
                return;
            }
            boolean inside = isPointerInsideComponent(pdfViewerPanel.getComponent());
            if (inside) {
                pointerInsidePdfSinceShown = true;
                return;
            }
            // Avoid instant fallback when PDF was shown by shortcut while pointer is elsewhere.
            if (pointerInsidePdfSinceShown) {
                showDisguise();
            }
        });
        pdfLeaveWatchTimer.setRepeats(true);

        MouseAdapter hoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!pdfToggleEnabled) {
                    return;
                }
                resetHoverTimer();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!pdfToggleEnabled) {
                    return;
                }
                resetHoverTimer();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                stopHoverTimer();
            }
        };
        disguisePanel.setHoverListener(hoverListener);

        MouseAdapter pdfExitListener = new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (!isPdfShown()) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    if (isPdfShown() && !isPointerInsideComponent(pdfViewerPanel.getComponent())) {
                        showDisguise();
                    }
                });
            }
        };
        pdfViewerPanel.setRegionMouseListener(pdfExitListener);
        showDisguise();
    }

    public @NotNull JComponent getComponent() {
        return splitPane;
    }

    public @NotNull PdfViewerPanel getPdfPanel() {
        return pdfViewerPanel;
    }

    public boolean isPdfShown() {
        return CARD_PDF.equals(getVisibleCard());
    }

    public boolean isPdfToggleEnabled() {
        return pdfToggleEnabled;
    }

    public void setPdfToggleEnabled(boolean enabled) {
        pdfToggleEnabled = enabled;
        if (!pdfToggleEnabled) {
            stopHoverTimer();
        }
    }

    public void setHoverSeconds(int seconds) {
        hoverSeconds = Math.max(1, seconds);
        hoverTimer.setInitialDelay(hoverSeconds * 1000);
        hoverTimer.setDelay(hoverSeconds * 1000);
    }

    public void setAutoShowPdfCallback(@NotNull Runnable callback) {
        autoShowPdfCallback = callback;
    }

    public void setOnPdfShownCallback(@NotNull Runnable callback) {
        onPdfShownCallback = callback;
    }

    public void setOnDisguiseShownCallback(@NotNull Runnable callback) {
        onDisguiseShownCallback = callback;
    }

    public void setPdfBackgroundColor(@NotNull Color color) {
        pdfBackgroundColor = color;
        if (isPdfShown()) {
            applyPdfBackground();
        }
    }

    public void showPdf() {
        applyPdfBackground();
        pointerInsidePdfSinceShown = false;
        cardLayout.show(rightCards, CARD_PDF);
        if (!pdfLeaveWatchTimer.isRunning()) {
            pdfLeaveWatchTimer.start();
        }
        if (onPdfShownCallback != null) {
            onPdfShownCallback.run();
        }
    }

    public void showDisguise() {
        stopHoverTimer();
        pdfLeaveWatchTimer.stop();
        cardLayout.show(rightCards, CARD_DISGUISE);
        if (onDisguiseShownCallback != null) {
            onDisguiseShownCallback.run();
        }
    }

    private void applyPdfBackground() {
        pdfViewerPanel.setBackgroundColor(pdfBackgroundColor);
        rightCards.setOpaque(true);
        rightCards.setBackground(pdfBackgroundColor);
    }

    private void resetHoverTimer() {
        hoverTimer.restart();
    }

    private void stopHoverTimer() {
        hoverTimer.stop();
    }

    private static boolean isPointerInsideComponent(@NotNull JComponent component) {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            return false;
        }
        Point p = pointerInfo.getLocation();
        SwingUtilities.convertPointFromScreen(p, component);
        return p.x >= 0 && p.y >= 0 && p.x < component.getWidth() && p.y < component.getHeight();
    }

    private String getVisibleCard() {
        for (java.awt.Component comp : rightCards.getComponents()) {
            if (comp.isVisible()) {
                return comp == pdfViewerPanel.getComponent() ? CARD_PDF : CARD_DISGUISE;
            }
        }
        return CARD_DISGUISE;
    }

    @Override
    public void dispose() {
        stopHoverTimer();
        pdfLeaveWatchTimer.stop();
        disguisePanel.dispose();
        projectTreePanel.dispose();
        pdfViewerPanel.dispose();
    }
}
