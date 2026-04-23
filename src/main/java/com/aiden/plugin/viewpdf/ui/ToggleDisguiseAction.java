package com.aiden.plugin.viewpdf.ui;

import com.aiden.plugin.viewpdf.PdfViewerKeys;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class ToggleDisguiseAction extends ToggleAction {
    private final Project project;
    private final StealthSplitPanel splitPanel;

    public ToggleDisguiseAction(@NotNull Project project, @NotNull StealthSplitPanel splitPanel) {
        super("查看PDF");
        this.project = project;
        this.splitPanel = splitPanel;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        PdfViewerToolWindowController controller = project.getUserData(PdfViewerKeys.CONTROLLER_KEY);
        return controller != null && controller.isPdfVisible();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        PdfViewerToolWindowController controller = project.getUserData(PdfViewerKeys.CONTROLLER_KEY);
        if (controller == null) {
            return;
        }
        controller.setPdfVisible(state);
        if (state) {
            PdfViewerSettings settings = PdfViewerSettings.getInstance();
            splitPanel.setPdfBackgroundColor(settings.getPdfBackgroundColor());
            splitPanel.showPdf(true);
            controller.getPdfPanel().reload(settings.getPdfPath(), settings.isNightModeEnabled());
        } else {
            splitPanel.showPdf(false);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        boolean selected = isSelected(e);
        e.getPresentation().setText(selected ? "取消伪装" : "查看PDF");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
