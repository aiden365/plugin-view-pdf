package com.aiden.plugin.viewpdf.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;

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

    public StealthSplitPanel(@NotNull Project project) {
        disguisePanel = new DisguisePanel(project);
        projectTreePanel = new ProjectTreePanel(project, disguisePanel);
        pdfViewerPanel = new PdfViewerPanel();

        cardLayout = new CardLayout();
        rightCards = new JPanel(cardLayout);
        rightCards.add(disguisePanel.getComponent(), CARD_DISGUISE);
        rightCards.add(pdfViewerPanel.getComponent(), CARD_PDF);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectTreePanel.getComponent(), rightCards);
        splitPane.setResizeWeight(0.25);
        splitPane.setDividerSize(6);
        splitPane.setMinimumSize(new Dimension(200, 200));
        showPdf(false);
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

    public void setPdfBackgroundColor(@NotNull Color color) {
        pdfBackgroundColor = color;
        if (isPdfShown()) {
            applyPdfBackground();
        }
    }

    public void showPdf(boolean show) {
        if (show) {
            applyPdfBackground();
            cardLayout.show(rightCards, CARD_PDF);
        } else {
            cardLayout.show(rightCards, CARD_DISGUISE);
        }
    }

    private void applyPdfBackground() {
        pdfViewerPanel.setBackgroundColor(pdfBackgroundColor);
        rightCards.setOpaque(true);
        rightCards.setBackground(pdfBackgroundColor);
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
        disguisePanel.dispose();
        projectTreePanel.dispose();
        pdfViewerPanel.dispose();
    }
}
