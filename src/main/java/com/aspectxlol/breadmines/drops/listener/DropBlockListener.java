package com.aspectxlol.breadmines.drops.listener;

import com.aspectxlol.breadmines.drops.DropSystemHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Locale;

/**
 * DropBlockListener - Intercepts block break events and applies custom drops.
 * Cancels vanilla drops when custom items are registered and handles fortune calculations.
 */
public class DropBlockListener implements Listener {

    private final JavaPlugin plugin;
    private final DropSystemHandler dropHandler;

    public DropBlockListener(JavaPlugin plugin, DropSystemHandler dropHandler) {
        this.plugin = plugin;
        this.dropHandler = dropHandler;
    }

    /**
     * Block break event handler - intercepts and cancels vanilla drops.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String blockName = dropHandler.normalizeBlockName(block.getType().name().toLowerCase(Locale.ROOT));

        try {
            if (dropHandler.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] block broken=" + block.getType().name() + " normalized=" + blockName + " location=" + block.getLocation());
            }

            String itemId = dropHandler.queryBlockDrop(blockName);

            if (dropHandler.isDebugMode() && itemId == null) {
                plugin.getLogger().info("[DEBUG] onBlockBreak no registered drop found for=" + blockName);
            }

            if (itemId != null) {
                Player player = event.getPlayer();
                ItemStack tool = player.getInventory().getItemInMainHand();

                int nativeFortuneAmount = dropHandler.calculateFortuneAmount(block, tool, player);

                if (dropHandler.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] onBlockBreak registered=" + blockName
                            + " itemId=" + itemId
                            + " tool=" + (tool != null ? tool.getType() : "none")
                            + " fortune=" + nativeFortuneAmount);
                }

                event.setDropItems(false);
                event.setExpToDrop(0);

                dropHandler.resolveDropItem(itemId, nativeFortuneAmount).ifPresentOrElse(customDrop ->
                        dropHandler.giveItemToPlayer(player, customDrop, block, blockName),
                    () -> plugin.getLogger().warning("Registered drop item id not found in registry or material list: " + itemId)
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error querying database: " + e.getMessage());
        }
    }
}
