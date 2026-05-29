package com.aspectxlol.breadmines.skyblock.listener;

import com.aspectxlol.breadmines.Breadmines;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class EtherwarpPreviewTask {
    private EtherwarpPreviewTask() {}

    public static void start(Breadmines plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.isSystemEnabled()) return;

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player == null || !player.isSneaking()) continue;

                    var heldItem = player.getInventory().getItemInMainHand();
                    if (heldItem == null || !heldItem.hasItemMeta()) continue;
                    var meta = heldItem.getItemMeta();
                    if (meta == null || !meta.hasDisplayName()) continue;

                    String displayName = ChatColor.stripColor(meta.getDisplayName());
                    if (!displayName.equals("Aspect of the Void")) continue;

                    Block targetBlock = player.getTargetBlockExact(60, FluidCollisionMode.NEVER);
                    if (targetBlock != null && targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.BARRIER) {
                        Block feetBlock = targetBlock.getRelative(org.bukkit.block.BlockFace.UP);
                        Block headBlock = feetBlock.getRelative(org.bukkit.block.BlockFace.UP);
                        if (!feetBlock.getType().isSolid() && !headBlock.getType().isSolid() && feetBlock.getType() != Material.BARRIER && headBlock.getType() != Material.BARRIER) {
                            var targetLoc = targetBlock.getLocation().clone().add(0.5, 1.0, 0.5);
                            if (player.getLocation().distance(targetLoc) <= 60.0) {
                                player.spawnParticle(Particle.ENCHANT, targetLoc, 2, 0.2, 0.2, 0.2, 0.05);
                                player.spawnParticle(Particle.END_ROD, targetLoc, 1, 0.1, 0.1, 0.1, 0.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
