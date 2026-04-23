package com.aiden.plugin.viewpdf.ui;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

public final class ToggleNightModeAction extends ToggleAction {
    public ToggleNightModeAction() {
        super("夜间模式");
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return PdfViewerSettings.getInstance().isNightModeEnabled();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        PdfViewerSettings.getInstance().setNightModeEnabled(state);
    }
}
