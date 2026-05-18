package com.aiden.plugin.viewpdf.editorlookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import org.jetbrains.annotations.NotNull;

public final class EditorWordLookupElement extends LookupElement {
    public enum Kind {
        WORD,
        NEXT,
        PREV,
        LEARN
    }

    private final Kind kind;
    private final String text;

    public EditorWordLookupElement(@NotNull Kind kind, @NotNull String text) {
        this.kind = kind;
        this.text = text;
    }

    public @NotNull Kind getKind() {
        return kind;
    }

    @Override
    public @NotNull String getLookupString() {
        return text;
    }

    @Override
    public void renderElement(@NotNull LookupElementPresentation presentation) {
        presentation.setItemText(text);
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        context.setAddCompletionChar(false);
    }
}

