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
     * Get the player's block drop multiplier from permissions.
     * Checks for breadmines.multiplier.X permissions where X is 20, 40, 60, 80, or 100.
     * Returns the multiplier value or 0 if no multiplier permission.
     */
    public int getPlayerMultiplier(Player player) {
        if (player.hasPermission("breadmines.multiplier.100")) return 100;
        if (player.hasPermission("breadmines.multiplier.80")) return 80;
        if (player.hasPermission("breadmines.multiplier.60")) return 60;
        if (player.hasPermission("breadmines.multiplier.40")) return 40;
        if (player.hasPermission("breadmines.multiplier.20")) return 20;
        return 0;
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

        // Apply permission-based multiplier
        int multiplier = getPlayerMultiplier(player);
        if (multiplier > 0) {
            if (multiplier == 100) {
                // Guaranteed extra drop
                amount *= 2;
            } else {
                // Percentage chance for extra drop
                if (Math.random() * 100 < multiplier) {
                    amount += amount; // Double the amount if lucky
                }
            }
        }

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
        
        // Ores
        blockTypes.addAll(Arrays.asList(
            "coal_ore", "deepslate_coal_ore",
            "copper_ore", "deepslate_copper_ore",
            "diamond_ore", "deepslate_diamond_ore",
            "emerald_ore", "deepslate_emerald_ore",
            "gold_ore", "deepslate_gold_ore",
            "iron_ore", "deepslate_iron_ore",
            "lapis_ore", "deepslate_lapis_ore",
            "redstone_ore", "deepslate_redstone_ore",
            "nether_quartz_ore", "nether_gold_ore", "ancient_debris"
        ));
        
        // Stone and variants
        blockTypes.addAll(Arrays.asList(
            "stone", "cobblestone", "mossy_cobblestone",
            "deepslate", "cobbled_deepslate", "mossy_deepslate",
            "granite", "diorite", "andesite",
            "tuff", "calcite", "dripstone_block"
        ));
        
        // Logs and leaves
        blockTypes.addAll(Arrays.asList(
            "oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log",
            "oak_leaves", "birch_leaves", "spruce_leaves", "jungle_leaves", "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "cherry_leaves",
            "stripped_oak_log", "stripped_birch_log", "stripped_spruce_log", "stripped_jungle_log", "stripped_acacia_log", "stripped_dark_oak_log", "stripped_mangrove_log", "stripped_cherry_log"
        ));
        
        // Planks, stairs, slabs
        blockTypes.addAll(Arrays.asList(
            "oak_planks", "birch_planks", "spruce_planks", "jungle_planks", "acacia_planks", "dark_oak_planks", "mangrove_planks", "cherry_planks",
            "oak_stairs", "birch_stairs", "spruce_stairs", "jungle_stairs", "acacia_stairs", "dark_oak_stairs", "mangrove_stairs", "cherry_stairs",
            "oak_slab", "birch_slab", "spruce_slab", "jungle_slab", "acacia_slab", "dark_oak_slab", "mangrove_slab", "cherry_slab"
        ));
        
        // Dirt, sand, gravel
        blockTypes.addAll(Arrays.asList(
            "dirt", "coarse_dirt", "rooted_dirt",
            "sand", "red_sand",
            "gravel",
            "grass_block", "dirt_path", "mycelium", "podzol"
        ));
        
        // Clay, concrete, terracotta
        blockTypes.addAll(Arrays.asList(
            "clay",
            "white_concrete", "orange_concrete", "magenta_concrete", "light_blue_concrete", "yellow_concrete", "lime_concrete", "pink_concrete", "gray_concrete", "light_gray_concrete", "cyan_concrete", "purple_concrete", "blue_concrete", "brown_concrete", "green_concrete", "red_concrete", "black_concrete",
            "white_terracotta", "orange_terracotta", "magenta_terracotta", "light_blue_terracotta", "yellow_terracotta", "lime_terracotta", "pink_terracotta", "gray_terracotta", "light_gray_terracotta", "cyan_terracotta", "purple_terracotta", "blue_terracotta", "brown_terracotta", "green_terracotta", "red_terracotta", "black_terracotta"
        ));
        
        // Bricks and stone bricks
        blockTypes.addAll(Arrays.asList(
            "bricks", "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks",
            "deepslate_bricks", "deepslate_tiles",
            "nether_bricks", "red_nether_bricks"
        ));
        
        // Sand, gravel, soul sand
        blockTypes.addAll(Arrays.asList(
            "soul_sand", "soul_soil"
        ));
        
        // Glass and variants
        blockTypes.addAll(Arrays.asList(
            "glass",
            "white_stained_glass", "orange_stained_glass", "magenta_stained_glass", "light_blue_stained_glass", "yellow_stained_glass", "lime_stained_glass", "pink_stained_glass", "gray_stained_glass", "light_gray_stained_glass", "cyan_stained_glass", "purple_stained_glass", "blue_stained_glass", "brown_stained_glass", "green_stained_glass", "red_stained_glass", "black_stained_glass"
        ));
        
        // Ice
        blockTypes.addAll(Arrays.asList(
            "ice", "packed_ice", "blue_ice"
        ));
        
        // Snow
        blockTypes.addAll(Arrays.asList(
            "snow_block"
        ));
        
        // Mushroom blocks
        blockTypes.addAll(Arrays.asList(
            "brown_mushroom_block", "red_mushroom_block"
        ));
        
        // Obsidian and crying obsidian
        blockTypes.addAll(Arrays.asList(
            "obsidian", "crying_obsidian"
        ));
        
        // Blackstone
        blockTypes.addAll(Arrays.asList(
            "blackstone", "polished_blackstone"
        ));
        
        // Raw metal blocks
        blockTypes.addAll(Arrays.asList(
            "raw_copper_block", "raw_gold_block", "raw_iron_block"
        ));
        
        // Metal blocks
        blockTypes.addAll(Arrays.asList(
            "copper_block", "exposed_copper", "weathered_copper", "oxidized_copper",
            "gold_block", "iron_block", "diamond_block", "emerald_block", "lapis_block", "redstone_block", "coal_block"
        ));
        
        // Prismarine
        blockTypes.addAll(Arrays.asList(
            "prismarine", "prismarine_bricks", "dark_prismarine"
        ));
        
        // Purpur
        blockTypes.addAll(Arrays.asList(
            "purpur_block", "purpur_pillar"
        ));
        
        // Amethyst
        blockTypes.addAll(Arrays.asList(
            "amethyst_block"
        ));
        
        // Waxed copper blocks
        blockTypes.addAll(Arrays.asList(
            "waxed_copper_block", "waxed_exposed_copper", "waxed_weathered_copper", "waxed_oxidized_copper"
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
