package com.aiden.plugin.viewpdf.bookreading;

import com.aiden.plugin.viewpdf.settings.PdfViewerSettings;
import com.intellij.notification.NotificationGroup;
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
            notifyInfo(project, "未找到当前阅读图书");
            return;
        }
        String bookName = book.name == null ? "未命名图书" : book.name;
        String firstLine = readLine(book.inlineContent, 1);
        if (firstLine == null) {
            notifyInfo(project, "《" + bookName + "》无可显示内容或内容读取失败");
            return;
        }
        notifyLine(project, bookName, 1, firstLine);
    }

    public static void moveAndShowCurrentLine(@NotNull Project project, int delta) {
        PdfViewerSettings settings = PdfViewerSettings.getInstance();
        String bookId = settings.getCurrentReadingBookId();
        if (bookId == null) {
            notifyInfo(project, "未选择当前阅读图书");
            return;
        }
        PdfViewerSettings.BookData book = findBookById(settings.getBooks(), bookId);
        if (book == null) {
            notifyInfo(project, "未找到当前阅读图书");
            return;
        }
        String bookName = book.name == null ? "未命名图书" : book.name;
        if (book.inlineContent == null || book.inlineContent.isBlank()) {
            notifyInfo(project, "《" + bookName + "》无可显示内容或内容读取失败");
            return;
        }
        int currentLine = settings.getBookReadLine(bookId);
        int targetLine = currentLine + delta;
        if (targetLine < 1) {
            notifyInfo(project, "已到《" + bookName + "》第 1 行");
            return;
        }
        String line = readLine(book.inlineContent, targetLine);
        if (line == null) {
            notifyInfo(project, "已到《" + bookName + "》最后一行");
            return;
        }
        settings.setBookReadLine(bookId, targetLine);
        notifyLine(project, bookName, targetLine, line);
    }

    private static void notifyLine(@NotNull Project project, @NotNull String bookName, int lineNumber, @NotNull String lineText) {
        String content = "《" + escape(bookName) + "》第 " + lineNumber + " 行： " + escape(lineText);
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }

    private static void notifyInfo(@NotNull Project project, @NotNull String message) {
        NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID);
        group.createNotification(escape(message), NotificationType.INFORMATION).notify(project);
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
