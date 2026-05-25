package com.aspectxlol.breadmines.skyblock.util;

import com.aspectxlol.breadmines.skyblock.manager.ManaManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public final class AbilityUtils {

    private AbilityUtils() {
    }

    public static boolean tryConsumeMana(Player player, ManaManager manaManager, double cost) {
        if (!manaManager.deductMana(player, cost)) {
            player.sendMessage(ChatColor.RED + "✗ Insufficient mana (need " + String.format("%.0f", cost) + ")");
            return false;
        }
        return true;
    }

    public static void applyPotionEffects(Player source, double radius, PotionEffect... effects) {
        for (PotionEffect effect : effects) {
            source.addPotionEffect(effect);
        }

        for (Entity entity : source.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player) || entity == source) {
                continue;
            }
            Player target = (Player) entity;
            for (PotionEffect effect : effects) {
                target.addPotionEffect(effect);
            }
        }
    }
}
