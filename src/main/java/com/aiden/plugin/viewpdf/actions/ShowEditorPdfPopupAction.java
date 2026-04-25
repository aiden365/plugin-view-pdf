package com.aiden.plugin.viewpdf.actions;

import com.aiden.plugin.viewpdf.popup.EditorPdfPopupController;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class ShowEditorPdfPopupAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        if (project == null || editor == null) {
            return;
        }
        EditorPdfPopupController.getOrCreate(project).showOrReuse(editor);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        e.getPresentation().setEnabled(project != null && editor != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
