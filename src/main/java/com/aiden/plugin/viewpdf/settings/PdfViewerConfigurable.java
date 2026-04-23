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
import java.awt.FlowLayout;

public final class PdfViewerConfigurable implements Configurable {
    private JPanel panel;
    private TextFieldWithBrowseButton pdfPathField;
    private JSpinner bgRSpinner;
    private JSpinner bgGSpinner;
    private JSpinner bgBSpinner;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "PDF Viewer";
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
        return settings.getPdfBackgroundR() != r || settings.getPdfBackgroundG() != g || settings.getPdfBackgroundB() != b;
    }

    @Override
    public void apply() {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        settings.setPdfPath(pdfPathField.getText());
        settings.setPdfBackgroundRgb((int) bgRSpinner.getValue(), (int) bgGSpinner.getValue(), (int) bgBSpinner.getValue());
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
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        pdfPathField = null;
        bgRSpinner = null;
        bgGSpinner = null;
        bgBSpinner = null;
    }
}
