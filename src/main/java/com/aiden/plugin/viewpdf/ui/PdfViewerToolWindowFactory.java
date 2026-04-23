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
        PdfViewerToolWindowController controller = new PdfViewerToolWindowController(splitPanel.getPdfPanel());
        controller.setPdfVisible(false);
        project.putUserData(PdfViewerKeys.CONTROLLER_KEY, controller);

        Content content = ContentFactory.getInstance().createContent(splitPanel.getComponent(), "", false);
        content.setDisposer(splitPanel);
        toolWindow.getContentManager().addContent(content);

        PdfViewerSettings settings = PdfViewerSettings.getInstance();

        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(splitPanel)
                .subscribe(PdfViewerSettingsListener.TOPIC, new PdfViewerSettingsListener() {
                    @Override
                    public void pdfPathChanged(String newPdfPath) {
                        if (controller.isPdfVisible()) {
                            controller.getPdfPanel().reload(newPdfPath, settings.isNightModeEnabled());
                        }
                    }

                    @Override
                    public void nightModeChanged(boolean enabled) {
                        if (controller.isPdfVisible()) {
                            controller.getPdfPanel().reload(settings.getPdfPath(), enabled);
                        }
                    }

                    @Override
                    public void pdfBackgroundChanged(@NotNull java.awt.Color newBackgroundColor) {
                        splitPanel.setPdfBackgroundColor(newBackgroundColor);
                    }
                });

        toolWindow.setTitleActions(List.of(new ToggleNightModeAction(), new ToggleDisguiseAction(project, splitPanel)));
    }
}
