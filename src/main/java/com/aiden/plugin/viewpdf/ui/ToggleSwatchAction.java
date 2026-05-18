package com.aiden.plugin.viewpdf.ui;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

public final class ToggleSwatchAction extends ToggleAction {
    private final StealthSplitPanel splitPanel;

    public ToggleSwatchAction(@NotNull StealthSplitPanel splitPanel) {
        super("Swatch");
        this.splitPanel = splitPanel;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return splitPanel.isSwatchShown();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        if (state) {
            splitPanel.showSwatch();
        } else {
            splitPanel.hideSwatch();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
