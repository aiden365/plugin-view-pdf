package com.aiden.plugin.viewpdf.actions;

import com.aiden.plugin.viewpdf.bookreading.BookInlineInlayController;
import com.aiden.plugin.viewpdf.bookreading.BookReadingNotifier;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class PrevBookLineAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        if ("inline".equals(settings.getBookReadingOutputMode())) {
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor == null) {
                return;
            }
            BookInlineInlayController.getOrCreate(project).moveAndShowLine(editor, -1);
            return;
        }
        BookReadingNotifier.moveAndShowCurrentLine(project, -1);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        boolean enabled = project != null && settings.getCurrentReadingBookId() != null;
        if (enabled && "inline".equals(settings.getBookReadingOutputMode())) {
            enabled = e.getData(CommonDataKeys.EDITOR) != null;
        }
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
