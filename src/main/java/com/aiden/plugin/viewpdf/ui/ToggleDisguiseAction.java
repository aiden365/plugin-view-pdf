package com.aiden.plugin.viewpdf.ui;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class ToggleDisguiseAction extends ToggleAction {
    private final StealthSplitPanel splitPanel;

    public ToggleDisguiseAction(@NotNull Project project, @NotNull StealthSplitPanel splitPanel) {
        super("查看PDF");
        this.splitPanel = splitPanel;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return splitPanel.isPdfToggleEnabled();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        splitPanel.setPdfToggleEnabled(state);
        if (state) {
            PdfViewerSettings settings = PdfViewerSettings.getInstance();
            splitPanel.setPdfBackgroundColor(settings.getPdfBackgroundColor());
            splitPanel.getPdfPanel().ensureLoaded(settings.getPdfPath(), settings.isNightModeEnabled());
            splitPanel.showPdf();
        } else {
            splitPanel.showDisguise();
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
