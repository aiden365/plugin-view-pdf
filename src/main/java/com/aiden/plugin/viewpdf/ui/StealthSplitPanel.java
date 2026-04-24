package com.aiden.plugin.viewpdf.ui;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.BorderFactory;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Graphics;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

public final class StealthSplitPanel implements Disposable {
    private static final String CARD_DISGUISE = "disguise";
    private static final String CARD_PDF = "pdf";

    private final JSplitPane outerSplitPane;
    private final JSplitPane rightSplitPane;
    private final JPanel rightCards;
    private final CardLayout cardLayout;

    private final PdfViewerSettings settings;
    private final ProjectTreePanel projectTreePanel;
    private final DisguisePanel disguisePanel;
    private final DisguisePanel fixedCodePanel;
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
    private boolean applyingPaneRatios;
    private boolean thirdPaneVisible = true;

    public StealthSplitPanel(@NotNull Project project) {
        settings = PdfViewerSettings.getInstance();
        disguisePanel = new DisguisePanel(project);
        fixedCodePanel = new DisguisePanel(project);
        projectTreePanel = new ProjectTreePanel(project, vf -> {
            showDisguise();
            disguisePanel.setFile(vf);
            fixedCodePanel.setFile(vf);
        });
        pdfViewerPanel = new PdfViewerPanel();

        cardLayout = new CardLayout();
        rightCards = new JPanel(cardLayout);
        rightCards.add(disguisePanel.getComponent(), CARD_DISGUISE);
        rightCards.add(pdfViewerPanel.getComponent(), CARD_PDF);

        rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rightCards, fixedCodePanel.getComponent());
        rightSplitPane.setResizeWeight(0.6);
        rightSplitPane.setDividerSize(2);
        rightSplitPane.setBorder(BorderFactory.createEmptyBorder());
        rightSplitPane.setMinimumSize(new Dimension(200, 200));
        installFlatDividerUi(rightSplitPane);

        outerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectTreePanel.getComponent(), rightSplitPane);
        outerSplitPane.setResizeWeight(0.25);
        outerSplitPane.setDividerSize(2);
        outerSplitPane.setBorder(BorderFactory.createEmptyBorder());
        outerSplitPane.setMinimumSize(new Dimension(200, 200));
        installFlatDividerUi(outerSplitPane);
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
                handleHoverTrigger();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                handleHoverTrigger();
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
        installPaneRatioPersistence();
        showDisguise();
    }

    public @NotNull JComponent getComponent() {
        return outerSplitPane;
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
        hoverSeconds = Math.max(-1, seconds);
        if (hoverSeconds > 0) {
            hoverTimer.setInitialDelay(hoverSeconds * 1000);
            hoverTimer.setDelay(hoverSeconds * 1000);
        } else {
            stopHoverTimer();
        }
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

    public void setTreeBackgroundColor(@NotNull Color color) {
        projectTreePanel.setBackgroundColor(color);
    }

    public void setTreeTextColor(@NotNull Color color) {
        projectTreePanel.setTextColor(color);
    }

    public void setTreeFontSize(int size) {
        projectTreePanel.setFontSize(size);
    }

    public void setTreeStyle(@NotNull Color backgroundColor, @NotNull Color textColor, int fontSize) {
        projectTreePanel.setStyle(backgroundColor, textColor, fontSize);
    }

    public void setPaneRatios(int leftPercent, int middlePercent, int rightPercent) {
        int[] ratios = normalizeRatios(leftPercent, middlePercent, rightPercent);
        applyingPaneRatios = true;
        SwingUtilities.invokeLater(() -> {
            try {
                outerSplitPane.setDividerLocation(ratios[0] / 100.0);
                int rightTotal = ratios[1] + ratios[2];
                double middleWeight = rightTotal <= 0 ? 0.5 : (double) ratios[1] / rightTotal;
                if (thirdPaneVisible) {
                    rightSplitPane.setDividerLocation(middleWeight);
                }
            } finally {
                applyingPaneRatios = false;
            }
        });
    }

    public int @NotNull [] captureCurrentPaneRatios() {
        int left = projectTreePanel.getComponent().getWidth();
        int middle = rightCards.getWidth();
        int right = fixedCodePanel.getComponent().getWidth();
        if (!thirdPaneVisible) {
            return normalizeRatios(
                    settings.getPaneLeftPercent(),
                    settings.getPaneMiddlePercent(),
                    settings.getPaneRightPercent()
            );
        }
        if (left <= 0 || middle <= 0 || right <= 0) {
            return normalizeRatios(
                    settings.getPaneLeftPercent(),
                    settings.getPaneMiddlePercent(),
                    settings.getPaneRightPercent()
            );
        }
        return normalizeRatios(left, middle, right);
    }

    public boolean isThirdPaneVisible() {
        return thirdPaneVisible;
    }

    public void setThirdPaneVisible(boolean visible) {
        thirdPaneVisible = visible;
        SwingUtilities.invokeLater(() -> {
            if (visible) {
                fixedCodePanel.getComponent().setVisible(true);
                rightSplitPane.setEnabled(true);
                rightSplitPane.setDividerSize(2);
                int[] ratios = normalizeRatios(
                        settings.getPaneLeftPercent(),
                        settings.getPaneMiddlePercent(),
                        settings.getPaneRightPercent()
                );
                int rightTotal = ratios[1] + ratios[2];
                double middleWeight = rightTotal <= 0 ? 0.5 : (double) ratios[1] / rightTotal;
                rightSplitPane.setDividerLocation(middleWeight);
            } else {
                fixedCodePanel.getComponent().setVisible(false);
                rightSplitPane.setDividerSize(0);
                rightSplitPane.setEnabled(false);
                rightSplitPane.setDividerLocation(1.0);
            }
            rightSplitPane.revalidate();
            rightSplitPane.repaint();
        });
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
        pdfViewerPanel.saveReadingPositionNow();
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

    private void installPaneRatioPersistence() {
        PropertyChangeListener listener = evt -> {
            if (applyingPaneRatios) {
                return;
            }
            int[] ratios = captureCurrentPaneRatios();
            settings.setPaneRatios(ratios[0], ratios[1], ratios[2]);
        };
        outerSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, listener);
        rightSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, listener);
    }

    private static int @NotNull [] normalizeRatios(int leftPercent, int middlePercent, int rightPercent) {
        int l = Math.max(5, Math.min(90, leftPercent));
        int m = Math.max(5, Math.min(90, middlePercent));
        int r = Math.max(5, Math.min(90, rightPercent));
        int sum = l + m + r;
        if (sum <= 0) {
            return new int[]{25, 45, 30};
        }
        int nl = Math.max(5, Math.min(90, (int) Math.round(l * 100.0 / sum)));
        int nm = Math.max(5, Math.min(90, (int) Math.round(m * 100.0 / sum)));
        int nr = Math.max(5, Math.min(90, 100 - nl - nm));

        if (nr < 5) {
            nr = 5;
            int remaining = 95;
            int lmSum = nl + nm;
            if (lmSum > 0) {
                nl = Math.max(5, (int) Math.round(nl * (remaining / (double) lmSum)));
                nm = Math.max(5, remaining - nl);
            } else {
                nl = 50;
                nm = 45;
            }
        }
        int fixedSum = nl + nm + nr;
        if (fixedSum != 100) {
            int delta = 100 - fixedSum;
            nr = Math.max(5, Math.min(90, nr + delta));
        }
        return Arrays.stream(new int[]{nl, nm, nr}).map(v -> Math.max(5, Math.min(90, v))).toArray();
    }

    private void installFlatDividerUi(@NotNull JSplitPane splitPane) {
        Color dividerColor = new Color(43, 45, 48);
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public @NotNull BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(dividerColor);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });
        splitPane.setOpaque(false);
        BasicSplitPaneUI ui = (BasicSplitPaneUI) splitPane.getUI();
        if (ui == null) {
            return;
        }
        BasicSplitPaneDivider divider = ui.getDivider();
        if (divider != null) {
            divider.setBorder(BorderFactory.createEmptyBorder());
            divider.setBackground(dividerColor);
        }
    }

    private void resetHoverTimer() {
        hoverTimer.restart();
    }

    private void handleHoverTrigger() {
        if (!pdfToggleEnabled) {
            return;
        }
        if (hoverSeconds < 0) {
            return;
        }
        if (hoverSeconds == 0) {
            if (!isPdfShown()) {
                showPdf();
                if (autoShowPdfCallback != null) {
                    autoShowPdfCallback.run();
                }
            }
            return;
        }
        resetHoverTimer();
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
        fixedCodePanel.dispose();
        projectTreePanel.dispose();
        pdfViewerPanel.dispose();
    }
}
