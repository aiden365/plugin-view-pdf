package com.aiden.plugin.viewpdf.editorlookup;

import com.intellij.codeInsight.lookup.LookupManager;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Key;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class EditorWordLookupController {
    private static final Key<JBPopup> ACTIVE_POPUP_KEY = Key.create("EditorWordPopupListPopup");
    private static final int MIN_MARGIN = 8;
    private static final int POPUP_BORDER_WIDTH = 8;
    private static final Color POPUP_BORDER_COLOR = new Color(70, 70, 70);
    private static final Color POPUP_SELECTION_COLOR = new Color(47, 82, 143);
    private static final Color POPUP_SHORTCUT_COLOR = new Color(170, 170, 170);

    private enum Kind {
        WORD,
        NEXT,
        PREV,
        LEARN
    }

    private static final class Item {
        private final Kind kind;
        private final String text;
        private final String shortcut;

        private Item(@NotNull Kind kind, @NotNull String text, @NotNull String shortcut) {
            this.kind = kind;
            this.text = text;
            this.shortcut = shortcut;
        }

        private Item(@NotNull Kind kind, @NotNull String text) {
            this(kind, text, "");
        }
    }

    private EditorWordLookupController() {
    }

    public static void show(@NotNull Project project, @NotNull Editor editor) {
        if (project.isDisposed() || editor.isDisposed()) {
            return;
        }
        EditorWordLookupSession session = project.getService(EditorWordLookupSession.class);
        if (session == null) {
            return;
        }

        LookupManager.getInstance(project).hideActiveLookup();

        JBPopup existing = project.getUserData(ACTIVE_POPUP_KEY);
        if (existing != null && !existing.isDisposed()) {
            existing.cancel();
        }

        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        Color bgColor = settings.getEditorWordPopupBackgroundColor();
        Color fontColor = settings.getEditorWordPopupFontColor();
        Font editorFont = editor.getColorsScheme().getFont(EditorFontType.PLAIN);

        DefaultListModel<Item> model = new DefaultListModel<>();
        model.addElement(new Item(Kind.WORD, session.getCurrentWordDisplayText()));
        model.addElement(new Item(Kind.NEXT, "Next Method", "Alt+Shift+N"));
        model.addElement(new Item(Kind.PREV, "Prev Method", "Alt+Shift+P"));
        model.addElement(new Item(Kind.LEARN, "Learn Method", "Alt+Shift+L"));

        JList<Item> list = new JList<>(model);
        list.setOpaque(false);
        list.setBackground(new Color(0, 0, 0, 0));
        if (editorFont != null) {
            list.setFont(editorFont);
        }
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setFixedCellHeight(30);
        list.setVisibleRowCount(model.getSize());
        list.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
            int bgAlpha = toAlpha(settings.getEditorWordPopupBackgroundOpacityPercent());
            int textAlpha = toAlpha(settings.getEditorWordPopupTextOpacityPercent());
            Color background = withAlpha(bgColor, bgAlpha);
            Color selection = withAlpha(POPUP_SELECTION_COLOR, bgAlpha);
            Color effectiveBackground = isSelected ? selection : background;

            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(true);
            row.setBackground(effectiveBackground);
            row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

            com.intellij.ui.components.JBLabel left = new com.intellij.ui.components.JBLabel(value == null ? "" : value.text, SwingConstants.LEFT);
            left.setOpaque(false);
            left.setForeground(withAlpha(fontColor, textAlpha));
            if (editorFont != null) {
                left.setFont(editorFont);
            }
            row.add(left, BorderLayout.WEST);

            if (value != null && value.kind != Kind.WORD) {
                com.intellij.ui.components.JBLabel right = new com.intellij.ui.components.JBLabel(value.shortcut, SwingConstants.RIGHT);
                right.setOpaque(false);
                right.setForeground(withAlpha(POPUP_SHORTCUT_COLOR, textAlpha));
                if (editorFont != null) {
                    right.setFont(editorFont);
                }
                row.add(right, BorderLayout.EAST);
            }
            return row;
        });

        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) {
                    return;
                }
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_ESCAPE) {
                    return;
                }
                e.consume();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBackground(new Color(0, 0, 0, 0));
        root.setBorder(BorderFactory.createLineBorder(withAlpha(POPUP_BORDER_COLOR, toAlpha(settings.getEditorWordPopupBackgroundOpacityPercent()))));
        root.add(list, BorderLayout.CENTER);
        int minHeight = model.getSize() * 30;
        int width = Math.max(POPUP_BORDER_WIDTH * 2 + 120, settings.getEditorWordPopupWidth());
        int height = Math.max(minHeight, settings.getEditorWordPopupHeight());
        root.setPreferredSize(new Dimension(width, height));

        JBPopup[] holder = new JBPopup[1];
        Timer[] persistTimer = new Timer[1];
        Window[] trackedWindow = new Window[1];
        Point[] dragAnchorOnScreen = new Point[1];
        Point[] dragWindowStartOnScreen = new Point[1];

        ComponentAdapter windowTracker = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                schedulePersist();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                schedulePersist();
            }

            private void schedulePersist() {
                Window window = trackedWindow[0];
                if (window == null) {
                    return;
                }
                if (persistTimer[0] == null) {
                    persistTimer[0] = new Timer(150, ev -> persistWindowBounds(editor, window));
                    persistTimer[0].setRepeats(false);
                }
                persistTimer[0].restart();
            }
        };

        InputMap inputMap = list.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = list.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "xtools-editor-word-enter");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "xtools-editor-word-escape");
        actionMap.put("xtools-editor-word-enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Item selected = list.getSelectedValue();
                if (selected == null) {
                    return;
                }
                switch (selected.kind) {
                    case NEXT -> {
                        session.moveNext();
                        model.set(0, new Item(Kind.WORD, session.getCurrentWordDisplayText()));
                        list.repaint();
                    }
                    case PREV -> {
                        session.movePrevious();
                        model.set(0, new Item(Kind.WORD, session.getCurrentWordDisplayText()));
                        list.repaint();
                    }
                    case LEARN -> session.toggleLearn();
                    case WORD -> {
                    }
                }
            }
        });
        actionMap.put("xtools-editor-word-escape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JBPopup popup = holder[0];
                if (popup != null && !popup.isDisposed()) {
                    popup.cancel();
                }
            }
        });

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(root, list)
                .setProject(project)
                .setMovable(false)
                .setResizable(true)
                .setShowBorder(false)
                .setShowShadow(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setCancelOnWindowDeactivation(true)
                .setCancelKeyEnabled(false)
                .createPopup();
        holder[0] = popup;
        project.putUserData(ACTIVE_POPUP_KEY, popup);
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                if (persistTimer[0] != null) {
                    persistTimer[0].stop();
                }
                Window window = trackedWindow[0];
                if (window != null) {
                    persistWindowBounds(editor, window);
                    window.removeComponentListener(windowTracker);
                }
                project.putUserData(ACTIVE_POPUP_KEY, null);
            }
        });

        Point location = resolvePopupLocation(editor, root.getPreferredSize(), settings.isEditorWordPopupLocationConfigured(), settings.getEditorWordPopupX(), settings.getEditorWordPopupY());
        popup.show(new RelativePoint(editor.getContentComponent(), location));

        ApplicationManager.getApplication().invokeLater(() -> {
            JBPopup active = holder[0];
            if (active == null || active.isDisposed()) {
                return;
            }
            Window window = SwingUtilities.getWindowAncestor(root);
            if (window == null) {
                return;
            }
            trackedWindow[0] = window;
            try {
                window.setBackground(new Color(0, 0, 0, 0));
            } catch (Throwable ignored) {
            }
            window.addComponentListener(windowTracker);
        });

        MouseAdapter ctrlDragListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || !e.isControlDown()) {
                    dragAnchorOnScreen[0] = null;
                    dragWindowStartOnScreen[0] = null;
                    return;
                }
                Window window = trackedWindow[0];
                if (window == null) {
                    window = SwingUtilities.getWindowAncestor(root);
                    trackedWindow[0] = window;
                }
                if (window == null) {
                    dragAnchorOnScreen[0] = null;
                    dragWindowStartOnScreen[0] = null;
                    return;
                }
                dragAnchorOnScreen[0] = e.getLocationOnScreen();
                dragWindowStartOnScreen[0] = window.getLocationOnScreen();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragAnchorOnScreen[0] = null;
                dragWindowStartOnScreen[0] = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point anchor = dragAnchorOnScreen[0];
                Point start = dragWindowStartOnScreen[0];
                if (anchor == null || start == null) {
                    return;
                }
                Window window = trackedWindow[0];
                if (window == null) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                int dx = now.x - anchor.x;
                int dy = now.y - anchor.y;
                window.setLocation(start.x + dx, start.y + dy);
            }
        };
        root.addMouseListener(ctrlDragListener);
        root.addMouseMotionListener(ctrlDragListener);
        list.addMouseListener(ctrlDragListener);
        list.addMouseMotionListener(ctrlDragListener);
    }

    private static int toAlpha(int percent) {
        int clamped = Math.max(10, Math.min(100, percent));
        return (int) Math.round(clamped * 255.0 / 100.0);
    }

    private static @NotNull Color withAlpha(@NotNull Color color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    private static void persistWindowBounds(@NotNull Editor editor, @NotNull Window window) {
        Point local = window.getLocationOnScreen();
        SwingUtilities.convertPointFromScreen(local, editor.getContentComponent());
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        Color bg = settings.getEditorWordPopupBackgroundColor();
        Color font = settings.getEditorWordPopupFontColor();
        settings.setEditorWordPopupStyle(
                window.getWidth(),
                window.getHeight(),
                local.x,
                local.y,
                bg.getRed(),
                bg.getGreen(),
                bg.getBlue(),
                font.getRed(),
                font.getGreen(),
                font.getBlue()
        );
    }

    private static @NotNull Point resolvePopupLocation(
            @NotNull Editor editor,
            @NotNull Dimension popupSize,
            boolean useSaved,
            int savedX,
            int savedY
    ) {
        int x;
        int y;
        if (useSaved) {
            x = savedX;
            y = savedY;
        } else {
            Point caret = editor.visualPositionToXY(editor.getCaretModel().getVisualPosition());
            x = caret.x;
            y = caret.y + editor.getLineHeight();
        }

        Rectangle visible = editor.getScrollingModel().getVisibleArea();
        int maxX = visible.x + visible.width - popupSize.width - MIN_MARGIN;
        int maxY = visible.y + visible.height - popupSize.height - MIN_MARGIN;
        x = Math.max(visible.x + MIN_MARGIN, Math.min(maxX, x));
        y = Math.max(visible.y + MIN_MARGIN, Math.min(maxY, y));
        return new Point(x, y);
    }
}
