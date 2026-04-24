package com.aiden.plugin.viewpdf.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Dimension;
import java.awt.FlowLayout;

public final class PdfViewerConfigurable implements Configurable {
    private JPanel panel;
    private TextFieldWithBrowseButton pdfPathField;
    private JSpinner bgRSpinner;
    private JSpinner bgGSpinner;
    private JSpinner bgBSpinner;
    private JSpinner textRSpinner;
    private JSpinner textGSpinner;
    private JSpinner textBSpinner;
    private JSpinner hoverSecondsSpinner;
    private JSpinner zoomPercentSpinner;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "XCode Viewer";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel == null) {
            panel = new JPanel();
            panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
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
            pdfPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, pdfPathField.getPreferredSize().height));

            panel.add(pdfPathField);

            JPanel bgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bgPanel.add(new JLabel("PDF 背景色 (RGB)"));
            bgRSpinner = new JSpinner(new SpinnerNumberModel(43, 0, 255, 1));
            bgGSpinner = new JSpinner(new SpinnerNumberModel(45, 0, 255, 1));
            bgBSpinner = new JSpinner(new SpinnerNumberModel(48, 0, 255, 1));
            bgPanel.add(bgRSpinner);
            bgPanel.add(bgGSpinner);
            bgPanel.add(bgBSpinner);
            panel.add(bgPanel);

            JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            textPanel.add(new JLabel("PDF 文字颜色 (RGB)"));
            textRSpinner = new JSpinner(new SpinnerNumberModel(220, 0, 255, 1));
            textGSpinner = new JSpinner(new SpinnerNumberModel(220, 0, 255, 1));
            textBSpinner = new JSpinner(new SpinnerNumberModel(220, 0, 255, 1));
            textPanel.add(textRSpinner);
            textPanel.add(textGSpinner);
            textPanel.add(textBSpinner);
            panel.add(textPanel);

            JPanel hoverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            hoverPanel.add(new JLabel("代码区悬停自动显示 PDF（秒）"));
            hoverSecondsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
            hoverPanel.add(hoverSecondsSpinner);
            panel.add(hoverPanel);

            JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            zoomPanel.add(new JLabel("PDF 缩放（%）"));
            zoomPercentSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 500, 5));
            zoomPanel.add(zoomPercentSpinner);
            panel.add(zoomPanel);
        }
        reset();
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

        int hoverSeconds = (int) hoverSecondsSpinner.getValue();
        if (settings.getAutoShowPdfHoverSeconds() != hoverSeconds) {
            return true;
        }

        int zoomPercent = (int) zoomPercentSpinner.getValue();
        return settings.getPdfZoomPercent() != zoomPercent;
    }

    @Override
    public void apply() {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        settings.setPdfPath(pdfPathField.getText());
        settings.setPdfBackgroundRgb((int) bgRSpinner.getValue(), (int) bgGSpinner.getValue(), (int) bgBSpinner.getValue());
        settings.setPdfTextRgb((int) textRSpinner.getValue(), (int) textGSpinner.getValue(), (int) textBSpinner.getValue());
        settings.setAutoShowPdfHoverSeconds((int) hoverSecondsSpinner.getValue());
        settings.setPdfZoomPercent((int) zoomPercentSpinner.getValue());
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
        hoverSecondsSpinner.setValue(settings.getAutoShowPdfHoverSeconds());
        zoomPercentSpinner.setValue(settings.getPdfZoomPercent());
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
        hoverSecondsSpinner = null;
        zoomPercentSpinner = null;
    }
}
