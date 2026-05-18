package com.aiden.plugin.viewpdf.bookreading;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

public final class BookReadingNotifier {
    public static final String NOTIFICATION_GROUP_ID = "XTools.BookReading";

    private BookReadingNotifier() {
    }

    public static void showFirstLineOnBookSelected(@NotNull Project project, @Nullable String bookId) {
        if (bookId == null) {
            return;
        }
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        PdfViewerSettings.BookData book = findBookById(settings.getBooks(), bookId);
        if (book == null) {
            return;
        }
        String firstLine = readLine(book.inlineContent, 1);
        if (firstLine == null) {
            return;
        }
        notifyLine(project, firstLine);
    }

    public static void moveAndShowCurrentLine(@NotNull Project project, int delta) {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        String bookId = settings.getCurrentReadingBookId();
        if (bookId == null) {
            return;
        }
        PdfViewerSettings.BookData book = findBookById(settings.getBooks(), bookId);
        if (book == null) {
            return;
        }
        if (book.inlineContent == null || book.inlineContent.isBlank()) {
            return;
        }
        int currentLine = settings.getBookReadLine(bookId);
        int targetLine = Math.max(1, currentLine + delta);
        String line = readLine(book.inlineContent, targetLine);
        if (line == null) {
            return;
        }
        settings.setBookReadLine(bookId, targetLine);
        notifyLine(project, line);
    }

    private static void notifyLine(@NotNull Project project, @NotNull String lineText) {
        String content = escape(lineText);
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }

    private static @Nullable PdfViewerSettings.BookData findBookById(@NotNull List<PdfViewerSettings.BookData> books, @NotNull String bookId) {
        for (PdfViewerSettings.BookData book : books) {
            if (book == null || book.id == null) {
                continue;
            }
            if (bookId.equals(book.id)) {
                return book;
            }
        }
        return null;
    }

    private static @Nullable String readLine(@Nullable String content, int lineNumber) {
        if (content == null || content.isBlank()) {
            return null;
        }
        int normalizedLine = Math.max(1, lineNumber);
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            int current = 1;
            while ((line = reader.readLine()) != null) {
                if (current == normalizedLine) {
                    return line;
                }
                current++;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static @NotNull String escape(@NotNull String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
