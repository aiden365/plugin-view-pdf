package com.aiden.plugin.viewpdf.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.awt.Color;
import java.awt.Component;

public final class ProjectTreePanel implements Disposable {
    private static final Object PLACEHOLDER = new Object();

    private final Tree tree;
    private final DefaultTreeModel model;
    private final JBScrollPane component;
    private final Consumer<VirtualFile> onFileClicked;
    private Color backgroundColor;

    public ProjectTreePanel(@NotNull Project project, @NotNull DisguisePanel disguisePanel, @NotNull Consumer<VirtualFile> onFileClicked) {
        this.onFileClicked = onFileClicked;
        VirtualFile rootDir = guessRootDir(project);
        DefaultMutableTreeNode rootNode = rootDir == null
                ? new DefaultMutableTreeNode(" ")
                : createNode(rootDir);

        model = new DefaultTreeModel(rootNode);
        tree = new Tree(model);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(javax.swing.JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode node) {
                    Object userObject = node.getUserObject();
                    if (userObject instanceof VirtualFile vf) {
                        setText(vf.getName());
                    }
                }
                if (backgroundColor != null) {
                    setBackgroundNonSelectionColor(backgroundColor);
                    setTextNonSelectionColor(Color.WHITE);
                    setBackgroundSelectionColor(backgroundColor.darker());
                    setTextSelectionColor(Color.WHITE);
                }
                return this;
            }
        });

        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                ensureChildrenLoaded(node);
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });

        tree.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            if (path == null) {
                return;
            }
            Object last = path.getLastPathComponent();
            if (!(last instanceof DefaultMutableTreeNode node)) {
                return;
            }
            Object userObject = node.getUserObject();
            if (userObject instanceof VirtualFile vf && !vf.isDirectory()) {
                this.onFileClicked.accept(vf);
                disguisePanel.setFile(vf);
            }
        });

        component = new JBScrollPane(tree);
    }

    public @NotNull JComponent getComponent() {
        return component;
    }

    public void setBackgroundColor(@NotNull Color color) {
        backgroundColor = color;
        component.setOpaque(true);
        component.getViewport().setOpaque(true);
        component.getViewport().setBackground(color);
        tree.setOpaque(true);
        tree.setBackground(color);
        tree.repaint();
    }

    private static @Nullable VirtualFile guessRootDir(@NotNull Project project) {
        VirtualFile dir = ProjectUtil.guessProjectDir(project);
        if (dir != null) {
            return dir;
        }
        return project.getBaseDir();
    }

    private static DefaultMutableTreeNode createNode(@NotNull VirtualFile file) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(file);
        if (file.isDirectory()) {
            node.add(new DefaultMutableTreeNode(PLACEHOLDER));
        }
        return node;
    }

    private void ensureChildrenLoaded(@NotNull DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (!(userObject instanceof VirtualFile dir) || !dir.isDirectory()) {
            return;
        }
        if (node.getChildCount() == 1) {
            DefaultMutableTreeNode only = (DefaultMutableTreeNode) node.getChildAt(0);
            if (only.getUserObject() == PLACEHOLDER) {
                node.removeAllChildren();
                VirtualFile[] children = dir.getChildren();
                Arrays.sort(children, Comparator
                        .comparing(VirtualFile::isDirectory).reversed()
                        .thenComparing(VirtualFile::getName, String.CASE_INSENSITIVE_ORDER));
                for (VirtualFile child : children) {
                    node.add(createNode(child));
                }
                model.nodeStructureChanged(node);
            }
        }
    }

    @Override
    public void dispose() {
    }
}
