package com.aspectxlol.breadmines.skyblock.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class TeleportUtils {
    private TeleportUtils() {}

    public static Location calculateTeleportLocation(Player player, double maxDist) {
        Location loc = player.getLocation().clone();
        if (loc.getWorld() == null) return null;

        org.bukkit.util.Vector dir = loc.getDirection().normalize();
        Location lastSafe = loc.clone();

        for (double d = 0.5; d <= maxDist; d += 0.5) {
            Location check = loc.clone().add(dir.clone().multiply(d));

            Block feet = check.getBlock();
            Block head = check.clone().add(0, 1.0, 0).getBlock();

            if (feet.getType().isSolid() || head.getType().isSolid() || feet.getType() == Material.BARRIER || head.getType() == Material.BARRIER) {
                break;
            }

            lastSafe = check;
        }

        lastSafe.setYaw(loc.getYaw());
        lastSafe.setPitch(loc.getPitch());
        return lastSafe;
    }
}
