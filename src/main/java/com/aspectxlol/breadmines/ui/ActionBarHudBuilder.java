package com.aspectxlol.breadmines.ui;

import com.aspectxlol.breadmines.Breadmines;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class ActionBarHudBuilder {
    private ActionBarHudBuilder() {}

    public static String build(Breadmines plugin, Player player) {
        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();
        var manaManager = plugin.getManaManager();
        double mana = manaManager.getMana(player);
        double maxMana = manaManager.getMaxMana(player);

        return ChatColor.RED + "❤ " + String.format("%.0f", health) + "/" + String.format("%.0f", maxHealth) + 
               ChatColor.GRAY + " ┃ " + ChatColor.BLUE + "✎ " + String.format("%.0f", mana) + "/" + String.format("%.0f", maxMana);
    }
}
