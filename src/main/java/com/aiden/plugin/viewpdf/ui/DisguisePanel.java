package com.aiden.plugin.viewpdf.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;

public final class DisguisePanel implements Disposable {
    private final Project project;
    private final JPanel root;
    private final JLabel placeholder;

    private Editor editor;
    private MouseAdapter hoverListener;

    public DisguisePanel(@NotNull Project project) {
        this.project = project;
        root = new JPanel(new BorderLayout());
        placeholder = new JLabel(" ");
        placeholder.setHorizontalAlignment(JLabel.CENTER);
        root.add(placeholder, BorderLayout.CENTER);
    }

    public @NotNull JComponent getComponent() {
        return root;
    }

    public void setHoverListener(@NotNull MouseAdapter listener) {
        if (hoverListener != null) {
            root.removeMouseListener(hoverListener);
            root.removeMouseMotionListener(hoverListener);
            if (editor != null) {
                editor.getComponent().removeMouseListener(hoverListener);
                editor.getComponent().removeMouseMotionListener(hoverListener);
                editor.getContentComponent().removeMouseListener(hoverListener);
                editor.getContentComponent().removeMouseMotionListener(hoverListener);
            }
        }
        hoverListener = listener;
        root.addMouseListener(listener);
        root.addMouseMotionListener(listener);
        if (editor != null) {
            editor.getComponent().addMouseListener(listener);
            editor.getComponent().addMouseMotionListener(listener);
            editor.getContentComponent().addMouseListener(listener);
            editor.getContentComponent().addMouseMotionListener(listener);
        }
    }

    public void setFile(@Nullable VirtualFile file) {
        if (file == null) {
            return;
        }
        if (!"java".equalsIgnoreCase(file.getExtension())) {
            return;
        }
        Document doc = FileDocumentManager.getInstance().getDocument(file);
        if (doc == null) {
            return;
        }
        setEditor(doc, file);
    }

    private void setEditor(@NotNull Document doc, @NotNull VirtualFile file) {
        releaseEditor();
        Editor created = EditorFactory.getInstance().createViewer(doc, project);
        if (created instanceof EditorEx editorEx) {
            editorEx.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file));
        }
        editor = created;
        root.removeAll();
        root.add(created.getComponent(), BorderLayout.CENTER);
        if (hoverListener != null) {
            created.getComponent().addMouseListener(hoverListener);
            created.getComponent().addMouseMotionListener(hoverListener);
            created.getContentComponent().addMouseListener(hoverListener);
            created.getContentComponent().addMouseMotionListener(hoverListener);
        }
        root.revalidate();
        root.repaint();
    }

    private void releaseEditor() {
        if (editor != null) {
            EditorFactory.getInstance().releaseEditor(editor);
            editor = null;
        }
    }

    @Override
    public void dispose() {
        releaseEditor();
    }
}
