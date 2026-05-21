package com.aspectxlol.breadmines;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Material;
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
import java.util.stream.Collectors;

public final class Breadmines extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private Connection dbConnection;
    private static final String DB_NAME = "drops_registry.db";
    private static final String TABLE_NAME = "block_drops";
    private boolean debugMode = false;

    @Override
    public void onEnable() {
        // Initialize database
        try {
            initializeDatabase();
            getLogger().info("✓ Database initialized successfully.");
        } catch (SQLException e) {
            getLogger().severe("✗ Failed to initialize database: " + e.getMessage());
            setEnabled(false);
            return;
        }

        // Register listeners and commands
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("drops").setExecutor(this);
        getCommand("drops").setTabCompleter(this);
        getCommand("dropslist").setExecutor(this);
        getCommand("dropslist").setTabCompleter(this);

        getLogger().info("CustomDrops Core has been enabled successfully!");
        getLogger().info("Injecting BlockBreakListener at HIGHEST priority...");
    }

    @Override
    public void onDisable() {
        // Close database connection
        if (dbConnection != null) {
            try {
                dbConnection.close();
                getLogger().info("✓ Database connection closed.");
            } catch (SQLException e) {
                getLogger().severe("✗ Failed to close database: " + e.getMessage());
            }
        }
        getLogger().info("CustomDrops Core has been disabled.");
    }

    /**
     * Initialize SQLite database connection and create table if needed.
     */
    private void initializeDatabase() throws SQLException {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, DB_NAME);
        String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        dbConnection = DriverManager.getConnection(dbUrl);

        // Create table if it doesn't exist
        try (Statement stmt = dbConnection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "block_name TEXT PRIMARY KEY, " +
                    "item_id TEXT NOT NULL" +
                    ")");
        }
    }

    /**
     * Block break event handler - intercepts and cancels vanilla drops.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String blockName = normalizeBlockName(block.getType().name().toLowerCase(Locale.ROOT));

        try {
            if (debugMode) {
                getLogger().info("[DEBUG] block broken=" + block.getType().name() + " normalized=" + blockName + " location=" + block.getLocation());
            }

            // Query database for custom item_id
            String itemId = queryBlockDrop(blockName);

            if (debugMode && itemId == null) {
                getLogger().info("[DEBUG] onBlockBreak no registered drop found for=" + blockName);
            }

            if (itemId != null) {
                Player player = event.getPlayer();
                ItemStack tool = player.getInventory().getItemInMainHand();

                // Calculate fortune amount
                int nativeFortuneAmount = calculateFortuneAmount(block, tool, player);

                if (debugMode) {
                    getLogger().info("[DEBUG] onBlockBreak registered=" + blockName
                            + " itemId=" + itemId
                            + " tool=" + (tool != null ? tool.getType() : "none")
                            + " fortune=" + nativeFortuneAmount);
                }

                // Cancel vanilla drops and prevent XP from dropping.
                event.setDropItems(false);
                event.setExpToDrop(0);

                // Write to Skript temporary variables for external Skript handling.
                Variables.setVariable("registry::temp::target_id", itemId, null, false);
                Variables.setVariable("registry::temp::fortune_amount", nativeFortuneAmount, null, false);

                Material customMaterial = resolveCustomMaterial(itemId);
                if (customMaterial != null) {
                    ItemStack customDrop = new ItemStack(customMaterial, Math.max(1, nativeFortuneAmount));
                    giveItemToPlayer(player, customDrop, block, blockName);
                } else {
                    Object skriptItem = getSkriptItemValue(itemId);
                    if (debugMode) {
                        getLogger().info("[DEBUG] Skript alias lookup=" + itemId + " -> "
                                + (skriptItem != null ? skriptItem.getClass().getName() : "null"));
                    }
                    if (skriptItem instanceof ItemStack) {
                        ItemStack customDrop = ((ItemStack) skriptItem).clone();
                        customDrop.setAmount(Math.max(1, nativeFortuneAmount));
                        giveItemToPlayer(player, customDrop, block, blockName);
                    } else {
                        getLogger().info("Block break intercepted: " + blockName + " -> " + itemId + " (Skript alias/custom item)");
                    }
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Error querying database: " + e.getMessage());
        }
    }

    /**
     * Query the database for a block's custom item_id.
     */
    private String queryBlockDrop(String blockName) throws SQLException {
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
     * Calculate the native fortune amount from block drops.
     */
    private int calculateFortuneAmount(Block block, ItemStack tool, Player player) {
        // Check for Silk Touch
        if (tool != null && (tool.containsEnchantment(Enchantment.SILK_TOUCH) ||
                (tool.getItemMeta() != null && tool.getItemMeta().getLore() != null &&
                        tool.getItemMeta().getLore().toString().contains("Silk Touch")))) {
            return 1;
        }

        // Calculate native drop amount
        int amount = block.getDrops(tool, player).stream()
                .mapToInt(ItemStack::getAmount)
                .max()
                .orElse(1);

        // Normalize between 1 and 64
        return Math.max(1, Math.min(64, amount));
    }

    /**
     * Resolve a configured item ID to a Bukkit Material if possible.
     */
    private Material resolveCustomMaterial(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        String normalized = itemId.trim().toUpperCase(Locale.ROOT)
                .replace("minecraft:", "")
                .replace("-", "_")
                .replace(" ", "_");

        return Material.matchMaterial(normalized);
    }

    private void giveItemToPlayer(Player player, ItemStack itemStack, Block block, String blockName) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        if (leftovers.isEmpty()) {
            getLogger().info("Added custom drop to inventory: " + itemStack.getType() + " x" + itemStack.getAmount() + " for block " + blockName);
        } else {
            leftovers.values().forEach(remaining -> block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), remaining));
            getLogger().warning("Inventory full for player " + player.getName() + "; dropped leftover custom drop for block " + blockName);
        }
    }

    private Object getSkriptItemValue(String itemId) {
        try {
            Object itemsRegistry = Variables.getVariable("items::*", null, false);
            if (itemsRegistry instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) itemsRegistry;
                return itemMap.get(itemId);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to fetch Skript alias value for " + itemId + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("admin.setup")) {
            sender.sendMessage("§c✗ You don't have permission to use this command.");
            return true;
        }

        if (label.equalsIgnoreCase("drops")) {
            return handleDropsCommand(sender, args);
        } else if (label.equalsIgnoreCase("dropslist")) {
            return handleDropsListCommand(sender, args);
        }

        return false;
    }

    /**
     * Handle /drops command: create/delete/read operations.
     */
    private boolean handleDropsCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/drops create <block_name> <item_id> - Create a new block drop");
            sender.sendMessage("§e/drops delete <block_name> - Delete a block drop");
            sender.sendMessage("§e/drops read <block_name> - Read a block drop entry");
            return true;
        }

        String action = args[0].toLowerCase();

        try {
            if (action.equals("create") || action.equals("set") || action.equals("update")) {
                if (args.length < 3) {
                    sender.sendMessage("§c✗ Usage: /drops create <block_name> <item_id>");
                    return true;
                }

                String blockName = normalizeBlockName(args[1]);
                String itemId = args[2];

                insertOrUpdateBlockDrop(blockName, itemId);
                sender.sendMessage("§a✓ Block drop registered: " + blockName + " -> " + itemId);

            } else if (action.equals("delete") || action.equals("remove")) {
                if (args.length < 2) {
                    sender.sendMessage("§c✗ Usage: /drops delete <block_name>");
                    return true;
                }

                String blockName = normalizeBlockName(args[1]);
                deleteBlockDrop(blockName);
                sender.sendMessage("§a✓ Block drop deleted: " + blockName);

            } else if (action.equals("read") || action.equals("info")) {
                if (args.length < 2) {
                    sender.sendMessage("§c✗ Usage: /drops read <block_name>");
                    return true;
                }

                String blockName = normalizeBlockName(args[1]);
                String itemId = queryBlockDrop(blockName);

                if (itemId != null) {
                    sender.sendMessage("§a✓ " + blockName + " -> " + itemId);
                } else {
                    sender.sendMessage("§c✗ No entry found for: " + blockName);
                }

            } else if (action.equals("debug")) {
                return handleDebugCommand(sender, args);
            } else {
                sender.sendMessage("§c✗ Unknown action: " + action);
            }

        } catch (SQLException e) {
            sender.sendMessage("§c✗ Database error: " + e.getMessage());
            getLogger().severe("SQL Error: " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle /drops debug command and debug output.
     */
    private boolean handleDebugCommand(CommandSender sender, String[] args) {
        try {
            if (args.length == 1) {
                debugMode = !debugMode;
                sender.sendMessage("§a✓ Debug mode " + (debugMode ? "enabled" : "disabled"));
                return true;
            }

            String debugAction = args[1].toLowerCase(Locale.ROOT);
            if (debugAction.equals("status")) {
                sender.sendMessage("§6=== Debug Status ===");
                sender.sendMessage("§aDebug mode: " + (debugMode ? "enabled" : "disabled"));
                sender.sendMessage("§aRegistered blocks: " + getAllBlockDrops().size());
                List<String> itemIds = getSkriptItemIds();
                sender.sendMessage("§aSkript item IDs loaded: " + itemIds.size());
                return true;
            }

            if (debugAction.equals("list")) {
                sender.sendMessage("§6=== Registered Block Drops ===");
                List<String[]> entries = getAllBlockDrops();
                if (entries.isEmpty()) {
                    sender.sendMessage("§cNo registered block drops.");
                    return true;
                }
                for (String[] entry : entries) {
                    sender.sendMessage("§a" + entry[0] + " -> " + entry[1]);
                }
                return true;
            }

            if (debugAction.equals("check")) {
                if (args.length < 3) {
                    sender.sendMessage("§c✗ Usage: /drops debug check <block_name>");
                    return true;
                }
                String blockName = normalizeBlockName(args[2]);
                String itemId = queryBlockDrop(blockName);
                sender.sendMessage("§6=== Debug Check: " + blockName + " ===");
                if (itemId == null) {
                    sender.sendMessage("§cNo registered drop for " + blockName);
                    return true;
                }
                sender.sendMessage("§aDatabase item_id: " + itemId);
                Material material = resolveCustomMaterial(itemId);
                sender.sendMessage("§aResolved Bukkit Material: " + (material != null ? material.name() : "none"));
                Object skriptItem = getSkriptItemValue(itemId);
                sender.sendMessage("§aSkript alias lookup value type: " + (skriptItem != null ? skriptItem.getClass().getSimpleName() : "none"));
                List<String> itemIds = getSkriptItemIds();
                sender.sendMessage("§aSkript item registry contains alias: " + itemIds.contains(itemId));
                return true;
            }

            sender.sendMessage("§e/drops debug - toggle debug mode");
            sender.sendMessage("§e/drops debug status - show debug status");
            sender.sendMessage("§e/drops debug list - list registered blocks");
            sender.sendMessage("§e/drops debug check <block_name> - inspect a registered block entry");
            return true;
        } catch (SQLException e) {
            sender.sendMessage("§c✗ Database error: " + e.getMessage());
            getLogger().severe("SQL Error in debug command: " + e.getMessage());
            return true;
        }
    }

    /**
     * Handle /dropslist command with pagination.
     */
    private boolean handleDropsListCommand(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage("§c✗ Invalid page number.");
                return true;
            }
        }

        try {
            List<String[]> entries = getAllBlockDrops();
            int entriesPerPage = 10;
            int totalPages = (int) Math.ceil((double) entries.size() / entriesPerPage);

            if (page > totalPages) {
                page = totalPages;
            }
            if (page < 1) {
                page = 1;
            }

            sender.sendMessage("§6=== Block Drops (Page " + page + "/" + totalPages + ") ===");

            int startIdx = (page - 1) * entriesPerPage;
            int endIdx = Math.min(startIdx + entriesPerPage, entries.size());

            for (int i = startIdx; i < endIdx; i++) {
                String[] entry = entries.get(i);
                sender.sendMessage("§a• " + entry[0] + " §7-> §b" + entry[1]);
            }

            if (totalPages > 1) {
                sender.sendMessage("§7Use /dropslist <page> to navigate");
            }

        } catch (SQLException e) {
            sender.sendMessage("§c✗ Database error: " + e.getMessage());
            getLogger().severe("SQL Error: " + e.getMessage());
        }

        return true;
    }

    /**
     * Normalize block names (replace spaces/dashes with underscores).
     */
    private String normalizeBlockName(String blockName) {
        return blockName.toLowerCase().replace(" ", "_").replace("-", "_");
    }

    /**
     * Insert or update a block drop entry.
     */
    private void insertOrUpdateBlockDrop(String blockName, String itemId) throws SQLException {
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
    private void deleteBlockDrop(String blockName) throws SQLException {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE block_name = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            pstmt.setString(1, blockName);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieve all block drop entries.
     */
    private List<String[]> getAllBlockDrops() throws SQLException {
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
     * Tab completion for /drops and /dropslist commands.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("admin.setup")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (label.equalsIgnoreCase("drops")) {
            if (args.length == 1) {
                // Suggest subcommands
                completions.addAll(Arrays.asList("create", "set", "update", "delete", "remove", "read", "info"));
            } else if (args.length == 2) {
                String action = args[0].toLowerCase();
                // Suggest existing block names for delete/read actions
                if (action.equals("delete") || action.equals("remove") || action.equals("read") || action.equals("info")) {
                    try {
                        List<String> blockNames = getAllBlockDropsNames();
                        completions.addAll(blockNames);
                    } catch (SQLException e) {
                        getLogger().warning("Error fetching block names for completion: " + e.getMessage());
                    }
                } else if (action.equals("create") || action.equals("set") || action.equals("update")) {
                    // Suggest available Minecraft block types for create/set/update
                    try {
                        completions.addAll(getMinecraftBlockTypes());
                    } catch (Exception e) {
                        getLogger().warning("Error fetching block types: " + e.getMessage());
                    }
                }
            } else if (args.length == 3) {
                String action = args[0].toLowerCase();
                // Suggest Skript item IDs for create/set/update actions
                if (action.equals("create") || action.equals("set") || action.equals("update")) {
                    try {
                        List<String> itemIds = getSkriptItemIds();
                        completions.addAll(itemIds);
                    } catch (Exception e) {
                        getLogger().warning("Error fetching Skript item IDs: " + e.getMessage());
                    }
                }
            }
        } else if (label.equalsIgnoreCase("dropslist")) {
            // No arguments needed for dropslist
            if (args.length == 1) {
                completions.add("[page]");
            }
        }

        // Filter completions based on current input
        String currentArg = args.length > 0 ? args[args.length - 1].toLowerCase() : "";
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all registered block names from database for tab completion.
     */
    private List<String> getAllBlockDropsNames() throws SQLException {
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
     * Get all available Minecraft block types for ore mining (tab completion).
     */
    private List<String> getMinecraftBlockTypes() {
        List<String> blockTypes = new ArrayList<>();
        
        // Ore blocks
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
     * Hook into Skript variable {items::*} to fetch available item IDs from the registry.
     * This retrieves all item ID keys from the Skript items registry.
     */
    private List<String> getSkriptItemIds() {
        List<String> itemIds = new ArrayList<>();

        try {
            // Fetch the entire {items::*} map from Skript
            Object itemsRegistry = Variables.getVariable("items::*", null, false);

            if (itemsRegistry != null) {
                // Handle Map/Dictionary structure (most common for {items::*})
                if (itemsRegistry instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> itemMap = (java.util.Map<String, Object>) itemsRegistry;
                    // Extract all the item ID keys
                    itemIds.addAll(itemMap.keySet());
                } else if (itemsRegistry instanceof List) {
                    // Handle List structure (alternative)
                    @SuppressWarnings("unchecked")
                    List<Object> itemList = (List<Object>) itemsRegistry;
                    for (Object item : itemList) {
                        if (item != null) {
                            itemIds.add(item.toString());
                        }
                    }
                } else if (itemsRegistry instanceof String[]) {
                    // Handle String array (alternative)
                    String[] itemArray = (String[]) itemsRegistry;
                    itemIds.addAll(Arrays.asList(itemArray));
                }
            }

            // Sort the item IDs for better usability
            itemIds.sort(String::compareTo);

            // If empty, add some default suggestions
            if (itemIds.isEmpty()) {
                getLogger().info("No items found in Skript registry {items::*}. Using fallback suggestions.");
                itemIds.addAll(Arrays.asList("custom_item_1", "custom_item_2", "custom_item_3"));
            } else {
                getLogger().info("Loaded " + itemIds.size() + " item IDs from Skript registry: " + itemIds);
            }

        } catch (Exception e) {
            getLogger().warning("Failed to fetch Skript variable {items::*}: " + e.getMessage());
            e.printStackTrace();
            // Fallback to default suggestions
            itemIds.addAll(Arrays.asList("custom_item_1", "custom_item_2", "custom_item_3"));
        }

        return itemIds;
    }
}
