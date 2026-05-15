package com.aiden.plugin.viewpdf.popup;

import com.aiden.plugin.viewpdf.PdfViewerKeys;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.aiden.plugin.viewpdf.settings.PdfViewerSettingsListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import com.intellij.icons.AllIcons;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class WordPopupController implements Disposable {
    private static final Color POPUP_BACKGROUND = new Color(35, 37, 40);
    private static final int MENU_PANEL_WIDTH = 44;

    private final Project project;
    private final JPanel rootPanel;
    private final JPanel contentPanel;
    private final JLabel wordLabel;
    private final JLabel phoneticLabel;
    private final JLabel meaningLabel;
    private final JLabel sentenceLabel;
    private final JLabel synonymsLabel;
    private final JLabel hintLabel;
    private final JPanel menuPanel;
    private final JPanel menuGrid;
    private final JLabel prevWordLabel;
    private final JLabel nextWordLabel;
    private final JLabel translateToggleLabel;
    private final JLabel masteredToggleLabel;

    private JBPopup popup;
    private Editor lastEditor;
    private List<PdfViewerSettings.WordEntryData> activeWords = List.of();
    private int currentIndex = -1;
    private Window trackedWindow;
    private ComponentAdapter trackedWindowListener;
    private Point dragAnchorOnScreen;
    private Point dragWindowStartOnScreen;
    private boolean adjustingBounds;
    private boolean hasPendingBoundsPersist;
    private int pendingWidth;
    private int pendingHeight;
    private int pendingX;
    private int pendingY;
    private Timer boundsPersistTimer;
    private Boolean meaningVisibleOverride;

    public static @NotNull WordPopupController getOrCreate(@NotNull Project project) {
        WordPopupController existing = project.getUserData(PdfViewerKeys.WORD_POPUP_CONTROLLER_KEY);
        if (existing != null) {
            return existing;
        }
        WordPopupController created = new WordPopupController(project);
        project.putUserData(PdfViewerKeys.WORD_POPUP_CONTROLLER_KEY, created);
        Disposer.register(project, created);
        return created;
    }

    private WordPopupController(@NotNull Project project) {
        this.project = project;
        this.rootPanel = new JPanel();
        this.rootPanel.setLayout(new BorderLayout());
        this.rootPanel.setOpaque(true);
        this.rootPanel.setBackground(POPUP_BACKGROUND);
        this.rootPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        this.contentPanel = new JPanel();
        this.contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        this.contentPanel.setOpaque(false);
        this.contentPanel.setBorder(BorderFactory.createEmptyBorder());

        this.wordLabel = new JLabel("", SwingConstants.LEFT);
        this.phoneticLabel = new JLabel("", SwingConstants.LEFT);
        this.meaningLabel = new JLabel("", SwingConstants.LEFT);
        this.sentenceLabel = new JLabel("", SwingConstants.LEFT);
        this.synonymsLabel = new JLabel("", SwingConstants.LEFT);
        this.hintLabel = new JLabel("", SwingConstants.LEFT);
        this.prevWordLabel = new JLabel(AllIcons.Actions.Back, SwingConstants.CENTER);
        this.nextWordLabel = new JLabel(AllIcons.Actions.Forward, SwingConstants.CENTER);
        this.translateToggleLabel = new JLabel("中", SwingConstants.CENTER);
        this.masteredToggleLabel = new JLabel("否", SwingConstants.CENTER);
        this.menuGrid = new JPanel(new GridLayout(2, 2, 2, 2));
        this.menuPanel = new JPanel(new GridBagLayout());
        this.menuGrid.setOpaque(false);
        this.menuPanel.setOpaque(false);
        this.wordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.phoneticLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.meaningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.sentenceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.synonymsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.prevWordLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.nextWordLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.translateToggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.masteredToggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.prevWordLabel.setOpaque(false);
        this.nextWordLabel.setOpaque(false);
        this.translateToggleLabel.setOpaque(false);
        this.masteredToggleLabel.setOpaque(false);
        this.prevWordLabel.setVisible(false);
        this.nextWordLabel.setVisible(false);
        this.translateToggleLabel.setVisible(false);
        this.masteredToggleLabel.setVisible(false);

        this.contentPanel.add(wordLabel);
        this.contentPanel.add(Box.createVerticalStrut(8));
        this.contentPanel.add(phoneticLabel);
        this.contentPanel.add(Box.createVerticalStrut(8));
        this.contentPanel.add(meaningLabel);
        this.contentPanel.add(Box.createVerticalStrut(8));
        this.contentPanel.add(sentenceLabel);
        this.contentPanel.add(Box.createVerticalStrut(8));
        this.contentPanel.add(synonymsLabel);
        this.contentPanel.add(Box.createVerticalGlue());
        menuGrid.add(prevWordLabel);
        menuGrid.add(nextWordLabel);
        menuGrid.add(translateToggleLabel);
        menuGrid.add(masteredToggleLabel);
        GridBagConstraints menuGbc = new GridBagConstraints();
        menuGbc.gridx = 0;
        menuGbc.gridy = 0;
        menuGbc.anchor = GridBagConstraints.CENTER;
        menuGbc.fill = GridBagConstraints.NONE;
        menuGbc.weightx = 1;
        menuGbc.weighty = 1;
        menuGbc.insets = new Insets(0, 0, 0, 0);
        menuPanel.add(menuGrid, menuGbc);

        rootPanel.add(contentPanel, BorderLayout.CENTER);
        rootPanel.add(menuPanel, BorderLayout.EAST);

        MouseAdapter contextMenuListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowContextMenu(e);
            }
        };
        this.rootPanel.addMouseListener(contextMenuListener);
        this.menuPanel.addMouseListener(contextMenuListener);
        this.contentPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowContextMenu(e);
            }
        });

        this.prevWordLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.isControlDown()) {
                    return;
                }
                showPreviousWord();
            }
        });
        this.nextWordLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.isControlDown()) {
                    return;
                }
                showNextWord();
            }
        });
        this.translateToggleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.isControlDown()) {
                    return;
                }
                PdfViewerSettings settings = PdfViewerSettings.getInstance();
                boolean base = settings.isWordPopupShowMeaning();
                boolean current = meaningVisibleOverride == null ? base : meaningVisibleOverride;
                meaningVisibleOverride = !current;
                refreshContent();
            }
        });
        this.masteredToggleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.isControlDown()) {
                    return;
                }
                PdfViewerSettings.WordEntryData current = getCurrentEntry();
                if (current == null || current.word == null || current.word.isBlank()) {
                    return;
                }
                PdfViewerSettings settings = PdfViewerSettings.getInstance();
                settings.toggleWordMastered(current);
                refreshWordPool(true);
                refreshContent();
            }
        });
        MouseAdapter ctrlDragListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isControlDown() || !SwingUtilities.isLeftMouseButton(e)) {
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
                int x = dragWindowStartOnScreen.x + dx;
                int y = dragWindowStartOnScreen.y + dy;
                window.setLocation(x, y);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Window window = SwingUtilities.getWindowAncestor(contentPanel);
                if (window != null && dragAnchorOnScreen != null && dragWindowStartOnScreen != null) {
                    scheduleBoundsPersist(window);
                }
                dragAnchorOnScreen = null;
                dragWindowStartOnScreen = null;
            }
        };
        installCtrlDragListeners(ctrlDragListener);

        MouseAdapter hoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setMenuVisible(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point local = new Point(e.getLocationOnScreen());
                SwingUtilities.convertPointFromScreen(local, rootPanel);
                if (rootPanel.contains(local)) {
                    return;
                }
                setMenuVisible(false);
            }
        };
        installHoverListeners(hoverListener);

        refreshWordPool(true);
        refreshContent();
        applyStyle(PdfViewerSettings.getInstance());

        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(this)
                .subscribe(PdfViewerSettingsListener.TOPIC, new PdfViewerSettingsListener() {
                    @Override
                    public void pdfPathChanged(@Nullable String newPdfPath) {
                    }

                    @Override
                    public void nightModeChanged(boolean enabled) {
                    }

                    @Override
                    public void pdfBackgroundChanged(@NotNull Color newBackgroundColor) {
                    }

                    @Override
                    public void hoverSecondsChanged(int seconds) {
                    }

                    @Override
                    public void zoomPercentChanged(int percent) {
                    }

                    @Override
                    public void pdfTextColorChanged(@NotNull Color newTextColor) {
                    }

                    @Override
                    public void treeBackgroundChanged(@NotNull Color newBackgroundColor) {
                    }

                    @Override
                    public void treeTextColorChanged(@NotNull Color newTextColor) {
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
                    }

                    @Override
                    public void editorPopupBorderVisibilityChanged(boolean visible) {
                    }

                    @Override
                    public void editorPopupPdfBackgroundChanged(@NotNull Color newBackgroundColor) {
                    }

                    @Override
                    public void editorPopupPdfTextColorChanged(@NotNull Color newTextColor) {
                    }

                    @Override
                    public void renderBatchPageCountChanged(int pageCount) {
                    }

                    @Override
                    public void wordPopupStyleChanged(int width, int height, int x, int y, int fontSize, @NotNull Color fontColor) {
                        if (dragAnchorOnScreen != null || adjustingBounds) {
                            return;
                        }
                        PdfViewerSettings settings = PdfViewerSettings.getInstance();
                        applyStyle(settings);
                        if (isPopupActive()) {
                            relocatePopup(settings);
                        }
                    }

                    @Override
                    public void wordPopupContentDisplayChanged(boolean showMeaning, boolean showSentence, boolean showSynonyms, int sentenceLimit) {
                        refreshContent();
                    }

                    @Override
                    public void wordPopupOpacityChanged(int percent) {
                        applyWindowOpacity(percent);
                    }

                    @Override
                    public void wordSourceChanged(boolean builtinEnabled, @Nullable String customPath) {
                        refreshWordPool(true);
                        refreshContent();
                    }

                    @Override
                    public void wordCategoryFiltersChanged(@NotNull List<String> difficulties, @NotNull List<String> themes, @NotNull List<String> sources) {
                        refreshWordPool(true);
                        refreshContent();
                    }

                    @Override
                    public void wordHiddenStateChanged(@NotNull String bookKey) {
                        if (!Objects.equals(PdfViewerSettings.getInstance().getSelectedVocabularyBookKey(), bookKey)) {
                            return;
                        }
                        refreshWordPool(true);
                        refreshContent();
                    }
                });
    }

    public void toggle(@NotNull Editor editor) {
        if (isPopupActive()) {
            hide();
            return;
        }
        show(editor);
    }

    public void show(@NotNull Editor editor) {
        lastEditor = editor;
        refreshWordPool(true);
        refreshContent();

        if (isPopupActive()) {
            relocatePopup(PdfViewerSettings.getInstance());
            return;
        }

        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(rootPanel, (JComponent) rootPanel)
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
                flushPendingBoundsPersistNow();
                detachWindowTracking();
                popup = null;
            }
        });

        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        Point location = resolvePopupLocation(editor, settings);
        popup.show(new RelativePoint(editor.getContentComponent(), location));
        attachWindowTracking();
        applyWindowOpacity(settings.getWordPopupOpacityPercent());
    }

    public void hide() {
        flushPendingBoundsPersistNow();
        JBPopup current = popup;
        if (current != null && !current.isDisposed()) {
            current.cancel();
        }
        detachWindowTracking();
        popup = null;
    }

    public boolean isPopupVisible() {
        return isPopupActive();
    }

    public boolean hasAnyWord() {
        refreshWordPool(false);
        return !activeWords.isEmpty();
    }

    public void showNextWord() {
        refreshWordPool(true);
        if (activeWords.isEmpty()) {
            refreshContent();
            return;
        }
        int next = findNextIndexSkippingMastered(currentIndex);
        if (next < 0) {
            next = (currentIndex + 1 + activeWords.size()) % activeWords.size();
        }
        currentIndex = next;
        refreshContent();
    }

    public void showPreviousWord() {
        refreshWordPool(true);
        if (activeWords.isEmpty()) {
            refreshContent();
            return;
        }
        int fromIndex = currentIndex < 0 ? 0 : currentIndex;
        int prev = findPreviousIndexSkippingMastered(fromIndex);
        if (prev < 0) {
            prev = (fromIndex - 1 + activeWords.size()) % activeWords.size();
        }
        currentIndex = prev;
        refreshContent();
    }

    public void markCurrentWordMasteredAndAdvance() {
        refreshWordPool(false);
        PdfViewerSettings.WordEntryData current = getCurrentEntry();
        if (current == null || current.word == null || current.word.isBlank()) {
            return;
        }
        boolean mastered = PdfViewerSettings.getInstance().toggleWordMastered(current);
        refreshWordPool(true);
        if (mastered && !activeWords.isEmpty()) {
            int next = findNextIndexSkippingMastered(currentIndex);
            if (next >= 0) {
                currentIndex = next;
            }
        }
        refreshContent();
    }

    private void refreshWordPool(boolean keepCurrentWord) {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        String bookKey = settings.getSelectedVocabularyBookKey();
        List<PdfViewerSettings.WordEntryData> source = settings.getWordEntries();
        List<PdfViewerSettings.WordEntryData> filtered = new ArrayList<>(source.size());

        for (PdfViewerSettings.WordEntryData entry : source) {
            if (entry == null || entry.word == null || entry.word.isBlank()) {
                continue;
            }
            if (settings.isWordHiddenInPopup(bookKey, entry.word)) {
                continue;
            }
            filtered.add(entry);
        }
        activeWords = List.copyOf(filtered);
        if (activeWords.isEmpty()) {
            currentIndex = -1;
            return;
        }

        if (keepCurrentWord) {
            String currentKey = normalizeWordKey(getCurrentWordText());
            if (currentKey != null) {
                for (int i = 0; i < activeWords.size(); i++) {
                    String entryKey = normalizeWordKey(activeWords.get(i).word);
                    if (Objects.equals(currentKey, entryKey)) {
                        currentIndex = i;
                        return;
                    }
                }
            }
        }

        int firstPending = findNextIndexSkippingMastered(-1);
        currentIndex = firstPending >= 0 ? firstPending : 0;
    }

    private void refreshContent() {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        PdfViewerSettings.WordEntryData current = getCurrentEntry();
        if (current == null) {
            wordLabel.setText("暂无可学习单词");
            phoneticLabel.setText("");
            phoneticLabel.setVisible(false);
            meaningLabel.setText("请先导入词库或在设置中放宽分类筛选");
            meaningLabel.setVisible(true);
            hintLabel.setText("快捷键：显示/隐藏、下一个单词、标记已学会");
            masteredToggleLabel.setText("否");
            applyStyle(settings);
            return;
        }

        String phonetic = current.phonetic == null ? "" : current.phonetic.trim();
        String meaning = current.meaning == null || current.meaning.isBlank() ? "(暂无释义)" : current.meaning.trim();
        wordLabel.setText(current.word);
        phoneticLabel.setText(phonetic.isEmpty() ? "" : "/" + phonetic + "/");
        phoneticLabel.setVisible(false);

        boolean showMeaning = meaningVisibleOverride == null ? settings.isWordPopupShowMeaning() : meaningVisibleOverride;
        boolean showSentence = settings.isWordPopupShowSentence();
        boolean showSynonyms = settings.isWordPopupShowSynonyms();

        meaningLabel.setVisible(showMeaning);
        meaningLabel.setText(showMeaning ? toHtmlLine("释义", meaning) : "");

        sentenceLabel.setVisible(showSentence);
        sentenceLabel.setText(showSentence ? toHtmlList("例句", limitedSentences(current.sentenceEnList, settings.getWordPopupSentenceLimit())) : "");

        synonymsLabel.setVisible(showSynonyms);
        synonymsLabel.setText(showSynonyms ? toHtmlSynonyms(current.synonymsByPos) : "");

        masteredToggleLabel.setText(settings.isWordMastered(current.word) ? "是" : "否");
        hintLabel.setText("快捷键：下一个单词 / 标记已学会（右键可切换状态）");
        applyStyle(settings);
    }

    private void applyStyle(@NotNull PdfViewerSettings settings) {
        int width = settings.getWordPopupWidth();
        int height = settings.getWordPopupHeight();
        int fontSize = settings.getWordPopupFontSize();
        Color fontColor = settings.getWordPopupFontColor();

        int contentWidth = Math.max(1, width - MENU_PANEL_WIDTH);
        Dimension rootSize = new Dimension(width, height);
        rootPanel.setPreferredSize(rootSize);
        contentPanel.setPreferredSize(new Dimension(contentWidth, height));
        menuPanel.setPreferredSize(new Dimension(MENU_PANEL_WIDTH, height));
        int menuGridSize = Math.max(24, MENU_PANEL_WIDTH - 6);
        menuGrid.setPreferredSize(new Dimension(menuGridSize, menuGridSize));

        Font base = wordLabel.getFont();
        Font wordFont = base.deriveFont(Font.BOLD, Math.max(10, fontSize + 2));
        Font detailFont = base.deriveFont(Font.PLAIN, Math.max(8, fontSize - 2));
        Font meaningFont = base.deriveFont(Font.PLAIN, fontSize);
        Font menuFont = base.deriveFont(Font.BOLD, Math.max(10, fontSize - 4));
        wordLabel.setFont(wordFont);
        phoneticLabel.setFont(detailFont);
        meaningLabel.setFont(meaningFont);
        sentenceLabel.setFont(detailFont);
        synonymsLabel.setFont(detailFont);
        hintLabel.setFont(detailFont);
        translateToggleLabel.setFont(menuFont);
        masteredToggleLabel.setFont(menuFont);
        wordLabel.setForeground(fontColor);
        phoneticLabel.setForeground(fontColor);
        meaningLabel.setForeground(fontColor);
        sentenceLabel.setForeground(fontColor);
        synonymsLabel.setForeground(fontColor);
        hintLabel.setForeground(fontColor.darker());
        translateToggleLabel.setForeground(fontColor);
        masteredToggleLabel.setForeground(fontColor);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void relocatePopup(@NotNull PdfViewerSettings settings) {
        Editor editor = lastEditor;
        if (editor == null) {
            return;
        }
        Window window = SwingUtilities.getWindowAncestor(contentPanel);
        if (window == null) {
            return;
        }
        Point local = resolvePopupLocation(editor, settings);
        Point screen = new Point(local);
        SwingUtilities.convertPointToScreen(screen, editor.getContentComponent());
        if (!screen.equals(window.getLocation())) {
            window.setLocation(screen);
        }
        Dimension desiredSize = new Dimension(settings.getWordPopupWidth(), settings.getWordPopupHeight());
        if (!desiredSize.equals(window.getSize())) {
            window.setSize(desiredSize);
        }
    }

    private @NotNull Point resolvePopupLocation(@NotNull Editor editor, @NotNull PdfViewerSettings settings) {
        return new Point(settings.getWordPopupX(), settings.getWordPopupY());
    }

    private int findNextIndexSkippingMastered(int fromIndex) {
        if (activeWords.isEmpty()) {
            return -1;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        int size = activeWords.size();
        for (int i = 1; i <= size; i++) {
            int candidate = (fromIndex + i + size) % size;
            PdfViewerSettings.WordEntryData entry = activeWords.get(candidate);
            if (!settings.isWordMastered(entry.word)) {
                return candidate;
            }
        }
        return -1;
    }

    private int findPreviousIndexSkippingMastered(int fromIndex) {
        if (activeWords.isEmpty()) {
            return -1;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        int size = activeWords.size();
        for (int i = 1; i <= size; i++) {
            int candidate = (fromIndex - i + size) % size;
            PdfViewerSettings.WordEntryData entry = activeWords.get(candidate);
            if (!settings.isWordMastered(entry.word)) {
                return candidate;
            }
        }
        return -1;
    }

    private @Nullable PdfViewerSettings.WordEntryData getCurrentEntry() {
        if (activeWords.isEmpty() || currentIndex < 0 || currentIndex >= activeWords.size()) {
            return null;
        }
        return activeWords.get(currentIndex);
    }

    private @Nullable String getCurrentWordText() {
        PdfViewerSettings.WordEntryData entry = getCurrentEntry();
        return entry == null ? null : entry.word;
    }

    private static @Nullable String normalizeFilterValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static @Nullable String normalizeWordKey(@Nullable String value) {
        return normalizeFilterValue(value);
    }

    private void maybeShowContextMenu(@NotNull MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        PdfViewerSettings.WordEntryData current = getCurrentEntry();
        if (current == null || current.word == null || current.word.isBlank()) {
            return;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        boolean mastered = settings.isWordMastered(current.word);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem toggleMastered = new JMenuItem(mastered ? "标记为未学会" : "标记为已学会");
        toggleMastered.addActionListener(e -> {
            settings.toggleWordMastered(current);
            refreshWordPool(true);
            refreshContent();
        });
        menu.add(toggleMastered);
        menu.show(event.getComponent(), event.getX(), event.getY());
    }

    private static @NotNull String toHtmlLine(@NotNull String title, @NotNull String content) {
        return "<html><b>" + escapeHtml(title) + "：</b>" + escapeHtml(content) + "</html>";
    }

    private static @NotNull String toHtmlList(@NotNull String title, @NotNull List<String> values) {
        if (values.isEmpty()) {
            return "<html><b>" + escapeHtml(title) + "：</b>(暂无)</html>";
        }
        StringBuilder sb = new StringBuilder("<html><b>").append(escapeHtml(title)).append("：</b><br/>");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append("<br/>");
            }
            sb.append(i + 1).append(". ").append(escapeHtml(values.get(i)));
        }
        sb.append("</html>");
        return sb.toString();
    }

    private static @NotNull String toHtmlSynonyms(@Nullable List<PdfViewerSettings.WordSynonymGroupData> groups) {
        if (groups == null || groups.isEmpty()) {
            return "<html><b>同近义：</b>(暂无)</html>";
        }
        StringBuilder sb = new StringBuilder("<html><b>同近义：</b><br/>");
        boolean appended = false;
        for (PdfViewerSettings.WordSynonymGroupData group : groups) {
            if (group == null || group.pos == null || group.words == null || group.words.isEmpty()) {
                continue;
            }
            if (appended) {
                sb.append("<br/>");
            }
            sb.append(escapeHtml(group.pos)).append("：").append(escapeHtml(String.join(", ", group.words)));
            appended = true;
        }
        if (!appended) {
            return "<html><b>同近义：</b>(暂无)</html>";
        }
        sb.append("</html>");
        return sb.toString();
    }

    private static @NotNull List<String> limitedSentences(@Nullable List<String> source, int limit) {
        if (source == null || source.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<String> values = new ArrayList<>(Math.min(limit, source.size()));
        for (String sentence : source) {
            if (sentence == null || sentence.isBlank()) {
                continue;
            }
            values.add(sentence.trim());
            if (values.size() >= limit) {
                break;
            }
        }
        return values;
    }

    private static @NotNull String escapeHtml(@NotNull String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private boolean isPopupActive() {
        return popup != null && !popup.isDisposed();
    }

    private void setMenuVisible(boolean visible) {
        boolean effective = visible && isPopupActive() && getCurrentEntry() != null;
        prevWordLabel.setVisible(effective);
        nextWordLabel.setVisible(effective);
        translateToggleLabel.setVisible(effective);
        masteredToggleLabel.setVisible(effective);
        menuPanel.repaint();
    }

    private void installCtrlDragListeners(@NotNull MouseAdapter listener) {
        rootPanel.addMouseListener(listener);
        rootPanel.addMouseMotionListener(listener);
        contentPanel.addMouseListener(listener);
        contentPanel.addMouseMotionListener(listener);
        menuPanel.addMouseListener(listener);
        menuPanel.addMouseMotionListener(listener);
        menuGrid.addMouseListener(listener);
        menuGrid.addMouseMotionListener(listener);
        wordLabel.addMouseListener(listener);
        wordLabel.addMouseMotionListener(listener);
        phoneticLabel.addMouseListener(listener);
        phoneticLabel.addMouseMotionListener(listener);
        meaningLabel.addMouseListener(listener);
        meaningLabel.addMouseMotionListener(listener);
        sentenceLabel.addMouseListener(listener);
        sentenceLabel.addMouseMotionListener(listener);
        synonymsLabel.addMouseListener(listener);
        synonymsLabel.addMouseMotionListener(listener);
        hintLabel.addMouseListener(listener);
        hintLabel.addMouseMotionListener(listener);
        prevWordLabel.addMouseListener(listener);
        prevWordLabel.addMouseMotionListener(listener);
        nextWordLabel.addMouseListener(listener);
        nextWordLabel.addMouseMotionListener(listener);
        translateToggleLabel.addMouseListener(listener);
        translateToggleLabel.addMouseMotionListener(listener);
        masteredToggleLabel.addMouseListener(listener);
        masteredToggleLabel.addMouseMotionListener(listener);
    }

    private void installHoverListeners(@NotNull MouseAdapter listener) {
        rootPanel.addMouseListener(listener);
        contentPanel.addMouseListener(listener);
        menuPanel.addMouseListener(listener);
        menuGrid.addMouseListener(listener);
        wordLabel.addMouseListener(listener);
        phoneticLabel.addMouseListener(listener);
        meaningLabel.addMouseListener(listener);
        sentenceLabel.addMouseListener(listener);
        synonymsLabel.addMouseListener(listener);
        hintLabel.addMouseListener(listener);
        prevWordLabel.addMouseListener(listener);
        nextWordLabel.addMouseListener(listener);
        translateToggleLabel.addMouseListener(listener);
        masteredToggleLabel.addMouseListener(listener);
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
                scheduleBoundsPersist(window);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                scheduleBoundsPersist(window);
            }
        };
        window.addComponentListener(trackedWindowListener);
    }

    private void scheduleBoundsPersist(@NotNull Window window) {
        Editor editor = lastEditor;
        if (editor == null) {
            return;
        }
        Point local = window.getLocationOnScreen();
        SwingUtilities.convertPointFromScreen(local, editor.getContentComponent());
        pendingWidth = window.getWidth();
        pendingHeight = window.getHeight();
        pendingX = local.x;
        pendingY = local.y;
        hasPendingBoundsPersist = true;
        adjustingBounds = true;
        if (boundsPersistTimer == null) {
            boundsPersistTimer = new Timer(150, e -> flushPendingBoundsPersist());
            boundsPersistTimer.setRepeats(false);
        }
        boundsPersistTimer.restart();
    }

    private void flushPendingBoundsPersist() {
        if (!hasPendingBoundsPersist) {
            adjustingBounds = false;
            return;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        Color color = settings.getWordPopupFontColor();
        settings.setWordPopupStyle(
                pendingWidth,
                pendingHeight,
                pendingX,
                pendingY,
                settings.getWordPopupFontSize(),
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );
        hasPendingBoundsPersist = false;
        adjustingBounds = false;
    }

    private void flushPendingBoundsPersistNow() {
        if (boundsPersistTimer != null) {
            boundsPersistTimer.stop();
        }
        flushPendingBoundsPersist();
    }

    private void applyWindowOpacity(int percent) {
        if (!isPopupActive()) {
            return;
        }
        Window window = trackedWindow;
        if (window == null) {
            window = SwingUtilities.getWindowAncestor(contentPanel);
        }
        if (window == null) {
            return;
        }
        float alpha = Math.max(0.10f, Math.min(1.0f, percent / 100f));
        try {
            window.setOpacity(alpha);
        } catch (Throwable ignored) {
        }
    }

    private void detachWindowTracking() {
        if (trackedWindow != null && trackedWindowListener != null) {
            trackedWindow.removeComponentListener(trackedWindowListener);
        }
        trackedWindow = null;
        trackedWindowListener = null;
    }

    private void persistWindowBounds(@NotNull Window window) {
        Editor editor = lastEditor;
        if (editor == null) {
            return;
        }
        Point local = window.getLocationOnScreen();
        SwingUtilities.convertPointFromScreen(local, editor.getContentComponent());
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        Color color = settings.getWordPopupFontColor();
        settings.setWordPopupStyle(
                window.getWidth(),
                window.getHeight(),
                local.x,
                local.y,
                settings.getWordPopupFontSize(),
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );
    }

    @Override
    public void dispose() {
        hide();
        detachWindowTracking();
        if (boundsPersistTimer != null) {
            boundsPersistTimer.stop();
        }
        lastEditor = null;
        activeWords = List.of();
        currentIndex = -1;
    }
}
