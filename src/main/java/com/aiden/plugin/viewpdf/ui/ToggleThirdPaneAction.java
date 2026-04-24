package com.aiden.plugin.viewpdf.ui;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

public final class ToggleThirdPaneAction extends ToggleAction {
    private final StealthSplitPanel splitPanel;

    public ToggleThirdPaneAction(@NotNull StealthSplitPanel splitPanel) {
        super("右侧代码区");
        this.splitPanel = splitPanel;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return splitPanel.isThirdPaneVisible();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        PdfViewerSettings.getInstance().setThirdPaneVisible(state);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
