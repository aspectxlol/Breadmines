package com.aspectxlol.breadmines.drops;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ch.njol.skript.variables.Variables;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DropSystemHandler - Manages the block drops registry and database operations.
 * Handles all database interactions, item resolution, and drop calculations.
 */
public class DropSystemHandler {

    private final JavaPlugin plugin;
    private Connection dbConnection;
    private static final String DB_NAME = "drops_registry.db";
    private static final String TABLE_NAME = "block_drops";
    private boolean debugMode = false;

    public DropSystemHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the database connection and create table if needed.
     */
    public void initializeDatabase() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, DB_NAME);
        String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        dbConnection = DriverManager.getConnection(dbUrl);

        try (Statement stmt = dbConnection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "block_name TEXT PRIMARY KEY, " +
                    "item_id TEXT NOT NULL" +
                    ")");
        }
    }

    /**
     * Close the database connection.
     */
    public void closeDatabase() {
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close database: " + e.getMessage());
            }
        }
    }

    /**
     * Query the database for a block's custom item_id.
     */
    public String queryBlockDrop(String blockName) throws SQLException {
        String query = "SELECT item_id FROM " + TABLE_NAME + " WHERE block_name = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            pstmt.setString(1, blockName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("item_id");
                }
            }
        }
        return null;
    }

    /**
     * Insert or update a block drop entry.
     */
    public void insertOrUpdateBlockDrop(String blockName, String itemId) throws SQLException {
        String query = "INSERT OR REPLACE INTO " + TABLE_NAME + " (block_name, item_id) VALUES (?, ?)";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            pstmt.setString(1, blockName);
            pstmt.setString(2, itemId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Delete a block drop entry.
     */
    public void deleteBlockDrop(String blockName) throws SQLException {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE block_name = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            pstmt.setString(1, blockName);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieve all block drop entries.
     */
    public List<String[]> getAllBlockDrops() throws SQLException {
        List<String[]> entries = new ArrayList<>();
        String query = "SELECT block_name, item_id FROM " + TABLE_NAME + " ORDER BY block_name";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                entries.add(new String[]{
                        rs.getString("block_name"),
                        rs.getString("item_id")
                });
            }
        }

        return entries;
    }

    /**
     * Retrieve all registered block names from database.
     */
    public List<String> getAllBlockDropsNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String query = "SELECT block_name FROM " + TABLE_NAME + " ORDER BY block_name";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                names.add(rs.getString("block_name"));
            }
        }

        return names;
    }

    /**
     * Calculate the native fortune amount from block drops.
     */
    public int calculateFortuneAmount(Block block, ItemStack tool, Player player) {
        if (tool != null && (tool.containsEnchantment(Enchantment.SILK_TOUCH) ||
                (tool.getItemMeta() != null && tool.getItemMeta().getLore() != null &&
                        tool.getItemMeta().getLore().toString().contains("Silk Touch")))) {
            return 1;
        }

        int amount = block.getDrops(tool, player).stream()
                .mapToInt(ItemStack::getAmount)
                .max()
                .orElse(1);

        return Math.max(1, Math.min(64, amount));
    }

    /**
     * Resolve a configured item ID to a Bukkit Material if possible.
     */
    public Material resolveCustomMaterial(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        String normalized = itemId.trim().toUpperCase(Locale.ROOT)
                .replace("minecraft:", "")
                .replace("-", "_")
                .replace(" ", "_");

        return Material.matchMaterial(normalized);
    }

    /**
     * Get the Skript item value for a given item ID.
     */
    public Object getSkriptItemValue(String itemId) {
        try {
            Object itemsRegistry = Variables.getVariable("items::*", null, false);
            if (itemsRegistry instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) itemsRegistry;
                return itemMap.get(itemId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch Skript alias value for " + itemId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Hook into Skript variable {items::*} to fetch available item IDs from the registry.
     */
    public List<String> getSkriptItemIds() {
        List<String> itemIds = new ArrayList<>();

        try {
            Object itemsRegistry = Variables.getVariable("items::*", null, false);

            if (itemsRegistry != null) {
                if (itemsRegistry instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) itemsRegistry;
                    itemIds.addAll(itemMap.keySet());
                } else if (itemsRegistry instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> itemList = (List<Object>) itemsRegistry;
                    for (Object item : itemList) {
                        if (item != null) {
                            itemIds.add(item.toString());
                        }
                    }
                } else if (itemsRegistry instanceof String[]) {
                    String[] itemArray = (String[]) itemsRegistry;
                    itemIds.addAll(Arrays.asList(itemArray));
                }
            }

            itemIds.sort(String::compareTo);

            if (itemIds.isEmpty()) {
                plugin.getLogger().info("No items found in Skript registry {items::*}. Using fallback suggestions.");
                itemIds.addAll(Arrays.asList("custom_item_1", "custom_item_2", "custom_item_3"));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch Skript variable {items::*}: " + e.getMessage());
            itemIds.addAll(Arrays.asList("custom_item_1", "custom_item_2", "custom_item_3"));
        }

        return itemIds;
    }

    /**
     * Get available Minecraft block types for tab completion.
     */
    public List<String> getMinecraftBlockTypes() {
        List<String> blockTypes = new ArrayList<>();
        
        blockTypes.addAll(Arrays.asList(
            "deepslate_coal_ore", "deepslate_copper_ore", "deepslate_diamond_ore", 
            "deepslate_emerald_ore", "deepslate_gold_ore", "deepslate_iron_ore", 
            "deepslate_lapis_ore", "deepslate_redstone_ore",
            "stone_coal_ore", "stone_copper_ore", "stone_diamond_ore",
            "stone_emerald_ore", "stone_gold_ore", "stone_iron_ore",
            "stone_lapis_ore", "stone_redstone_ore",
            "coal_ore", "copper_ore", "diamond_ore", "emerald_ore",
            "gold_ore", "iron_ore", "lapis_ore", "redstone_ore",
            "nether_gold_ore", "nether_quartz_ore",
            "ancient_debris", "raw_copper_block", "raw_gold_block", "raw_iron_block"
        ));
        
        blockTypes.sort(String::compareTo);
        return blockTypes;
    }

    /**
     * Normalize block names (replace spaces/dashes with underscores).
     */
    public String normalizeBlockName(String blockName) {
        return blockName.toLowerCase().replace(" ", "_").replace("-", "_");
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
