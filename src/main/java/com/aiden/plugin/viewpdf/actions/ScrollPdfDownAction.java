package com.aiden.plugin.viewpdf.actions;

import com.aiden.plugin.viewpdf.PdfViewerKeys;
import com.aiden.plugin.viewpdf.ui.PdfViewerToolWindowController;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class ScrollPdfDownAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        PdfViewerToolWindowController controller = project.getUserData(PdfViewerKeys.CONTROLLER_KEY);
        if (controller == null || !controller.isPdfVisible()) {
            return;
        }
        controller.scrollPdfByViewport(true);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PdfViewerToolWindowController controller = project == null ? null : project.getUserData(PdfViewerKeys.CONTROLLER_KEY);
        e.getPresentation().setEnabled(controller != null && controller.isPdfVisible());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
