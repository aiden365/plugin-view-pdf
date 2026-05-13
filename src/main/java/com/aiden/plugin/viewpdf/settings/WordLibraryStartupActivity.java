package com.aiden.plugin.viewpdf.settings;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public final class WordLibraryStartupActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        WordLibraryLoader.reloadWordEntriesFromSettings(PdfViewerSettings.getInstance());
    }
}
