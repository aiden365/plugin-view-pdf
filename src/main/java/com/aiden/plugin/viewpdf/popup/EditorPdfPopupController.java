package com.aiden.plugin.viewpdf.popup;

import com.aiden.plugin.viewpdf.PdfViewerKeys;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettingsListener;
import com.aiden.plugin.viewpdf.ui.PdfViewerPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public final class EditorPdfPopupController implements Disposable {
    private static final int MIN_MARGIN = 16;
    private static final Color POPUP_BORDER_COLOR = new Color(70, 70, 70);

    private final Project project;
    private final PdfViewerPanel pdfPanel;
    private final JPanel contentPanel;
    private final Map<String, Integer> sessionReadPositions = new HashMap<>();
    private JBPopup popup;
    private Point lastScreenLocation;
    private Dimension lastSessionSize;
    private Window trackedWindow;
    private ComponentAdapter trackedWindowListener;
    private Point dragAnchorOnScreen;
    private Point dragWindowStartOnScreen;

    public static @NotNull EditorPdfPopupController getOrCreate(@NotNull Project project) {
        EditorPdfPopupController existing = project.getUserData(PdfViewerKeys.EDITOR_POPUP_CONTROLLER_KEY);
        if (existing != null) {
            return existing;
        }
        EditorPdfPopupController created = new EditorPdfPopupController(project);
        project.putUserData(PdfViewerKeys.EDITOR_POPUP_CONTROLLER_KEY, created);
        return created;
    }

    private EditorPdfPopupController(@NotNull Project project) {
        this.project = project;
        this.pdfPanel = new PdfViewerPanel();
        this.contentPanel = new JPanel(new BorderLayout());
        this.contentPanel.setOpaque(false);
        MouseAdapter ctrlDragListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isControlDown()) {
                    dragAnchorOnScreen = null;
                    dragWindowStartOnScreen = null;
                    return;
                }
                Window window = SwingUtilities.getWindowAncestor(contentPanel);
                if (window == null) {
                    dragAnchorOnScreen = null;
                    dragWindowStartOnScreen = null;
                    return;
                }
                dragAnchorOnScreen = e.getLocationOnScreen();
                dragWindowStartOnScreen = window.getLocationOnScreen();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragAnchorOnScreen = null;
                dragWindowStartOnScreen = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragAnchorOnScreen == null || dragWindowStartOnScreen == null) {
                    return;
                }
                Window window = SwingUtilities.getWindowAncestor(contentPanel);
                if (window == null) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                int dx = now.x - dragAnchorOnScreen.x;
                int dy = now.y - dragAnchorOnScreen.y;
                int targetX = dragWindowStartOnScreen.x + dx;
                int targetY = dragWindowStartOnScreen.y + dy;
                window.setLocation(targetX, targetY);
                lastScreenLocation = new Point(targetX, targetY);
            }
        };
        this.contentPanel.addMouseListener(ctrlDragListener);
        this.contentPanel.addMouseMotionListener(ctrlDragListener);
        this.pdfPanel.setRegionMouseListener(ctrlDragListener);
        this.contentPanel.add(pdfPanel.getComponent(), BorderLayout.CENTER);
        pdfPanel.setReadingPositionHandlers(
                path -> sessionReadPositions.getOrDefault(path, 0),
                sessionReadPositions::put
        );
        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(this)
                .subscribe(PdfViewerSettingsListener.TOPIC, new PdfViewerSettingsListener() {
                    @Override
                    public void pdfPathChanged(@Nullable String newPdfPath) {
                        if (isPopupActive()) {
                            pdfPanel.ensureLoaded(newPdfPath, PdfViewerSettings.getInstance().isNightModeEnabled());
                        }
                    }

                    @Override
                    public void nightModeChanged(boolean enabled) {
                        if (isPopupActive()) {
                            pdfPanel.ensureLoaded(PdfViewerSettings.getInstance().getPdfPath(), enabled);
                        }
                    }

                    @Override
                    public void pdfBackgroundChanged(@NotNull java.awt.Color newBackgroundColor) {
                        pdfPanel.setBackgroundColor(newBackgroundColor);
                        if (isPopupActive() && PdfViewerSettings.getInstance().isNightModeEnabled()) {
                            pdfPanel.ensureLoaded(PdfViewerSettings.getInstance().getPdfPath(), true);
                        }
                    }

                    @Override
                    public void hoverSecondsChanged(int seconds) {
                    }

                    @Override
                    public void zoomPercentChanged(int percent) {
                        pdfPanel.setZoomPercent(percent);
                        if (isPopupActive()) {
                            pdfPanel.ensureLoaded(PdfViewerSettings.getInstance().getPdfPath(), PdfViewerSettings.getInstance().isNightModeEnabled());
                        }
                    }

                    @Override
                    public void pdfTextColorChanged(@NotNull java.awt.Color newTextColor) {
                        pdfPanel.setTextColor(newTextColor);
                        if (isPopupActive() && PdfViewerSettings.getInstance().isNightModeEnabled()) {
                            pdfPanel.ensureLoaded(PdfViewerSettings.getInstance().getPdfPath(), true);
                        }
                    }

                    @Override
                    public void treeBackgroundChanged(@NotNull java.awt.Color newBackgroundColor) {
                    }

                    @Override
                    public void treeTextColorChanged(@NotNull java.awt.Color newTextColor) {
                    }

                    @Override
                    public void treeFontSizeChanged(int size) {
                    }

                    @Override
                    public void paneRatiosChanged(int leftPercent, int middlePercent, int rightPercent) {
                    }

                    @Override
                    public void thirdPaneVisibilityChanged(boolean visible) {
                    }

                    @Override
                    public void editorPopupSizeChanged(int width, int height) {
                        if (!isPopupActive()) {
                            return;
                        }
                        Dimension resized = new Dimension(width, height);
                        lastSessionSize = resized;
                        contentPanel.setPreferredSize(resized);
                        contentPanel.revalidate();
                    }

                    @Override
                    public void editorPopupBorderVisibilityChanged(boolean visible) {
                        applyPopupBorderVisibility(visible);
                    }

                    @Override
                    public void renderBatchPageCountChanged(int pageCount) {
                        pdfPanel.setRenderBatchPageCount(pageCount);
                    }
                });
    }

    public void showOrReuse(@NotNull Editor editor) {
        if (isPopupActive()) {
            return;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        applySharedPdfSettings(settings);

        Dimension popupSize = resolvePopupSize(settings);
        contentPanel.setPreferredSize(popupSize);
        contentPanel.setMinimumSize(new Dimension(1, 1));

        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(contentPanel, (JComponent) pdfPanel.getComponent())
                .setProject(project)
                .setMovable(false)
                .setResizable(true)
                .setShowBorder(false)
                .setShowShadow(false)
                .setCancelOnClickOutside(true)
                .setCancelOnWindowDeactivation(true)
                .setCancelKeyEnabled(true)
                .setRequestFocus(false)
                .createPopup();

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                pdfPanel.saveReadingPositionNow();
                detachWindowTracking();
                popup = null;
            }
        });

        Point target = resolvePopupLocation(editor, popupSize);
        popup.show(new RelativePoint(editor.getContentComponent(), target));
        attachWindowTracking();
        pdfPanel.ensureLoaded(settings.getPdfPath(), settings.isNightModeEnabled());
    }

    private void applySharedPdfSettings(@NotNull PdfViewerSettings settings) {
        pdfPanel.setBackgroundColor(settings.getPdfBackgroundColor());
        pdfPanel.setTextColor(settings.getPdfTextColor());
        pdfPanel.setZoomPercent(settings.getPdfZoomPercent());
        pdfPanel.setRenderBatchPageCount(settings.getRenderBatchPageCount());
        applyPopupBorderVisibility(settings.isEditorPopupBorderVisible());
    }

    private void applyPopupBorderVisibility(boolean visible) {
        if (visible) {
            contentPanel.setBorder(BorderFactory.createLineBorder(POPUP_BORDER_COLOR, 1));
        } else {
            contentPanel.setBorder(BorderFactory.createEmptyBorder());
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private @NotNull Dimension resolvePopupSize(@NotNull PdfViewerSettings settings) {
        if (lastSessionSize != null) {
            return lastSessionSize;
        }
        return new Dimension(settings.getEditorPopupWidth(), settings.getEditorPopupHeight());
    }

    private @NotNull Point resolvePopupLocation(@NotNull Editor editor, @NotNull Dimension popupSize) {
        if (lastScreenLocation != null) {
            Point p = new Point(lastScreenLocation);
            SwingUtilities.convertPointFromScreen(p, editor.getContentComponent());
            return p;
        }
        Point caretPoint = editor.visualPositionToXY(editor.getCaretModel().getVisualPosition());
        int x = caretPoint.x + MIN_MARGIN;
        int y = caretPoint.y + MIN_MARGIN;
        Component content = editor.getContentComponent();
        int maxX = Math.max(0, content.getWidth() - popupSize.width - MIN_MARGIN);
        int maxY = Math.max(0, content.getHeight() - popupSize.height - MIN_MARGIN);
        return new Point(Math.max(MIN_MARGIN, Math.min(x, maxX)), Math.max(MIN_MARGIN, Math.min(y, maxY)));
    }

    private void attachWindowTracking() {
        detachWindowTracking();
        Window window = SwingUtilities.getWindowAncestor(contentPanel);
        if (window == null) {
            return;
        }
        trackedWindow = window;
        trackedWindowListener = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                lastScreenLocation = window.getLocationOnScreen();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                Dimension size = window.getSize();
                if (size.width > 0 && size.height > 0) {
                    lastSessionSize = new Dimension(size);
                    PdfViewerSettings.getInstance().setEditorPopupSize(size.width, size.height);
                }
            }
        };
        window.addComponentListener(trackedWindowListener);
    }

    private void detachWindowTracking() {
        if (trackedWindow != null && trackedWindowListener != null) {
            trackedWindow.removeComponentListener(trackedWindowListener);
        }
        trackedWindow = null;
        trackedWindowListener = null;
    }

    private boolean isPopupActive() {
        return popup != null && !popup.isDisposed();
    }

    @Override
    public void dispose() {
        JBPopup current = popup;
        if (current != null && !current.isDisposed()) {
            current.cancel();
        }
        detachWindowTracking();
        pdfPanel.dispose();
        popup = null;
    }
}
