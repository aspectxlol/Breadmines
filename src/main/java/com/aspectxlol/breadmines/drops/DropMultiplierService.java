package com.aspectxlol.breadmines.drops;

import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class DropMultiplierService {

    public int getPlayerMultiplier(Player player) {
        if (player.hasPermission("breadmines.multiplier.100")) return 100;
        if (player.hasPermission("breadmines.multiplier.80")) return 80;
        if (player.hasPermission("breadmines.multiplier.60")) return 60;
        if (player.hasPermission("breadmines.multiplier.40")) return 40;
        if (player.hasPermission("breadmines.multiplier.20")) return 20;
        return 0;
    }

    public int applyMultiplier(int amount, Player player) {
        int multiplier = getPlayerMultiplier(player);
        if (multiplier <= 0) {
            return amount;
        }

        if (multiplier == 100) {
            return amount + 1;
        }

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < multiplier) {
            return amount + 1;
        }

        return amount;
    }
}
