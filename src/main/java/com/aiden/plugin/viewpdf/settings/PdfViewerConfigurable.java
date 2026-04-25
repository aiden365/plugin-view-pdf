package com.aiden.plugin.viewpdf.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public final class PdfViewerConfigurable implements Configurable {
    private JPanel panel;
    private TextFieldWithBrowseButton pdfPathField;
    private JSpinner bgRSpinner;
    private JSpinner bgGSpinner;
    private JSpinner bgBSpinner;
    private JSpinner textRSpinner;
    private JSpinner textGSpinner;
    private JSpinner textBSpinner;
    private JSpinner treeBgRSpinner;
    private JSpinner treeBgGSpinner;
    private JSpinner treeBgBSpinner;
    private JSpinner treeTextRSpinner;
    private JSpinner treeTextGSpinner;
    private JSpinner treeTextBSpinner;
    private JSpinner treeFontSizeSpinner;
    private JCheckBox nightModeCheckBox;
    private JSpinner hoverSecondsSpinner;
    private JSpinner zoomPercentSpinner;
    private JSpinner paneLeftSpinner;
    private JSpinner paneMiddleSpinner;
    private JSpinner paneRightSpinner;
    private JSpinner renderBatchPageCountSpinner;
    private JCheckBox popupBorderVisibleCheckBox;
    private JSpinner editorPopupWidthSpinner;
    private JSpinner editorPopupHeightSpinner;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "XCode Tools";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel == null) {
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            pdfPathField = new TextFieldWithBrowseButton();

            FileChooserDescriptor descriptor = new FileChooserDescriptor(
                    true,
                    false,
                    false,
                    false,
                    false,
                    false
            ) {
                @Override
                public boolean isFileSelectable(VirtualFile file) {
                    if (!super.isFileSelectable(file)) {
                        return false;
                    }
                    return "pdf".equalsIgnoreCase(file.getExtension());
                }
            };
            descriptor.setTitle("选择 PDF 文件");
            pdfPathField.addBrowseFolderListener(new TextBrowseFolderListener(descriptor));
            pdfPathField.setPreferredSize(new Dimension(520, pdfPathField.getPreferredSize().height));
            pdfPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, pdfPathField.getPreferredSize().height));

            JPanel pathPanel = createRowPanel();
            pathPanel.add(new JLabel("PDF 文件"));
            pathPanel.add(pdfPathField);
            panel.add(pathPanel);
            panel.add(Box.createVerticalStrut(4));
            panel.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updatePdfPathFieldWidth();
                }
            });

            JPanel bgPanel = createRowPanel();
            bgPanel.add(new JLabel("PDF 背景色 (RGB)"));
            bgRSpinner = new JSpinner(new SpinnerNumberModel(43, 0, 255, 1));
            bgGSpinner = new JSpinner(new SpinnerNumberModel(45, 0, 255, 1));
            bgBSpinner = new JSpinner(new SpinnerNumberModel(48, 0, 255, 1));
            bgPanel.add(bgRSpinner);
            bgPanel.add(bgGSpinner);
            bgPanel.add(bgBSpinner);
            panel.add(bgPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel textPanel = createRowPanel();
            textPanel.add(new JLabel("PDF 文字颜色 (RGB)"));
            textRSpinner = new JSpinner(new SpinnerNumberModel(220, 0, 255, 1));
            textGSpinner = new JSpinner(new SpinnerNumberModel(220, 0, 255, 1));
            textBSpinner = new JSpinner(new SpinnerNumberModel(220, 0, 255, 1));
            textPanel.add(textRSpinner);
            textPanel.add(textGSpinner);
            textPanel.add(textBSpinner);
            panel.add(textPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel treeBgPanel = createRowPanel();
            treeBgPanel.add(new JLabel("目录区背景色 (RGB)"));
            treeBgRSpinner = new JSpinner(new SpinnerNumberModel(43, 0, 255, 1));
            treeBgGSpinner = new JSpinner(new SpinnerNumberModel(45, 0, 255, 1));
            treeBgBSpinner = new JSpinner(new SpinnerNumberModel(48, 0, 255, 1));
            treeBgPanel.add(treeBgRSpinner);
            treeBgPanel.add(treeBgGSpinner);
            treeBgPanel.add(treeBgBSpinner);
            panel.add(treeBgPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel treeTextPanel = createRowPanel();
            treeTextPanel.add(new JLabel("目录区文字颜色 (RGB)"));
            treeTextRSpinner = new JSpinner(new SpinnerNumberModel(220, 0, 255, 1));
            treeTextGSpinner = new JSpinner(new SpinnerNumberModel(220, 0, 255, 1));
            treeTextBSpinner = new JSpinner(new SpinnerNumberModel(220, 0, 255, 1));
            treeTextPanel.add(treeTextRSpinner);
            treeTextPanel.add(treeTextGSpinner);
            treeTextPanel.add(treeTextBSpinner);
            panel.add(treeTextPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel treeFontPanel = createRowPanel();
            treeFontPanel.add(new JLabel("目录区字体大小"));
            treeFontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 32, 1));
            treeFontPanel.add(treeFontSizeSpinner);
            panel.add(treeFontPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel nightModePanel = createRowPanel();
            nightModePanel.add(new JLabel("启用夜间模式"));
            nightModeCheckBox = new JCheckBox();
            nightModePanel.add(nightModeCheckBox);
            panel.add(nightModePanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel hoverPanel = createRowPanel();
            hoverPanel.add(new JLabel("代码区悬停自动显示 PDF（秒，-1禁用，0立即）"));
            hoverSecondsSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 3600, 1));
            hoverPanel.add(hoverSecondsSpinner);
            panel.add(hoverPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel zoomPanel = createRowPanel();
            zoomPanel.add(new JLabel("PDF 缩放（%）"));
            zoomPercentSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 500, 5));
            zoomPanel.add(zoomPercentSpinner);
            panel.add(zoomPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel paneLeftPanel = createRowPanel();
            paneLeftPanel.add(new JLabel("左侧宽度（%）"));
            paneLeftSpinner = new JSpinner(new SpinnerNumberModel(25, 5, 90, 1));
            paneLeftPanel.add(paneLeftSpinner);
            panel.add(paneLeftPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel paneMiddlePanel = createRowPanel();
            paneMiddlePanel.add(new JLabel("中间宽度（%）"));
            paneMiddleSpinner = new JSpinner(new SpinnerNumberModel(45, 5, 90, 1));
            paneMiddlePanel.add(paneMiddleSpinner);
            panel.add(paneMiddlePanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel paneRightPanel = createRowPanel();
            paneRightPanel.add(new JLabel("右侧宽度（%）"));
            paneRightSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 90, 1));
            paneRightPanel.add(paneRightSpinner);
            panel.add(paneRightPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel batchPageCountPanel = createRowPanel();
            batchPageCountPanel.add(new JLabel("每批渲染页数"));
            renderBatchPageCountSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 5000, 1));
            batchPageCountPanel.add(renderBatchPageCountSpinner);
            panel.add(batchPageCountPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel popupBorderPanel = createRowPanel();
            popupBorderPanel.add(new JLabel("显示弹框边框"));
            popupBorderVisibleCheckBox = new JCheckBox();
            popupBorderPanel.add(popupBorderVisibleCheckBox);
            panel.add(popupBorderPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel popupWidthPanel = createRowPanel();
            popupWidthPanel.add(new JLabel("悬浮窗默认宽度（px）"));
            editorPopupWidthSpinner = new JSpinner(new SpinnerNumberModel(760, 1, 2000, 10));
            popupWidthPanel.add(editorPopupWidthSpinner);
            panel.add(popupWidthPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel popupHeightPanel = createRowPanel();
            popupHeightPanel.add(new JLabel("悬浮窗默认高度（px）"));
            editorPopupHeightSpinner = new JSpinner(new SpinnerNumberModel(520, 1, 2000, 10));
            popupHeightPanel.add(editorPopupHeightSpinner);
            panel.add(popupHeightPanel);
        }
        reset();
        updatePdfPathFieldWidth();
        return panel;
    }

    @Override
    public boolean isModified() {
        String uiValue = pdfPathField == null ? "" : pdfPathField.getText();
        String current = PdfViewerSettings.getInstance().getPdfPath();
        String currentValue = current == null ? "" : current;
        if (!currentValue.equals(uiValue == null ? "" : uiValue)) {
            return true;
        }

        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        int r = (int) bgRSpinner.getValue();
        int g = (int) bgGSpinner.getValue();
        int b = (int) bgBSpinner.getValue();
        if (settings.getPdfBackgroundR() != r || settings.getPdfBackgroundG() != g || settings.getPdfBackgroundB() != b) {
            return true;
        }
        int tr = (int) textRSpinner.getValue();
        int tg = (int) textGSpinner.getValue();
        int tb = (int) textBSpinner.getValue();
        if (settings.getPdfTextR() != tr || settings.getPdfTextG() != tg || settings.getPdfTextB() != tb) {
            return true;
        }
        int tbr = (int) treeBgRSpinner.getValue();
        int tbg = (int) treeBgGSpinner.getValue();
        int tbb = (int) treeBgBSpinner.getValue();
        if (settings.getTreeBackgroundR() != tbr || settings.getTreeBackgroundG() != tbg || settings.getTreeBackgroundB() != tbb) {
            return true;
        }
        int ttr = (int) treeTextRSpinner.getValue();
        int ttg = (int) treeTextGSpinner.getValue();
        int ttb = (int) treeTextBSpinner.getValue();
        if (settings.getTreeTextR() != ttr || settings.getTreeTextG() != ttg || settings.getTreeTextB() != ttb) {
            return true;
        }
        int treeFontSize = (int) treeFontSizeSpinner.getValue();
        if (settings.getTreeFontSize() != treeFontSize) {
            return true;
        }
        if (settings.isNightModeEnabled() != nightModeCheckBox.isSelected()) {
            return true;
        }

        int hoverSeconds = (int) hoverSecondsSpinner.getValue();
        if (settings.getAutoShowPdfHoverSeconds() != hoverSeconds) {
            return true;
        }


        int zoomPercent = (int) zoomPercentSpinner.getValue();
        if (settings.getPdfZoomPercent() != zoomPercent) {
            return true;
        }

        int paneLeft = (int) paneLeftSpinner.getValue();
        int paneMiddle = (int) paneMiddleSpinner.getValue();
        int paneRight = (int) paneRightSpinner.getValue();
        if (settings.getPaneLeftPercent() != paneLeft
                || settings.getPaneMiddlePercent() != paneMiddle
                || settings.getPaneRightPercent() != paneRight) {
            return true;
        }
        int renderBatchPageCount = (int) renderBatchPageCountSpinner.getValue();
        if (settings.getRenderBatchPageCount() != renderBatchPageCount) {
            return true;
        }
        if (settings.isEditorPopupBorderVisible() != popupBorderVisibleCheckBox.isSelected()) {
            return true;
        }
        int popupWidth = (int) editorPopupWidthSpinner.getValue();
        int popupHeight = (int) editorPopupHeightSpinner.getValue();
        return settings.getEditorPopupWidth() != popupWidth
                || settings.getEditorPopupHeight() != popupHeight;
    }

    @Override
    public void apply() {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        settings.setPdfPath(pdfPathField.getText());
        settings.setPdfBackgroundRgb((int) bgRSpinner.getValue(), (int) bgGSpinner.getValue(), (int) bgBSpinner.getValue());
        settings.setPdfTextRgb((int) textRSpinner.getValue(), (int) textGSpinner.getValue(), (int) textBSpinner.getValue());
        settings.setTreeBackgroundRgb((int) treeBgRSpinner.getValue(), (int) treeBgGSpinner.getValue(), (int) treeBgBSpinner.getValue());
        settings.setTreeTextRgb((int) treeTextRSpinner.getValue(), (int) treeTextGSpinner.getValue(), (int) treeTextBSpinner.getValue());
        settings.setTreeFontSize((int) treeFontSizeSpinner.getValue());
        settings.setNightModeEnabled(nightModeCheckBox.isSelected());
        settings.setAutoShowPdfHoverSeconds((int) hoverSecondsSpinner.getValue());
        settings.setPdfZoomPercent((int) zoomPercentSpinner.getValue());
        settings.setPaneRatios(
                (int) paneLeftSpinner.getValue(),
                (int) paneMiddleSpinner.getValue(),
                (int) paneRightSpinner.getValue()
        );
        settings.setRenderBatchPageCount((int) renderBatchPageCountSpinner.getValue());
        settings.setEditorPopupBorderVisible(popupBorderVisibleCheckBox.isSelected());
        settings.setEditorPopupSize(
                (int) editorPopupWidthSpinner.getValue(),
                (int) editorPopupHeightSpinner.getValue()
        );
    }

    @Override
    public void reset() {
        if (pdfPathField == null) {
            return;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        String value = settings.getPdfPath();
        pdfPathField.setText(value == null ? "" : value);

        bgRSpinner.setValue(settings.getPdfBackgroundR());
        bgGSpinner.setValue(settings.getPdfBackgroundG());
        bgBSpinner.setValue(settings.getPdfBackgroundB());
        textRSpinner.setValue(settings.getPdfTextR());
        textGSpinner.setValue(settings.getPdfTextG());
        textBSpinner.setValue(settings.getPdfTextB());
        treeBgRSpinner.setValue(settings.getTreeBackgroundR());
        treeBgGSpinner.setValue(settings.getTreeBackgroundG());
        treeBgBSpinner.setValue(settings.getTreeBackgroundB());
        treeTextRSpinner.setValue(settings.getTreeTextR());
        treeTextGSpinner.setValue(settings.getTreeTextG());
        treeTextBSpinner.setValue(settings.getTreeTextB());
        treeFontSizeSpinner.setValue(settings.getTreeFontSize());
        nightModeCheckBox.setSelected(settings.isNightModeEnabled());
        hoverSecondsSpinner.setValue(settings.getAutoShowPdfHoverSeconds());
        zoomPercentSpinner.setValue(settings.getPdfZoomPercent());
        paneLeftSpinner.setValue(settings.getPaneLeftPercent());
        paneMiddleSpinner.setValue(settings.getPaneMiddlePercent());
        paneRightSpinner.setValue(settings.getPaneRightPercent());
        renderBatchPageCountSpinner.setValue(settings.getRenderBatchPageCount());
        popupBorderVisibleCheckBox.setSelected(settings.isEditorPopupBorderVisible());
        editorPopupWidthSpinner.setValue(settings.getEditorPopupWidth());
        editorPopupHeightSpinner.setValue(settings.getEditorPopupHeight());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        pdfPathField = null;
        bgRSpinner = null;
        bgGSpinner = null;
        bgBSpinner = null;
        textRSpinner = null;
        textGSpinner = null;
        textBSpinner = null;
        treeBgRSpinner = null;
        treeBgGSpinner = null;
        treeBgBSpinner = null;
        treeTextRSpinner = null;
        treeTextGSpinner = null;
        treeTextBSpinner = null;
        treeFontSizeSpinner = null;
        nightModeCheckBox = null;
        hoverSecondsSpinner = null;
        zoomPercentSpinner = null;
        paneLeftSpinner = null;
        paneMiddleSpinner = null;
        paneRightSpinner = null;
        renderBatchPageCountSpinner = null;
        popupBorderVisibleCheckBox = null;
        editorPopupWidthSpinner = null;
        editorPopupHeightSpinner = null;
    }

    private static JPanel createRowPanel() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return row;
    }

    private void updatePdfPathFieldWidth() {
        if (panel == null || pdfPathField == null) {
            return;
        }
        int totalWidth = panel.getWidth();
        if (totalWidth <= 0) {
            return;
        }
        int targetWidth = Math.max(360, (int) (totalWidth * 0.7));
        int height = pdfPathField.getPreferredSize().height;
        Dimension size = new Dimension(targetWidth, height);
        pdfPathField.setPreferredSize(size);
        pdfPathField.setMinimumSize(size);
        pdfPathField.revalidate();
    }
}
