package com.aiden.plugin.viewpdf.ui;

import com.aiden.plugin.viewpdf.PdfViewerKeys;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettingsListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PdfViewerToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        StealthSplitPanel splitPanel = new StealthSplitPanel(project);
        PdfViewerToolWindowController controller = new PdfViewerToolWindowController(splitPanel.getPdfPanel(), splitPanel);
        controller.setPdfVisible(false);
        project.putUserData(PdfViewerKeys.CONTROLLER_KEY, controller);

        Content content = ContentFactory.getInstance().createContent(splitPanel.getComponent(), "", false);
        content.setDisposer(splitPanel);
        toolWindow.getContentManager().addContent(content);

        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        splitPanel.setPdfBackgroundColor(settings.getPdfBackgroundColor());
        splitPanel.setTreeStyle(
                settings.getTreeBackgroundColor(),
                settings.getTreeTextColor(),
                settings.getTreeFontSize()
        );
        splitPanel.setPaneRatios(
                settings.getPaneLeftPercent(),
                settings.getPaneMiddlePercent(),
                settings.getPaneRightPercent()
        );
        splitPanel.setThirdPaneVisible(settings.isThirdPaneVisible());
        splitPanel.setHoverSeconds(settings.getAutoShowPdfHoverSeconds());
        splitPanel.getPdfPanel().setZoomPercent(settings.getPdfZoomPercent());
        splitPanel.getPdfPanel().setTextColor(settings.getPdfTextColor());
        splitPanel.getPdfPanel().setRenderBatchPageCount(settings.getRenderBatchPageCount());
        splitPanel.getPdfPanel().setReadingPositionHandlers(settings::getPdfReadPosition, settings::setPdfReadPosition);
        splitPanel.setPdfToggleEnabled(false);
        splitPanel.showDisguise();
        splitPanel.setOnPdfShownCallback(() -> controller.setPdfVisible(true));
        splitPanel.setOnDisguiseShownCallback(() -> controller.setPdfVisible(false));
        splitPanel.setAutoShowPdfCallback(() ->
                splitPanel.getPdfPanel().ensureLoaded(settings.getPdfPath(), settings.isNightModeEnabled())
        );

        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(splitPanel)
                .subscribe(PdfViewerSettingsListener.TOPIC, new PdfViewerSettingsListener() {
                    @Override
                    public void pdfPathChanged(String newPdfPath) {
                        controller.getPdfPanel().ensureLoaded(newPdfPath, settings.isNightModeEnabled());
                    }

                    @Override
                    public void nightModeChanged(boolean enabled) {
                        if (controller.isPdfVisible()) {
                            controller.getPdfPanel().ensureLoaded(settings.getPdfPath(), enabled);
                        }
                    }

                    @Override
                    public void pdfBackgroundChanged(@NotNull java.awt.Color newBackgroundColor) {
                        splitPanel.setPdfBackgroundColor(newBackgroundColor);
                    }

                    @Override
                    public void hoverSecondsChanged(int seconds) {
                        splitPanel.setHoverSeconds(seconds);
                    }

                    @Override
                    public void zoomPercentChanged(int percent) {
                        splitPanel.getPdfPanel().setZoomPercent(percent);
                        controller.getPdfPanel().ensureLoaded(settings.getPdfPath(), settings.isNightModeEnabled());
                    }

                    @Override
                    public void pdfTextColorChanged(@NotNull java.awt.Color newTextColor) {
                        splitPanel.getPdfPanel().setTextColor(newTextColor);
                        if (controller.isPdfVisible() && settings.isNightModeEnabled()) {
                            controller.getPdfPanel().ensureLoaded(settings.getPdfPath(), settings.isNightModeEnabled());
                        }
                    }

                    @Override
                    public void treeBackgroundChanged(@NotNull java.awt.Color newBackgroundColor) {
                        splitPanel.setTreeBackgroundColor(newBackgroundColor);
                    }

                    @Override
                    public void treeTextColorChanged(@NotNull java.awt.Color newTextColor) {
                        splitPanel.setTreeTextColor(newTextColor);
                    }

                    @Override
                    public void treeFontSizeChanged(int size) {
                        splitPanel.setTreeFontSize(size);
                    }

                    @Override
                    public void paneRatiosChanged(int leftPercent, int middlePercent, int rightPercent) {
                        splitPanel.setPaneRatios(leftPercent, middlePercent, rightPercent);
                    }

                    @Override
                    public void thirdPaneVisibilityChanged(boolean visible) {
                        splitPanel.setThirdPaneVisible(visible);
                    }

                    @Override
                    public void editorPopupSizeChanged(int width, int height) {
                    }

                    @Override
                    public void editorPopupBorderVisibilityChanged(boolean visible) {
                    }

                    @Override
                    public void renderBatchPageCountChanged(int pageCount) {
                        splitPanel.getPdfPanel().setRenderBatchPageCount(pageCount);
                    }
                });

        toolWindow.setTitleActions(List.of(
                new ToggleThirdPaneAction(splitPanel),
                new ToggleDisguiseAction(project, splitPanel)
        ));
    }
}
