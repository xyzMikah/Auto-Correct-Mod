package com.mk.autocorrect.client;

import java.util.Collections;
import java.util.List;

public class AutocorrectSuggestions {
    private static List<String> suggestions = Collections.emptyList();
    private static int selectedIndex = 0;
    private static int wordStart = -1; // index in input string where the flagged word begins

    public static void set(List<String> newSuggestions, int start) {
        suggestions = newSuggestions;
        selectedIndex = 0;
        wordStart = start;
    }

    public static void clear() {
        suggestions = Collections.emptyList();
        wordStart = -1;
    }

    public static boolean hasSuggestions() {
        return !suggestions.isEmpty();
    }

    public static List<String> get() {
        return suggestions;
    }

    public static int getSelectedIndex() {
        return selectedIndex;
    }

    public static String getSelected() {
        return suggestions.isEmpty() ? null : suggestions.get(selectedIndex);
    }

    public static int getWordStart() {
        return wordStart;
    }

    public static void moveUp() {
        if (suggestions.isEmpty()) return;
        selectedIndex = (selectedIndex - 1 + suggestions.size()) % suggestions.size();
    }

    public static void moveDown() {
        if (suggestions.isEmpty()) return;
        selectedIndex = (selectedIndex + 1) % suggestions.size();
    }
}
