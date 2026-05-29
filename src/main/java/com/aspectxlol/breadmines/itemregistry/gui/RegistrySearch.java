package com.aspectxlol.breadmines.itemregistry.gui;

import com.aspectxlol.breadmines.itemregistry.CustomItemDefinition;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RegistrySearch {
    private RegistrySearch() {}

    public static String normalizeSearchQuery(String value) {
        if (value == null) {
            return "";
        }

        return ChatColor.stripColor(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static List<String> tokenizeSearchQuery(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return List.of();
        }

        String[] parts = normalizedQuery.split("\\s+");
        List<String> tokens = new ArrayList<>();
        java.util.Collections.addAll(tokens, parts);
        return tokens;
    }

    public static int getSearchScore(CustomItemDefinition definition, String normalizedQuery, List<String> tokens) {
        if (definition == null) return 0;
        ItemStack itemStack = definition.getItemStack();
        String id = normalizeSearchQuery(definition.getId());
        String displayName = normalizeSearchQuery(definition.getDisplayName());
        String material = normalizeSearchQuery(itemStack.getType().name());
        String lore = normalizeSearchQuery(joinLore(itemStack));

        int score = 0;
        score += scoreField(id, normalizedQuery, tokens, 500, 350, 50);
        score += scoreField(displayName, normalizedQuery, tokens, 400, 300, 40);
        score += scoreField(material, normalizedQuery, tokens, 250, 200, 20);
        score += scoreField(lore, normalizedQuery, tokens, 100, 75, 10);
        return score;
    }

    private static int scoreField(String field, String normalizedQuery, List<String> tokens, int exactScore, int prefixScore, int tokenScore) {
        if (field == null || field.isEmpty()) {
            return 0;
        }

        int score = 0;
        if (field.equals(normalizedQuery)) {
            score += exactScore;
        } else if (field.startsWith(normalizedQuery)) {
            score += prefixScore;
        } else if (field.contains(normalizedQuery)) {
            score += tokenScore * 2;
        }

        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (field.equals(token)) {
                score += exactScore / 4;
            } else if (field.startsWith(token)) {
                score += prefixScore / 4;
            } else if (field.contains(token)) {
                score += tokenScore;
            }
        }

        return score;
    }

    private static String joinLore(ItemStack itemStack) {
        if (itemStack == null) return "";
        var meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasLore()) return "";
        return String.join(" ", meta.getLore());
    }
}
