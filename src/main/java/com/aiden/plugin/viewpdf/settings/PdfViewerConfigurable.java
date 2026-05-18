package com.aiden.plugin.viewpdf.settings;

import com.aiden.plugin.viewpdf.stockwatcher.StockWatcherColumn;
import com.aiden.plugin.viewpdf.stockwatcher.StockWatcherSettings;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PdfViewerConfigurable implements Configurable {
    private JPanel panel;
    private TextFieldWithBrowseButton pdfPathField;
    private JComboBox<ReadingBookOption> currentReadingBookComboBox;
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
    private JSpinner popupBgRSpinner;
    private JSpinner popupBgGSpinner;
    private JSpinner popupBgBSpinner;
    private JSpinner popupTextRSpinner;
    private JSpinner popupTextGSpinner;
    private JSpinner popupTextBSpinner;
    private JSpinner editorPopupOpacitySpinner;
    private JSpinner editorPopupWidthSpinner;
    private JSpinner editorPopupHeightSpinner;
    private JSpinner wordPopupWidthSpinner;
    private JSpinner wordPopupHeightSpinner;
    private JSpinner wordPopupXSpinner;
    private JSpinner wordPopupYSpinner;
    private JSpinner wordPopupFontSizeSpinner;
    private JSpinner wordPopupFontRSpinner;
    private JSpinner wordPopupFontGSpinner;
    private JSpinner wordPopupFontBSpinner;
    private JSpinner wordPopupOpacitySpinner;
    private JSpinner editorWordPopupBackgroundOpacitySpinner;
    private JSpinner editorWordPopupTextOpacitySpinner;
    private JCheckBox wordPopupShowMeaningCheckBox;
    private JCheckBox wordPopupShowSentenceCheckBox;
    private JCheckBox wordPopupShowSynonymsCheckBox;
    private JSpinner wordPopupSentenceLimitSpinner;
    private JComboBox<VocabularyBookOption> vocabularyBookComboBox;
    private JTextField customBookNameField;
    private TextFieldWithBrowseButton customBookPathField;
    private JButton addCustomBookButton;
    private List<PdfViewerSettings.CustomVocabularyBookData> uiCustomVocabularyBooks = new ArrayList<>();
    private JTextField stockCodesField;
    private JLabel stockCodesErrorLabel;
    private MultiSelectDropdown visibleColumnsDropdown;
    private JSpinner refreshIntervalSecondsSpinner;
    private JSpinner cooldownMinutesSpinner;

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

            JPanel currentReadingBookPanel = createRowPanel();
            currentReadingBookPanel.add(new JLabel("当前阅读图书"));
            currentReadingBookComboBox = new JComboBox<>();
            currentReadingBookComboBox.setPreferredSize(new Dimension(360, currentReadingBookComboBox.getPreferredSize().height));
            currentReadingBookComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, currentReadingBookComboBox.getPreferredSize().height));
            currentReadingBookPanel.add(currentReadingBookComboBox);
            panel.add(currentReadingBookPanel);
            panel.add(Box.createVerticalStrut(4));

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

            JPanel popupBgPanel = createRowPanel();
            popupBgPanel.add(new JLabel("弹框PDF背景色 (RGB)"));
            popupBgRSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 255, 1));
            popupBgGSpinner = new JSpinner(new SpinnerNumberModel(31, 0, 255, 1));
            popupBgBSpinner = new JSpinner(new SpinnerNumberModel(34, 0, 255, 1));
            popupBgPanel.add(popupBgRSpinner);
            popupBgPanel.add(popupBgGSpinner);
            popupBgPanel.add(popupBgBSpinner);
            panel.add(popupBgPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel popupTextPanel = createRowPanel();
            popupTextPanel.add(new JLabel("弹框PDF文字颜色 (RGB)"));
            popupTextRSpinner = new JSpinner(new SpinnerNumberModel(122, 0, 255, 1));
            popupTextGSpinner = new JSpinner(new SpinnerNumberModel(126, 0, 255, 1));
            popupTextBSpinner = new JSpinner(new SpinnerNumberModel(133, 0, 255, 1));
            popupTextPanel.add(popupTextRSpinner);
            popupTextPanel.add(popupTextGSpinner);
            popupTextPanel.add(popupTextBSpinner);
            panel.add(popupTextPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel popupOpacityPanel = createRowPanel();
            popupOpacityPanel.add(new JLabel("PDF 弹框透明度（%）"));
            editorPopupOpacitySpinner = new JSpinner(new SpinnerNumberModel(100, 10, 100, 1));
            popupOpacityPanel.add(editorPopupOpacitySpinner);
            panel.add(popupOpacityPanel);
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
            panel.add(Box.createVerticalStrut(10));

            JPanel wordPopupWidthPanel = createRowPanel();
            wordPopupWidthPanel.add(new JLabel("背单词悬浮框宽度（px）"));
            wordPopupWidthSpinner = new JSpinner(new SpinnerNumberModel(360, 120, 2000, 10));
            wordPopupWidthPanel.add(wordPopupWidthSpinner);
            panel.add(wordPopupWidthPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel wordPopupHeightPanel = createRowPanel();
            wordPopupHeightPanel.add(new JLabel("背单词悬浮框高度（px）"));
            wordPopupHeightSpinner = new JSpinner(new SpinnerNumberModel(220, 120, 2000, 10));
            wordPopupHeightPanel.add(wordPopupHeightSpinner);
            panel.add(wordPopupHeightPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel wordPopupXPanel = createRowPanel();
            wordPopupXPanel.add(new JLabel("背单词悬浮框位置 X（px）"));
            wordPopupXSpinner = new JSpinner(new SpinnerNumberModel(36, -5000, 5000, 1));
            wordPopupXPanel.add(wordPopupXSpinner);
            panel.add(wordPopupXPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel wordPopupYPanel = createRowPanel();
            wordPopupYPanel.add(new JLabel("背单词悬浮框位置 Y（px）"));
            wordPopupYSpinner = new JSpinner(new SpinnerNumberModel(36, -5000, 5000, 1));
            wordPopupYPanel.add(wordPopupYSpinner);
            panel.add(wordPopupYPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel wordPopupFontSizePanel = createRowPanel();
            wordPopupFontSizePanel.add(new JLabel("背单词字体大小"));
            wordPopupFontSizeSpinner = new JSpinner(new SpinnerNumberModel(18, 8, 72, 1));
            wordPopupFontSizePanel.add(wordPopupFontSizeSpinner);
            panel.add(wordPopupFontSizePanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel wordPopupFontColorPanel = createRowPanel();
            wordPopupFontColorPanel.add(new JLabel("背单词字体颜色 (RGB)"));
            wordPopupFontRSpinner = new JSpinner(new SpinnerNumberModel(235, 0, 255, 1));
            wordPopupFontGSpinner = new JSpinner(new SpinnerNumberModel(235, 0, 255, 1));
            wordPopupFontBSpinner = new JSpinner(new SpinnerNumberModel(235, 0, 255, 1));
            wordPopupFontColorPanel.add(wordPopupFontRSpinner);
            wordPopupFontColorPanel.add(wordPopupFontGSpinner);
            wordPopupFontColorPanel.add(wordPopupFontBSpinner);
            panel.add(wordPopupFontColorPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel wordPopupOpacityPanel = createRowPanel();
            wordPopupOpacityPanel.add(new JLabel("背单词弹框透明度（%）"));
            wordPopupOpacitySpinner = new JSpinner(new SpinnerNumberModel(100, 10, 100, 1));
            wordPopupOpacityPanel.add(wordPopupOpacitySpinner);
            panel.add(wordPopupOpacityPanel);
            panel.add(Box.createVerticalStrut(10));

            JPanel editorWordPopupBackgroundOpacityPanel = createRowPanel();
            editorWordPopupBackgroundOpacityPanel.add(new JLabel("编辑器背单词弹框背景透明度（%）"));
            editorWordPopupBackgroundOpacitySpinner = new JSpinner(new SpinnerNumberModel(100, 10, 100, 1));
            editorWordPopupBackgroundOpacityPanel.add(editorWordPopupBackgroundOpacitySpinner);
            panel.add(editorWordPopupBackgroundOpacityPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel editorWordPopupTextOpacityPanel = createRowPanel();
            editorWordPopupTextOpacityPanel.add(new JLabel("编辑器背单词弹框文字透明度（%）"));
            editorWordPopupTextOpacitySpinner = new JSpinner(new SpinnerNumberModel(100, 10, 100, 1));
            editorWordPopupTextOpacityPanel.add(editorWordPopupTextOpacitySpinner);
            panel.add(editorWordPopupTextOpacityPanel);
            panel.add(Box.createVerticalStrut(10));

            JPanel showMeaningPanel = createRowPanel();
            showMeaningPanel.add(new JLabel("显示释义"));
            wordPopupShowMeaningCheckBox = new JCheckBox();
            showMeaningPanel.add(wordPopupShowMeaningCheckBox);
            panel.add(showMeaningPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel showSentencePanel = createRowPanel();
            showSentencePanel.add(new JLabel("显示例句（仅英文）"));
            wordPopupShowSentenceCheckBox = new JCheckBox();
            showSentencePanel.add(wordPopupShowSentenceCheckBox);
            panel.add(showSentencePanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel sentenceLimitPanel = createRowPanel();
            sentenceLimitPanel.add(new JLabel("例句显示条数"));
            wordPopupSentenceLimitSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
            sentenceLimitPanel.add(wordPopupSentenceLimitSpinner);
            panel.add(sentenceLimitPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel showSynonymsPanel = createRowPanel();
            showSynonymsPanel.add(new JLabel("显示同近义"));
            wordPopupShowSynonymsCheckBox = new JCheckBox();
            showSynonymsPanel.add(wordPopupShowSynonymsCheckBox);
            panel.add(showSynonymsPanel);
            panel.add(Box.createVerticalStrut(10));

            JPanel wordBuiltinBookPanel = createRowPanel();
            wordBuiltinBookPanel.add(new JLabel("词汇书列表"));
            vocabularyBookComboBox = new JComboBox<>();
            wordBuiltinBookPanel.add(vocabularyBookComboBox);
            panel.add(wordBuiltinBookPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel addBookPanel = createRowPanel();
            addBookPanel.add(new JLabel("添加词汇书"));
            customBookNameField = new JTextField();
            customBookNameField.setColumns(10);
            addBookPanel.add(customBookNameField);
            customBookPathField = new TextFieldWithBrowseButton();
            FileChooserDescriptor customBookDescriptor = new FileChooserDescriptor(
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
                    String extension = file.getExtension();
                    return extension != null && "json".equalsIgnoreCase(extension);
                }
            };
            customBookDescriptor.setTitle("选择词汇书 JSON Line 文件");
            customBookPathField.addBrowseFolderListener(new TextBrowseFolderListener(customBookDescriptor));
            customBookPathField.setPreferredSize(new Dimension(360, customBookPathField.getPreferredSize().height));
            customBookPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, customBookPathField.getPreferredSize().height));
            addBookPanel.add(customBookPathField);
            addCustomBookButton = new JButton("校验并添加");
            addCustomBookButton.addActionListener(e -> addCustomBook());
            addBookPanel.add(addCustomBookButton);
            panel.add(addBookPanel);
            panel.add(Box.createVerticalStrut(12));

            JPanel stockTitlePanel = createRowPanel();
            stockTitlePanel.add(new JLabel("股票监控"));
            panel.add(stockTitlePanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel stockCodesPanel = createRowPanel();
            stockCodesPanel.add(new JLabel("自选股票（逗号分隔）"));
            stockCodesField = new JTextField();
            stockCodesField.setPreferredSize(new Dimension(360, stockCodesField.getPreferredSize().height));
            stockCodesField.setMaximumSize(new Dimension(Integer.MAX_VALUE, stockCodesField.getPreferredSize().height));
            stockCodesPanel.add(stockCodesField);
            panel.add(stockCodesPanel);

            JPanel stockCodesErrorPanel = createRowPanel();
            stockCodesErrorLabel = new JLabel("");
            stockCodesErrorLabel.setForeground(new Color(215, 80, 80));
            stockCodesErrorPanel.add(stockCodesErrorLabel);
            panel.add(stockCodesErrorPanel);
            panel.add(Box.createVerticalStrut(4));

            stockCodesField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    updateStockCodesErrorLabelFromText();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    updateStockCodesErrorLabelFromText();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    updateStockCodesErrorLabelFromText();
                }
            });

            JPanel visibleColumnsPanel = createRowPanel();
            visibleColumnsPanel.add(new JLabel("行情列"));
            visibleColumnsDropdown = new MultiSelectDropdown(
                    StockWatcherColumn.getKeyToLabelMap(),
                    StockWatcherColumn.getDefaultVisibleKeys()
            );
            visibleColumnsPanel.add(visibleColumnsDropdown);
            panel.add(visibleColumnsPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel refreshIntervalPanel = createRowPanel();
            refreshIntervalPanel.add(new JLabel("刷新间隔（秒）"));
            refreshIntervalSecondsSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 3600, 1));
            refreshIntervalPanel.add(refreshIntervalSecondsSpinner);
            panel.add(refreshIntervalPanel);
            panel.add(Box.createVerticalStrut(4));

            JPanel cooldownPanel = createRowPanel();
            cooldownPanel.add(new JLabel("通知冷却期（分钟）"));
            cooldownMinutesSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 1440, 1));
            cooldownPanel.add(cooldownMinutesSpinner);
            panel.add(cooldownPanel);
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
        ReadingBookOption selectedReadingBook = currentReadingBookComboBox == null
                ? null
                : (ReadingBookOption) currentReadingBookComboBox.getSelectedItem();
        String selectedBookId = selectedReadingBook == null ? null : selectedReadingBook.bookId;
        if (!safeText(settings.getCurrentReadingBookId()).equals(safeText(selectedBookId))) {
            return true;
        }

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
        int pbr = (int) popupBgRSpinner.getValue();
        int pbg = (int) popupBgGSpinner.getValue();
        int pbb = (int) popupBgBSpinner.getValue();
        if (settings.getEditorPopupPdfBackgroundR() != pbr
                || settings.getEditorPopupPdfBackgroundG() != pbg
                || settings.getEditorPopupPdfBackgroundB() != pbb) {
            return true;
        }
        int ptr = (int) popupTextRSpinner.getValue();
        int ptg = (int) popupTextGSpinner.getValue();
        int ptb = (int) popupTextBSpinner.getValue();
        if (settings.getEditorPopupPdfTextR() != ptr
                || settings.getEditorPopupPdfTextG() != ptg
                || settings.getEditorPopupPdfTextB() != ptb) {
            return true;
        }
        int editorPopupOpacity = (int) editorPopupOpacitySpinner.getValue();
        if (settings.getEditorPopupOpacityPercent() != editorPopupOpacity) {
            return true;
        }
        int popupWidth = (int) editorPopupWidthSpinner.getValue();
        int popupHeight = (int) editorPopupHeightSpinner.getValue();
        if (settings.getEditorPopupWidth() != popupWidth
                || settings.getEditorPopupHeight() != popupHeight) {
            return true;
        }

        int wordPopupWidth = (int) wordPopupWidthSpinner.getValue();
        int wordPopupHeight = (int) wordPopupHeightSpinner.getValue();
        int wordPopupX = (int) wordPopupXSpinner.getValue();
        int wordPopupY = (int) wordPopupYSpinner.getValue();
        int wordPopupFontSize = (int) wordPopupFontSizeSpinner.getValue();
        int wordPopupFontR = (int) wordPopupFontRSpinner.getValue();
        int wordPopupFontG = (int) wordPopupFontGSpinner.getValue();
        int wordPopupFontB = (int) wordPopupFontBSpinner.getValue();
        if (settings.getWordPopupWidth() != wordPopupWidth
                || settings.getWordPopupHeight() != wordPopupHeight
                || settings.getWordPopupX() != wordPopupX
                || settings.getWordPopupY() != wordPopupY
                || settings.getWordPopupFontSize() != wordPopupFontSize
                || settings.getWordPopupFontR() != wordPopupFontR
                || settings.getWordPopupFontG() != wordPopupFontG
                || settings.getWordPopupFontB() != wordPopupFontB) {
            return true;
        }
        int wordPopupOpacity = (int) wordPopupOpacitySpinner.getValue();
        if (settings.getWordPopupOpacityPercent() != wordPopupOpacity) {
            return true;
        }
        int editorWordPopupBackgroundOpacity = (int) editorWordPopupBackgroundOpacitySpinner.getValue();
        if (settings.getEditorWordPopupBackgroundOpacityPercent() != editorWordPopupBackgroundOpacity) {
            return true;
        }
        int editorWordPopupTextOpacity = (int) editorWordPopupTextOpacitySpinner.getValue();
        if (settings.getEditorWordPopupTextOpacityPercent() != editorWordPopupTextOpacity) {
            return true;
        }
        if (settings.isWordPopupShowMeaning() != wordPopupShowMeaningCheckBox.isSelected()
                || settings.isWordPopupShowSentence() != wordPopupShowSentenceCheckBox.isSelected()
                || settings.isWordPopupShowSynonyms() != wordPopupShowSynonymsCheckBox.isSelected()
                || settings.getWordPopupSentenceLimit() != (int) wordPopupSentenceLimitSpinner.getValue()) {
            return true;
        }

        VocabularyBookOption selected = (VocabularyBookOption) vocabularyBookComboBox.getSelectedItem();
        String selectedBookKey = selected == null ? "" : selected.key;
        if (!settings.getSelectedVocabularyBookKey().equals(selectedBookKey)) {
            return true;
        }
        if (!sameCustomBooks(settings.getCustomVocabularyBooks(), uiCustomVocabularyBooks)) {
            return true;
        }

        StockWatcherSettings stockSettings = StockWatcherSettings.getInstance();
        String stockUiText = stockCodesField == null ? "" : stockCodesField.getText();
        String normalizedStockUiText = StockWatcherSettings.normalizeStocksInput(stockUiText).normalizedText;
        if (!stockSettings.getStocks().equals(normalizedStockUiText)) {
            return true;
        }
        List<String> uiVisibleColumns = visibleColumnsDropdown == null ? List.of() : visibleColumnsDropdown.getSelectedKeys();
        if (!stockSettings.getVisibleColumns().equals(uiVisibleColumns)) {
            return true;
        }
        int refreshIntervalSeconds = refreshIntervalSecondsSpinner == null ? 5 : (int) refreshIntervalSecondsSpinner.getValue();
        if (stockSettings.getRefreshIntervalSeconds() != refreshIntervalSeconds) {
            return true;
        }
        int cooldownMinutes = cooldownMinutesSpinner == null ? 5 : (int) cooldownMinutesSpinner.getValue();
        if (stockSettings.getCooldownMinutes() != cooldownMinutes) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        settings.setPdfPath(pdfPathField.getText());
        ReadingBookOption selectedReadingBook = (ReadingBookOption) currentReadingBookComboBox.getSelectedItem();
        settings.setCurrentReadingBookId(selectedReadingBook == null ? null : selectedReadingBook.bookId);
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
        settings.setEditorPopupPdfBackgroundRgb(
                (int) popupBgRSpinner.getValue(),
                (int) popupBgGSpinner.getValue(),
                (int) popupBgBSpinner.getValue()
        );
        settings.setEditorPopupPdfTextRgb(
                (int) popupTextRSpinner.getValue(),
                (int) popupTextGSpinner.getValue(),
                (int) popupTextBSpinner.getValue()
        );
        settings.setEditorPopupOpacityPercent((int) editorPopupOpacitySpinner.getValue());
        settings.setEditorPopupSize(
                (int) editorPopupWidthSpinner.getValue(),
                (int) editorPopupHeightSpinner.getValue()
        );
        settings.setWordPopupStyle(
                (int) wordPopupWidthSpinner.getValue(),
                (int) wordPopupHeightSpinner.getValue(),
                (int) wordPopupXSpinner.getValue(),
                (int) wordPopupYSpinner.getValue(),
                (int) wordPopupFontSizeSpinner.getValue(),
                (int) wordPopupFontRSpinner.getValue(),
                (int) wordPopupFontGSpinner.getValue(),
                (int) wordPopupFontBSpinner.getValue()
        );
        settings.setWordPopupOpacityPercent((int) wordPopupOpacitySpinner.getValue());
        settings.setEditorWordPopupBackgroundOpacityPercent((int) editorWordPopupBackgroundOpacitySpinner.getValue());
        settings.setEditorWordPopupTextOpacityPercent((int) editorWordPopupTextOpacitySpinner.getValue());
        settings.setWordPopupContentDisplay(
                wordPopupShowMeaningCheckBox.isSelected(),
                wordPopupShowSentenceCheckBox.isSelected(),
                wordPopupShowSynonymsCheckBox.isSelected(),
                (int) wordPopupSentenceLimitSpinner.getValue()
        );
        settings.setCustomVocabularyBooks(uiCustomVocabularyBooks);
        VocabularyBookOption selectedBook = (VocabularyBookOption) vocabularyBookComboBox.getSelectedItem();
        settings.setSelectedVocabularyBookKey(selectedBook == null ? null : selectedBook.key);
        WordLibraryLoader.reloadWordEntriesFromSettings(settings);

        StockWatcherSettings stockSettings = StockWatcherSettings.getInstance();
        stockSettings.setStocks(stockCodesField == null ? null : stockCodesField.getText());
        if (stockCodesField != null) {
            stockCodesField.setText(stockSettings.getStocks());
        }
        stockSettings.setVisibleColumns(visibleColumnsDropdown == null ? List.of() : visibleColumnsDropdown.getSelectedKeys());
        if (refreshIntervalSecondsSpinner != null) {
            stockSettings.setRefreshIntervalSeconds((int) refreshIntervalSecondsSpinner.getValue());
        }
        if (cooldownMinutesSpinner != null) {
            stockSettings.setCooldownMinutes((int) cooldownMinutesSpinner.getValue());
        }
        updateStockCodesErrorLabelFromSettings();
    }

    @Override
    public void reset() {
        if (pdfPathField == null) {
            return;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        String value = settings.getPdfPath();
        pdfPathField.setText(value == null ? "" : value);
        refreshReadingBookOptions(settings.getCurrentReadingBookId());

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
        popupBgRSpinner.setValue(settings.getEditorPopupPdfBackgroundR());
        popupBgGSpinner.setValue(settings.getEditorPopupPdfBackgroundG());
        popupBgBSpinner.setValue(settings.getEditorPopupPdfBackgroundB());
        popupTextRSpinner.setValue(settings.getEditorPopupPdfTextR());
        popupTextGSpinner.setValue(settings.getEditorPopupPdfTextG());
        popupTextBSpinner.setValue(settings.getEditorPopupPdfTextB());
        editorPopupOpacitySpinner.setValue(settings.getEditorPopupOpacityPercent());
        editorPopupWidthSpinner.setValue(settings.getEditorPopupWidth());
        editorPopupHeightSpinner.setValue(settings.getEditorPopupHeight());
        wordPopupWidthSpinner.setValue(settings.getWordPopupWidth());
        wordPopupHeightSpinner.setValue(settings.getWordPopupHeight());
        wordPopupXSpinner.setValue(settings.getWordPopupX());
        wordPopupYSpinner.setValue(settings.getWordPopupY());
        wordPopupFontSizeSpinner.setValue(settings.getWordPopupFontSize());
        wordPopupFontRSpinner.setValue(settings.getWordPopupFontR());
        wordPopupFontGSpinner.setValue(settings.getWordPopupFontG());
        wordPopupFontBSpinner.setValue(settings.getWordPopupFontB());
        wordPopupOpacitySpinner.setValue(settings.getWordPopupOpacityPercent());
        editorWordPopupBackgroundOpacitySpinner.setValue(settings.getEditorWordPopupBackgroundOpacityPercent());
        editorWordPopupTextOpacitySpinner.setValue(settings.getEditorWordPopupTextOpacityPercent());
        wordPopupShowMeaningCheckBox.setSelected(settings.isWordPopupShowMeaning());
        wordPopupShowSentenceCheckBox.setSelected(settings.isWordPopupShowSentence());
        wordPopupShowSynonymsCheckBox.setSelected(settings.isWordPopupShowSynonyms());
        wordPopupSentenceLimitSpinner.setValue(settings.getWordPopupSentenceLimit());
        uiCustomVocabularyBooks = cloneCustomBooks(settings.getCustomVocabularyBooks());
        refreshVocabularyBookOptions(settings.getSelectedVocabularyBookKey());
        customBookNameField.setText("");
        customBookPathField.setText("");

        StockWatcherSettings stockSettings = StockWatcherSettings.getInstance();
        if (stockCodesField != null) {
            stockCodesField.setText(stockSettings.getStocks());
        }
        if (visibleColumnsDropdown != null) {
            visibleColumnsDropdown.setSelectedKeys(stockSettings.getVisibleColumns());
        }
        if (refreshIntervalSecondsSpinner != null) {
            refreshIntervalSecondsSpinner.setValue(stockSettings.getRefreshIntervalSeconds());
        }
        if (cooldownMinutesSpinner != null) {
            cooldownMinutesSpinner.setValue(stockSettings.getCooldownMinutes());
        }
        updateStockCodesErrorLabelFromSettings();
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        pdfPathField = null;
        currentReadingBookComboBox = null;
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
        popupBgRSpinner = null;
        popupBgGSpinner = null;
        popupBgBSpinner = null;
        popupTextRSpinner = null;
        popupTextGSpinner = null;
        popupTextBSpinner = null;
        editorPopupOpacitySpinner = null;
        editorPopupWidthSpinner = null;
        editorPopupHeightSpinner = null;
        wordPopupWidthSpinner = null;
        wordPopupHeightSpinner = null;
        wordPopupXSpinner = null;
        wordPopupYSpinner = null;
        wordPopupFontSizeSpinner = null;
        wordPopupFontRSpinner = null;
        wordPopupFontGSpinner = null;
        wordPopupFontBSpinner = null;
        wordPopupOpacitySpinner = null;
        editorWordPopupBackgroundOpacitySpinner = null;
        editorWordPopupTextOpacitySpinner = null;
        wordPopupShowMeaningCheckBox = null;
        wordPopupShowSentenceCheckBox = null;
        wordPopupShowSynonymsCheckBox = null;
        wordPopupSentenceLimitSpinner = null;
        vocabularyBookComboBox = null;
        customBookNameField = null;
        customBookPathField = null;
        addCustomBookButton = null;
        uiCustomVocabularyBooks = new ArrayList<>();
        stockCodesField = null;
        stockCodesErrorLabel = null;
        visibleColumnsDropdown = null;
        refreshIntervalSecondsSpinner = null;
        cooldownMinutesSpinner = null;
    }

    private static JPanel createRowPanel() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return row;
    }

    private void addCustomBook() {
        String name = customBookNameField.getText() == null ? "" : customBookNameField.getText().trim();
        String path = customBookPathField.getText() == null ? "" : customBookPathField.getText().trim();
        if (name.isEmpty() || path.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "请输入书名和 JSON Line 文件地址", "添加词汇书", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (containsBookName(uiCustomVocabularyBooks, name)) {
            JOptionPane.showMessageDialog(panel, "书名已存在，请更换书名", "添加词汇书", JOptionPane.WARNING_MESSAGE);
            return;
        }
        WordLibraryLoader.ValidationResult result = WordLibraryLoader.validateCustomJsonl(path);
        if (!result.valid) {
            String sample = result.sampleErrorLines.isEmpty() ? "-" : result.sampleErrorLines.toString();
            String message = "校验失败，未导入。\n"
                    + "总行数: " + result.totalLines + "\n"
                    + "有效行: " + result.validLines + "\n"
                    + "错误行: " + result.invalidLines + "\n"
                    + "错误行样例: " + sample + "\n"
                    + "首个错误: " + (result.firstErrorMessage == null ? "-" : result.firstErrorMessage);
            JOptionPane.showMessageDialog(panel, message, "词汇书校验失败", JOptionPane.ERROR_MESSAGE);
            return;
        }
        PdfViewerSettings.CustomVocabularyBookData book = new PdfViewerSettings.CustomVocabularyBookData();
        book.name = name;
        book.jsonlPath = path;
        book.createdAtEpochMillis = System.currentTimeMillis();
        uiCustomVocabularyBooks.add(book);
        refreshVocabularyBookOptions("custom:" + name);
        customBookNameField.setText("");
        customBookPathField.setText("");
        JOptionPane.showMessageDialog(panel, "词汇书已添加，点击 Apply 后生效", "添加词汇书", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshVocabularyBookOptions(String selectedKey) {
        if (vocabularyBookComboBox == null) {
            return;
        }
        List<VocabularyBookOption> options = new ArrayList<>();
        options.add(new VocabularyBookOption(WordLibraryLoader.getSystemMasteredBookKey(), "系统 - 已学会"));
        for (String builtin : WordLibraryLoader.getBuiltinVocabularyBooks()) {
            options.add(new VocabularyBookOption("builtin:" + builtin, "内置 - " + builtin));
        }
        for (PdfViewerSettings.CustomVocabularyBookData book : uiCustomVocabularyBooks) {
            if (book == null || book.name == null || book.name.isBlank()) {
                continue;
            }
            options.add(new VocabularyBookOption("custom:" + book.name.trim(), "自定义 - " + book.name.trim()));
        }
        vocabularyBookComboBox.removeAllItems();
        VocabularyBookOption selected = null;
        for (VocabularyBookOption option : options) {
            vocabularyBookComboBox.addItem(option);
            if (selected == null && option.key.equals(selectedKey)) {
                selected = option;
            }
        }
        if (selected == null && vocabularyBookComboBox.getItemCount() > 0) {
            selected = vocabularyBookComboBox.getItemAt(0);
        }
        if (selected != null) {
            vocabularyBookComboBox.setSelectedItem(selected);
        }
    }

    private void refreshReadingBookOptions(@Nullable String selectedBookId) {
        if (currentReadingBookComboBox == null) {
            return;
        }
        List<ReadingBookOption> options = new ArrayList<>();
        options.add(new ReadingBookOption(null, "（不选择）"));
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        for (PdfViewerSettings.BookData book : settings.getBooks()) {
            if (book == null || book.id == null || book.name == null) {
                continue;
            }
            options.add(new ReadingBookOption(book.id, book.name));
        }
        currentReadingBookComboBox.removeAllItems();
        ReadingBookOption selected = null;
        for (ReadingBookOption option : options) {
            currentReadingBookComboBox.addItem(option);
            if (selected == null && safeText(option.bookId).equals(safeText(selectedBookId))) {
                selected = option;
            }
        }
        if (selected == null && currentReadingBookComboBox.getItemCount() > 0) {
            selected = currentReadingBookComboBox.getItemAt(0);
        }
        if (selected != null) {
            currentReadingBookComboBox.setSelectedItem(selected);
        }
    }

    private static boolean containsBookName(List<PdfViewerSettings.CustomVocabularyBookData> books, String name) {
        String target = name.trim().toLowerCase(Locale.ROOT);
        for (PdfViewerSettings.CustomVocabularyBookData book : books) {
            if (book == null || book.name == null) {
                continue;
            }
            if (book.name.trim().toLowerCase(Locale.ROOT).equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static List<PdfViewerSettings.CustomVocabularyBookData> cloneCustomBooks(List<PdfViewerSettings.CustomVocabularyBookData> books) {
        List<PdfViewerSettings.CustomVocabularyBookData> copies = new ArrayList<>();
        for (PdfViewerSettings.CustomVocabularyBookData book : books) {
            if (book == null || book.name == null || book.jsonlPath == null) {
                continue;
            }
            PdfViewerSettings.CustomVocabularyBookData copy = new PdfViewerSettings.CustomVocabularyBookData();
            copy.name = book.name;
            copy.jsonlPath = book.jsonlPath;
            copy.createdAtEpochMillis = book.createdAtEpochMillis;
            copies.add(copy);
        }
        return copies;
    }

    private static boolean sameCustomBooks(
            List<PdfViewerSettings.CustomVocabularyBookData> left,
            List<PdfViewerSettings.CustomVocabularyBookData> right
    ) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            PdfViewerSettings.CustomVocabularyBookData l = left.get(i);
            PdfViewerSettings.CustomVocabularyBookData r = right.get(i);
            if (l == null || r == null) {
                return false;
            }
            if (!safeText(l.name).equals(safeText(r.name)) || !safeText(l.jsonlPath).equals(safeText(r.jsonlPath))) {
                return false;
            }
        }
        return true;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class VocabularyBookOption {
        private final String key;
        private final String label;

        private VocabularyBookOption(String key, String label) {
            this.key = key;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ReadingBookOption {
        private final String bookId;
        private final String label;

        private ReadingBookOption(String bookId, String label) {
            this.bookId = bookId;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
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

    private void updateStockCodesErrorLabelFromSettings() {
        if (stockCodesErrorLabel == null) {
            return;
        }
        List<String> invalid = StockWatcherSettings.getInstance().getInvalidStockCodes();
        if (invalid.isEmpty()) {
            stockCodesErrorLabel.setText("");
            return;
        }
        stockCodesErrorLabel.setText("无效 code: " + String.join(", ", invalid));
    }

    private void updateStockCodesErrorLabelFromText() {
        if (stockCodesErrorLabel == null || stockCodesField == null) {
            return;
        }
        StockWatcherSettings.NormalizedStocks normalized = StockWatcherSettings.normalizeStocksInput(stockCodesField.getText());
        if (normalized.codes.isEmpty()) {
            stockCodesErrorLabel.setText("");
            return;
        }
        List<String> invalid = new ArrayList<>();
        for (String code : normalized.codes) {
            if (!StockWatcherSettings.isValidStockCode(code)) {
                invalid.add(code);
            }
        }
        if (invalid.isEmpty()) {
            stockCodesErrorLabel.setText("");
            return;
        }
        stockCodesErrorLabel.setText("无效 code: " + String.join(", ", invalid));
    }

    private static final class MultiSelectDropdown extends JPanel {
        private final JTextField displayField;
        private final JButton button;
        private final LinkedHashMap<String, String> options;
        private final LinkedHashSet<String> mandatoryKeys;
        private final LinkedHashSet<String> selectedKeys = new LinkedHashSet<>();

        private MultiSelectDropdown(@NotNull Map<String, String> keyToLabel, @NotNull List<String> mandatoryKeys) {
            this.options = new LinkedHashMap<>(keyToLabel);
            this.mandatoryKeys = new LinkedHashSet<>(mandatoryKeys);
            setLayout(new BorderLayout(4, 0));
            displayField = new JTextField();
            displayField.setEditable(false);
            displayField.setPreferredSize(new Dimension(360, displayField.getPreferredSize().height));
            displayField.setMaximumSize(new Dimension(Integer.MAX_VALUE, displayField.getPreferredSize().height));
            button = new JButton("选择");
            add(displayField, BorderLayout.CENTER);
            add(button, BorderLayout.EAST);
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    showPopup();
                }
            };
            displayField.addMouseListener(mouseAdapter);
            button.addActionListener(e -> showPopup());
            setSelectedKeys(StockWatcherColumn.getDefaultVisibleKeys());
        }

        public @NotNull List<String> getSelectedKeys() {
            ensureMandatorySelected();
            return new ArrayList<>(selectedKeys);
        }

        public void setSelectedKeys(@NotNull List<String> keys) {
            selectedKeys.clear();
            for (String key : keys) {
                if (key == null) {
                    continue;
                }
                String normalized = key.trim();
                if (normalized.isEmpty()) {
                    continue;
                }
                if (!options.containsKey(normalized)) {
                    continue;
                }
                selectedKeys.add(normalized);
            }
            ensureMandatorySelected();
            refreshDisplayText();
        }

        private void ensureMandatorySelected() {
            for (String key : mandatoryKeys) {
                if (key != null && !key.isBlank() && options.containsKey(key)) {
                    selectedKeys.add(key);
                }
            }
        }

        private void refreshDisplayText() {
            List<String> labels = new ArrayList<>();
            for (String key : selectedKeys) {
                String label = options.get(key);
                if (label != null) {
                    labels.add(label);
                }
            }
            displayField.setText(labels.isEmpty() ? "" : String.join(", ", labels));
        }

        private void showPopup() {
            JPopupMenu menu = new JPopupMenu();
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            for (Map.Entry<String, String> entry : options.entrySet()) {
                String key = entry.getKey();
                String label = entry.getValue();
                JCheckBox box = new JCheckBox(label);
                box.setSelected(selectedKeys.contains(key));
                if (mandatoryKeys.contains(key)) {
                    box.setEnabled(false);
                    box.setSelected(true);
                } else {
                    box.addActionListener(e -> {
                        if (box.isSelected()) {
                            selectedKeys.add(key);
                        } else {
                            selectedKeys.remove(key);
                        }
                        ensureMandatorySelected();
                        refreshDisplayText();
                    });
                }
                listPanel.add(box);
            }
            JScrollPane scrollPane = new JScrollPane(listPanel);
            scrollPane.setPreferredSize(new Dimension(320, 240));
            menu.setLayout(new BorderLayout());
            menu.add(scrollPane, BorderLayout.CENTER);
            menu.show(this, 0, getHeight());
        }
    }
}
