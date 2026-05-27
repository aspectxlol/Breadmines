package com.aspectxlol.breadmines.skyblock.listener;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.skyblock.manager.ManaManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * JoinListener - Manages player mana profile initialization and cleanup.
 * 
 * THREAD SAFETY:
 * - PlayerJoinEvent and PlayerQuitEvent fire on the main thread
 * - ManaManager uses ConcurrentHashMap for thread-safe operations
 * - Initialization/removal are idempotent operations (safe to call multiple times)
 */
public class JoinListener implements Listener {

    private final ManaManager manaManager;
    private final Breadmines plugin;

    public JoinListener(Breadmines plugin) {
        this.plugin = plugin;
        this.manaManager = plugin.getManaManager();
    }

    /**
     * Initializes a new player's mana profile when they join the server.
     * Sets default mana to 500.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        manaManager.initializePlayerMana(player);
        if (plugin.getSkyblockEnchantmentManager() != null) {
            plugin.getSkyblockEnchantmentManager().refreshPlayerStats(player, plugin.getBaseManaRegenPerSecond());
        }
    }

    /**
     * Removes a player's mana profile when they leave the server.
     * Prevents memory leaks from offline player data accumulation.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        manaManager.removePlayerMana(player);
    }
}
