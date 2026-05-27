package com.aspectxlol.breadmines.skyblock.enchantments;

import org.bukkit.ChatColor;

import java.util.Locale;

public enum SkyblockEnchantmentType {

    BIG_BRAIN("big_brain", "Big Brain"),
    MANA_REGEN("mana_regen", "Mana Regen");

    private final String key;
    private final String displayName;

    SkyblockEnchantmentType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayLine(int level) {
        return ChatColor.AQUA + displayName + " " + toRoman(level);
    }

    public double getMaxManaBonus(int level) {
        if (this != BIG_BRAIN || level <= 0) {
            return 0.0;
        }

        int clampedLevel = clampLevel(level);
        return Math.round(500.0 * Math.pow(1.3, clampedLevel - 1));
    }

    public double getManaRegenBonusPerSecond(int level) {
        if (this != MANA_REGEN || level <= 0) {
            return 0.0;
        }

        int clampedLevel = clampLevel(level);
        return 15.0 + ((clampedLevel - 1) * 5.0);
    }

    public static SkyblockEnchantmentType fromKey(String key) {
        if (key == null) {
            return null;
        }

        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (SkyblockEnchantmentType type : values()) {
            if (type.key.equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    public int clampLevel(int level) {
        if (level < 1) {
            return 1;
        }
        return Math.min(level, 5);
    }

    private static String toRoman(int level) {
        switch (level) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            default:
                return "V";
        }
    }
}