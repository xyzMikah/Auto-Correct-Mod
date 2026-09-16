package com.mk.autocorrect.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AutocorrectEngine {

    private static final Set<String> DICTIONARY = new HashSet<>();
    private static final Set<String> WHITELIST = new HashSet<>();

    static {
        loadDictionary();

        String[] mcTerms = {
                "diamond", "nether", "creeper", "enderman", "redstone", "obsidian",
                "netherite", "villager", "zombie", "skeleton", "pvp", "afk", "tp",
                "gg", "lol", "brb", "op", "mob", "xp", "griefer", "spawn", "lmao",
                "idk", "np", "ty", "gj", "wp", "gl", "hf", "rn", "tbh", "imo"
        };
        WHITELIST.addAll(Arrays.asList(mcTerms));
    }

    private static void loadDictionary() {
        // Loads from src/client/resources/words.txt -> packaged at assets root
        try (InputStream is = AutocorrectEngine.class.getResourceAsStream("/words.txt")) {
            if (is == null) {
                System.err.println("[AutoCorrect] words.txt not found on classpath!");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) DICTIONARY.add(line.toLowerCase());
                }
            }
        } catch (IOException e) {
            System.err.println("[AutoCorrect] Failed to load dictionary: " + e.getMessage());
        }
        System.out.println("[AutoCorrect] Loaded " + DICTIONARY.size() + " dictionary words.");
    }

    /**
     * Returns up to `limit` closest dictionary words to `word`, sorted by edit distance.
     * Returns empty list if the word is already known / whitelisted / not a plain word.
     */
    public static List<String> suggest(String word, int limit) {
        if (word == null || word.isEmpty()) return Collections.emptyList();

        String lower = word.toLowerCase();

        if (!lower.matches("[a-zA-Z]+")) return Collections.emptyList();
        if (word.length() < 3) return Collections.emptyList();
        if (DICTIONARY.contains(lower)) return Collections.emptyList();
        if (WHITELIST.contains(lower)) return Collections.emptyList();

        // Only scan words of similar length -- huge speedup on a 234k word dictionary.
        List<Map.Entry<String, Integer>> scored = new ArrayList<>();
        int len = lower.length();
        for (String dictWord : DICTIONARY) {
            if (Math.abs(dictWord.length() - len) > 2) continue;
            int distance = levenshtein(lower, dictWord);
            if (distance <= 2) {
                scored.add(Map.entry(dictWord, distance));
            }
        }

        if (scored.isEmpty()) return Collections.emptyList();

        scored.sort(Comparator.comparingInt(Map.Entry::getValue));

        List<String> result = new ArrayList<>();
        boolean capitalize = Character.isUpperCase(word.charAt(0));
        for (Map.Entry<String, Integer> entry : scored) {
            if (result.size() >= limit) break;
            String w = entry.getKey();
            result.add(capitalize ? Character.toUpperCase(w.charAt(0)) + w.substring(1) : w);
        }
        return result;
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}