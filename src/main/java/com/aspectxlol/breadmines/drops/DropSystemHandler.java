package com.aspectxlol.breadmines.drops;

import com.aspectxlol.breadmines.drops.storage.DropRepository;
import com.aspectxlol.breadmines.drops.util.DropUtils;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistryApi;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import com.aspectxlol.breadmines.Breadmines;

import java.io.IOException;
import com.aspectxlol.breadmines.util.GitHubClient;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.aspectxlol.breadmines.util.JsonSerializationHelper;
import com.aspectxlol.breadmines.util.GitHubSyncer;
import com.aspectxlol.breadmines.drops.DropJsonSerializer;

/**
 * DropSystemHandler - Manages the block drops registry and database operations.
 * Handles all database interactions, item resolution, and drop calculations.
 */
public class DropSystemHandler {

    private final Breadmines plugin;
    private final DropRepository repository;
    private final DropItemResolver itemResolver;
    private final DropMultiplierService multiplierService;
    private boolean debugMode = false;
    private final Set<UUID> miningDebugWatchers = ConcurrentHashMap.newKeySet();

    // GitHub sync config
    private final com.aspectxlol.breadmines.config.GitHubConfig githubConfig;
    private volatile String lastSyncedSha;
    private final GitHubClient githubClient;

    public DropSystemHandler(Breadmines plugin, CustomItemRegistryApi itemRegistry) {
        this.plugin = plugin;
        this.repository = new DropRepository(plugin);
        this.itemResolver = new DropItemResolver(itemRegistry);
        this.multiplierService = new DropMultiplierService();
        com.aspectxlol.breadmines.config.GitHubConfig gh = new com.aspectxlol.breadmines.config.GitHubConfig(plugin, "drops", "data/drops_registry.json");
        this.githubConfig = gh;
        this.githubClient = new GitHubClient(plugin, gh.getOwner(), gh.getRepo(), gh.getBranch(), gh.getPath(), gh.getToken());
    }

    /**
     * Initialize the database connection and create table if needed.
     */
    public void initializeDatabase() throws SQLException {
        repository.initialize();
        if (githubConfig.isEnabled() && githubConfig.isSyncOnStartup()) {
            syncWithGithub();
        }
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
        if (githubConfig.isEnabled() && githubConfig.isSyncOnSave()) {
            String msg = "Upsert drop: " + blockName;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> githubClient.pushFile(exportToJson(), null, msg));
        }
    }

    /**
     * Delete a block drop entry.
     */
    public void deleteBlockDrop(String blockName) throws SQLException {
        repository.delete(blockName);
        if (githubConfig.isEnabled() && githubConfig.isSyncOnSave()) {
            String msg = "Delete drop: " + blockName;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> githubClient.pushFile(exportToJson(), null, msg));
        }
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

    public Optional<ItemStack> resolveDropItem(String itemId, int amount) {
        return itemResolver.resolveItemStack(itemId, amount);
    }

    public List<String> getRegisteredItemIds() {
        return itemResolver.getRegisteredItemIds();
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

    public synchronized boolean syncWithGithub() {
        if (!isGithubConfigured()) {
            plugin.getLogger().warning("Drops GitHub sync not configured; skipping.");
            return false;
        }

        String localJson = exportToJson();
        return githubClient.pushFile(localJson, null, "Sync drops (push local)");
    }

    public synchronized boolean syncFromGithub() {
        if (!isGithubConfigured()) {
            plugin.getLogger().warning("Drops GitHub sync not configured; skipping.");
            return false;
        }

        GitHubClient.GitHubFile remote = githubClient.fetchFile();
        if (remote == null || remote.content == null || remote.content.isBlank()) {
            plugin.getLogger().warning("Drops GitHub pull failed: remote file not available.");
            return false;
        }

        try {
            boolean imported = importFromJson(remote.content);
            if (!imported) {
                plugin.getLogger().warning("Drops GitHub pull failed: could not import remote data.");
                return false;
            }

            lastSyncedSha = remote.sha;
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Drops GitHub pull failed: " + e.getMessage());
            return false;
        }
    }

    private String exportToJson() {
        JsonObject root = new JsonObject();
        JsonArray items = new JsonArray();
        try {
            List<String[]> entries = getAllBlockDrops();
            for (String[] entry : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("block_name", entry[0]);
                obj.addProperty("item_id", entry[1]);
                items.add(obj);
            }
        } catch (SQLException e) {
            // ignore, export empty
        }
        root.add("items", items);
        return JsonSerializationHelper.toJson(root);
    }

    private boolean importFromJson(String json) {
        if (json == null || json.isBlank()) return false;
        try {
            JsonElement root = JsonSerializationHelper.gson().fromJson(json, JsonElement.class);
            if (root == null || root.isJsonNull()) return false;
            JsonArray items = null;
            if (root.isJsonObject() && root.getAsJsonObject().has("items")) {
                items = root.getAsJsonObject().getAsJsonArray("items");
            } else if (root.isJsonArray()) {
                items = root.getAsJsonArray();
            }

            if (items == null) return false;

            // Replace DB contents: simple approach - delete all and insert each
            // Note: DropRepository doesn't have bulk replace; we'll upsert each and it's fine.
            for (JsonElement elem : items) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                String blockName = obj.has("block_name") ? obj.get("block_name").getAsString() : null;
                String itemId = obj.has("item_id") ? obj.get("item_id").getAsString() : null;
                if (blockName == null || itemId == null) continue;
                repository.upsert(blockName, itemId);
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to import drops json: " + e.getMessage());
            return false;
        }
    }

    

    private boolean isGithubConfigured() {
        return githubClient.isConfigured();
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

    public boolean isMiningDebugEnabled(Player player) {
        return player != null && miningDebugWatchers.contains(player.getUniqueId());
    }

    public boolean toggleMiningDebug(Player player) {
        if (player == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        if (miningDebugWatchers.contains(playerId)) {
            miningDebugWatchers.remove(playerId);
            return false;
        }

        miningDebugWatchers.add(playerId);
        return true;
    }
}
