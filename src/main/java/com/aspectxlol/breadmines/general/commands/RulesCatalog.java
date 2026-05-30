package com.aspectxlol.breadmines.general.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class RulesCatalog {

    private static final List<RuleSection> SECTIONS = List.of(
        new RuleSection(
            "chat",
            11,
            Material.PAPER,
            ChatColor.translateAlternateColorCodes('&', "&e&lChat Rules"),
            List.of(
                ChatColor.translateAlternateColorCodes('&', "&f1. &7No excessive spam"),
                ChatColor.translateAlternateColorCodes('&', "&f2. &7No begging"),
                ChatColor.translateAlternateColorCodes('&', "&f3. &7Keep chat in English"),
                ChatColor.translateAlternateColorCodes('&', "&f4. &7No advertising"),
                ChatColor.translateAlternateColorCodes('&', "&f5. &7No racism / hate speech"),
                ChatColor.translateAlternateColorCodes('&', "&f6. &7No Disrespecting Staff")
            )
        ),
        new RuleSection(
            "pvp",
            13,
            Material.DIAMOND_SWORD,
            ChatColor.translateAlternateColorCodes('&', "&c&lPVP Rules"),
            List.of(
                ChatColor.translateAlternateColorCodes('&', "&f1. &7No spam killing"),
                ChatColor.translateAlternateColorCodes('&', "&f2. &7No naked killing"),
                ChatColor.translateAlternateColorCodes('&', "&f3. &7No removing armor in combat"),
                ChatColor.translateAlternateColorCodes('&', "&f4. &7No boosting"),
                ChatColor.translateAlternateColorCodes('&', "&f5. &7No boosting via alts")
            )
        ),
        new RuleSection(
            "general",
            15,
            Material.BOOK,
            ChatColor.translateAlternateColorCodes('&', "&a&lGeneral Rules"),
            List.of(
                ChatColor.translateAlternateColorCodes('&', "&f1. &7No cheating / hacking"),
                ChatColor.translateAlternateColorCodes('&', "&f2. &7No exploiting bugs / glitches"),
                ChatColor.translateAlternateColorCodes('&', "&f3. &7Respect other players"),
                ChatColor.translateAlternateColorCodes('&', "&f4. &7No scamming")
            )
        )
    );

    private RulesCatalog() {
    }

    static List<RuleSection> sections() {
        return Collections.unmodifiableList(SECTIONS);
    }

    static List<String> ruleSuggestions() {
        List<String> suggestions = new ArrayList<>();
        for (RuleSection section : SECTIONS) {
            for (int index = 0; index < section.rules.size(); index++) {
                suggestions.add(section.key + "-" + (index + 1));
                suggestions.add(String.valueOf(globalRuleIndex(section, index)));
            }
        }
        return suggestions;
    }

    static Optional<RuleEntry> findRule(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String normalized = input.toLowerCase(Locale.ROOT).trim();
        for (RuleSection section : SECTIONS) {
            for (int index = 0; index < section.rules.size(); index++) {
                RuleEntry entry = new RuleEntry(section, index);
                if (entry.matches(normalized)) {
                    return Optional.of(entry);
                }
            }
        }

        return Optional.empty();
    }

    static String formatCitation(RuleEntry entry) {
        return ChatColor.GOLD + "Rule Cited: " + ChatColor.YELLOW + entry.displayName() + ChatColor.GRAY + " - " + ChatColor.WHITE + entry.ruleText();
    }

    private static int globalRuleIndex(RuleSection targetSection, int targetIndex) {
        int index = 0;
        for (RuleSection section : SECTIONS) {
            for (int ruleIndex = 0; ruleIndex < section.rules.size(); ruleIndex++) {
                index++;
                if (section == targetSection && ruleIndex == targetIndex) {
                    return index;
                }
            }
        }
        return -1;
    }

    static final class RuleSection {
        final String key;
        final int slot;
        final Material icon;
        final String title;
        final List<String> rules;

        private RuleSection(String key, int slot, Material icon, String title, List<String> rules) {
            this.key = key;
            this.slot = slot;
            this.icon = icon;
            this.title = title;
            this.rules = List.copyOf(rules);
        }
    }

    static final class RuleEntry {
        private final RuleSection section;
        private final int index;

        private RuleEntry(RuleSection section, int index) {
            this.section = section;
            this.index = index;
        }

        String ruleText() {
            return section.rules.get(index);
        }

        String displayName() {
            return section.title + " #" + (index + 1);
        }

        private boolean matches(String normalized) {
            String numericGlobal = String.valueOf(globalIndex());
            String numericSection = String.valueOf(index + 1);
            String sectionKey = section.key.toLowerCase(Locale.ROOT);
            return normalized.equals(numericGlobal)
                || normalized.equals(sectionKey + "-" + numericSection)
                || normalized.equals(sectionKey + numericSection)
                || normalized.equals(sectionKey + " " + numericSection);
        }

        private int globalIndex() {
            int index = 0;
            for (RuleSection current : SECTIONS) {
                for (int ruleIndex = 0; ruleIndex < current.rules.size(); ruleIndex++) {
                    index++;
                    if (current == section && ruleIndex == this.index) {
                        return index;
                    }
                }
            }
            return -1;
        }
    }
}