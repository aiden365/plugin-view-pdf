package com.aiden.plugin.viewpdf.ui;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

public final class ToggleBookManagerPaneAction extends ToggleAction {
    public ToggleBookManagerPaneAction() {
        super("图书管理");
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return PdfViewerSettings.getInstance().isBookManagerPaneVisible();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        PdfViewerSettings.getInstance().setBookManagerPaneVisible(state);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}

