package com.aspectxlol.breadmines.skyblock.task;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.skyblock.manager.ManaManager;
import com.aspectxlol.breadmines.ui.ActionBarHudBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class ManaRegenerator {
    private ManaRegenerator() {}

    public static BukkitTask start(Breadmines plugin, ManaManager manaManager, double manaRegenPerTick) {
        return Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.isSystemEnabled() || manaManager == null) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player == null || !player.isOnline()) continue;

                double currentMana = manaManager.getMana(player);
                double maxMana = manaManager.getMaxMana(player);
                if (currentMana < maxMana) {
                    double newMana = Math.min(currentMana + manaRegenPerTick, maxMana);
                    manaManager.setMana(player, newMana);
                }

                String actionBar = ActionBarHudBuilder.build(plugin, player);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(actionBar));
            }
        }, 0L, 10L);
    }
}
