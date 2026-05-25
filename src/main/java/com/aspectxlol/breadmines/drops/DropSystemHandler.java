package com.aspectxlol.breadmines.drops;

import com.aspectxlol.breadmines.drops.storage.DropRepository;
import com.aspectxlol.breadmines.drops.util.DropUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * DropSystemHandler - Manages the block drops registry and database operations.
 * Handles all database interactions, item resolution, and drop calculations.
 */
public class DropSystemHandler {

    private final JavaPlugin plugin;
    private final DropRepository repository;
    private final DropItemResolver itemResolver;
    private final DropMultiplierService multiplierService;
    private boolean debugMode = false;

    public DropSystemHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.repository = new DropRepository(plugin);
        this.itemResolver = new DropItemResolver(plugin);
        this.multiplierService = new DropMultiplierService();
    }

    /**
     * Initialize the database connection and create table if needed.
     */
    public void initializeDatabase() throws SQLException {
        repository.initialize();
    }

    /**
     * Close the database connection.
     */
    public void closeDatabase() {
        repository.close();
    }

    /**
     * Query the database for a block's custom item_id.
     */
    public String queryBlockDrop(String blockName) throws SQLException {
        return repository.findItemId(blockName);
    }

    /**
     * Insert or update a block drop entry.
     */
    public void insertOrUpdateBlockDrop(String blockName, String itemId) throws SQLException {
        repository.upsert(blockName, itemId);
    }

    /**
     * Delete a block drop entry.
     */
    public void deleteBlockDrop(String blockName) throws SQLException {
        repository.delete(blockName);
    }

    /**
     * Retrieve all block drop entries.
     */
    public List<String[]> getAllBlockDrops() throws SQLException {
        return repository.fetchAll();
    }

    /**
     * Retrieve all registered block names from database.
     */
    public List<String> getAllBlockDropsNames() throws SQLException {
        return repository.fetchAllNames();
    }

    /**
     * Get the player's block drop multiplier from permissions.
     * Checks for breadmines.multiplier.X permissions where X is 20, 40, 60, 80, or 100.
     * Returns the multiplier value or 0 if no multiplier permission.
     */
    public int getPlayerMultiplier(Player player) {
        return multiplierService.getPlayerMultiplier(player);
    }

    /**
     * Calculate the native fortune amount from block drops.
     * Telepathy is enabled by default - no enchant check needed.
     * Applies permission-based drop multiplier.
     */
    public int calculateFortuneAmount(Block block, ItemStack tool, Player player) {
        int amount = block.getDrops(tool, player).stream()
                .mapToInt(ItemStack::getAmount)
                .max()
                .orElse(1);

        amount = multiplierService.applyMultiplier(amount, player);

        return Math.max(1, Math.min(64, amount));
    }

    /**
     * Resolve a configured item ID to a Bukkit Material if possible.
     */
    public Material resolveCustomMaterial(String itemId) {
        return itemResolver.resolveCustomMaterial(itemId);
    }

    /**
     * Get the Skript item value for a given item ID.
     */
    public Object getSkriptItemValue(String itemId) {
        return itemResolver.getSkriptItemValue(itemId);
    }

    /**
     * Hook into Skript variable {items::*} to fetch available item IDs from the registry.
     */
    public List<String> getSkriptItemIds() {
        return itemResolver.getSkriptItemIds();
    }

    /**
     * Get available Minecraft block types for tab completion.
     */
    public List<String> getMinecraftBlockTypes() {
        return BlockTypeRegistry.getBreakableBlockTypes();
    }

    /**
     * Normalize block names (replace spaces/dashes with underscores).
     */
    public String normalizeBlockName(String blockName) {
        return DropUtils.normalizeBlockName(blockName);
    }

    /**
     * Give an item to a player with fallback to dropping it.
     */
    public void giveItemToPlayer(Player player, ItemStack itemStack, Block block, String blockName) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        if (leftovers.isEmpty()) {
            plugin.getLogger().info("Added custom drop to inventory: " + itemStack.getType() + " x" + itemStack.getAmount() + " for block " + blockName);
        } else {
            leftovers.values().forEach(remaining -> block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), remaining));
            plugin.getLogger().warning("Inventory full for player " + player.getName() + "; dropped leftover custom drop for block " + blockName);
        }
    }

    /**
     * Check if debug mode is enabled.
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Set debug mode.
     */
    public void setDebugMode(boolean mode) {
        debugMode = mode;
    }
}
