package com.aspectxlol.breadmines.drops;

import com.aspectxlol.breadmines.drops.storage.DropRepository;
import com.aspectxlol.breadmines.drops.util.DropUtils;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistryApi;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import com.aspectxlol.breadmines.Breadmines;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // GitHub sync config
    private final boolean githubEnabled;
    private final boolean githubSyncOnStartup;
    private final boolean githubSyncOnSave;
    private final String githubOwner;
    private final String githubRepo;
    private final String githubBranch;
    private final String githubPath;
    private final String githubToken;
    private volatile String lastSyncedSha;
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_USER_AGENT = "BreadminesDropsSync";
    private static final int GITHUB_TIMEOUT_MS = 10000;

    public DropSystemHandler(Breadmines plugin, CustomItemRegistryApi itemRegistry) {
        this.plugin = plugin;
        this.repository = new DropRepository(plugin);
        this.itemResolver = new DropItemResolver(itemRegistry);
        this.multiplierService = new DropMultiplierService();
        com.aspectxlol.breadmines.config.GitHubConfig gh = new com.aspectxlol.breadmines.config.GitHubConfig(plugin, "drops", "data/drops_registry.json");
        this.githubEnabled = gh.isEnabled();
        this.githubSyncOnStartup = gh.isSyncOnStartup();
        this.githubSyncOnSave = gh.isSyncOnSave();
        this.githubOwner = gh.getOwner();
        this.githubRepo = gh.getRepo();
        this.githubBranch = gh.getBranch();
        this.githubPath = gh.getPath();
        this.githubToken = gh.getToken();
    }

    /**
     * Initialize the database connection and create table if needed.
     */
    public void initializeDatabase() throws SQLException {
        repository.initialize();
        if (githubEnabled && githubSyncOnStartup) {
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
        if (githubEnabled && githubSyncOnSave) {
            String msg = "Upsert drop: " + blockName;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> pushGithubFile(exportToJson(), null, msg));
        }
    }

    /**
     * Delete a block drop entry.
     */
    public void deleteBlockDrop(String blockName) throws SQLException {
        repository.delete(blockName);
        if (githubEnabled && githubSyncOnSave) {
            String msg = "Delete drop: " + blockName;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> pushGithubFile(exportToJson(), null, msg));
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

        GitHubFile remote = fetchGithubFile();
        String localJson = exportToJson();

        if (remote == null) {
            // No remote file — push local
            return pushGithubFile(localJson, null);
        }

        if (lastSyncedSha != null && !lastSyncedSha.equals(remote.sha)) {
            if (!isSameJson(remote.content, localJson)) {
                plugin.getLogger().warning("Drops registry sync conflict detected; remote changed. Skipping push.");
                return false;
            }
            lastSyncedSha = remote.sha;
            return true;
        }

        // If remote differs and we don't have a lastSyncedSha (or it equals), prefer remote -> import
        if (!isSameJson(remote.content, localJson)) {
            boolean imported = importFromJson(remote.content);
            if (imported) {
                lastSyncedSha = remote.sha;
                return true;
            }
            // If import failed, attempt to push local
            return pushGithubFile(localJson, remote.sha);
        }

        lastSyncedSha = remote.sha;
        return true;
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
        return gson.toJson(root);
    }

    private boolean importFromJson(String json) {
        if (json == null || json.isBlank()) return false;
        try {
            JsonElement root = gson.fromJson(json, JsonElement.class);
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

    private boolean pushGithubFile(String json, String sha) {
        return pushGithubFile(json, sha, "Update drops registry");
    }

    private boolean pushGithubFile(String json, String sha, String message) {
        if (!isGithubConfigured()) {
            plugin.getLogger().warning("Drops GitHub sync not configured; skipping push.");
            return false;
        }

        if (githubToken == null || githubToken.isBlank()) {
            plugin.getLogger().warning("Drops GitHub token missing; skipping push.");
            return false;
        }

        String body = buildGithubPutPayload(json, sha, message);
        GitHubResponse response = sendGithubRequest("PUT", buildGithubContentUrl(), githubToken, body);
        if (response == null) return false;
        if (response.status >= 200 && response.status < 300) {
            try {
                JsonObject jsonResponse = gson.fromJson(response.body, JsonObject.class);
                if (jsonResponse != null && jsonResponse.has("content")) {
                    JsonObject content = jsonResponse.getAsJsonObject("content");
                    if (content != null && content.has("sha")) {
                        lastSyncedSha = content.get("sha").getAsString();
                    }
                }
            } catch (Exception ignored) {}
            return true;
        }

        plugin.getLogger().warning("Drops sync failed (" + response.status + "): " + response.body);
        return false;
    }

    private GitHubFile fetchGithubFile() {
        if (!isGithubConfigured()) return null;
        String token = githubToken == null || githubToken.isBlank() ? null : githubToken;
        GitHubResponse response = sendGithubRequest("GET", buildGithubContentUrl(), token, null);
        if (response == null) return null;
        if (response.status == 404) return null;
        if (response.status < 200 || response.status >= 300) {
            plugin.getLogger().warning("Drops sync fetch failed (" + response.status + "): " + response.body);
            return null;
        }

        try {
            JsonObject jsonResponse = gson.fromJson(response.body, JsonObject.class);
            if (jsonResponse == null) return null;
            String sha = getJsonString(jsonResponse, "sha");
            String contentEncoded = null;
            if (jsonResponse.has("content")) contentEncoded = jsonResponse.get("content").getAsString();
            String encoding = getJsonString(jsonResponse, "encoding");
            if (contentEncoded == null || encoding == null) return null;
            String content = contentEncoded;
            if ("base64".equalsIgnoreCase(encoding)) {
                String cleaned = contentEncoded.replace("\n", "").replace("\r", "");
                content = new String(Base64.getDecoder().decode(cleaned), StandardCharsets.UTF_8);
            }
            return new GitHubFile(sha, content);
        } catch (Exception e) {
            plugin.getLogger().warning("Drops sync parse failed: " + e.getMessage());
            return null;
        }
    }

    private GitHubResponse sendGithubRequest(String method, String url, String token, String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(GITHUB_TIMEOUT_MS);
            connection.setReadTimeout(GITHUB_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", GITHUB_USER_AGENT);
            if (token != null && !token.isBlank()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String responseBody = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new GitHubResponse(status, responseBody);
        } catch (IOException e) {
            plugin.getLogger().warning("Drops sync request failed: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String buildGithubPutPayload(String json, String sha, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message == null || message.isBlank() ? "Update drops registry" : message);
        payload.addProperty("content", Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
        payload.addProperty("branch", githubBranch);
        if (sha != null && !sha.isBlank()) payload.addProperty("sha", sha);
        return gson.toJson(payload);
    }

    private String buildGithubContentUrl() {
        String encodedPath = githubPath.replace(" ", "%20");
        String encodedBranch = githubBranch.replace(" ", "%20");
        return GITHUB_API_BASE + "/repos/" + githubOwner + "/" + githubRepo + "/contents/" + encodedPath + "?ref=" + encodedBranch;
    }

    private boolean isGithubConfigured() {
        return githubEnabled && githubOwner != null && !githubOwner.isBlank() && githubRepo != null && !githubRepo.isBlank() && githubPath != null && !githubPath.isBlank();
    }

    private String loadGithubToken() {
        String token = plugin.getConfig().getString("drops.github.token", "");
        if (token == null || token.isBlank()) {
            token = plugin.getConfig().getString("registry.github.token", "");
        }
        return token == null ? "" : token.trim();
    }

    private String getJsonString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return null;
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return null;
        return value.getAsString();
    }

    private boolean isSameJson(String left, String right) {
        if (left == null || right == null) return false;
        try {
            JsonElement l = gson.fromJson(left, JsonElement.class);
            JsonElement r = gson.fromJson(right, JsonElement.class);
            if (l == null || r == null) return left.equals(right);
            return l.equals(r);
        } catch (Exception e) {
            return left.equals(right);
        }
    }

    private String sanitizeSegment(String value) {
        if (value == null) return "";
        return value.trim();
    }

    private String sanitizePath(String path) {
        if (path == null) return "";
        String normalized = path.trim().replace("\\", "/");
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }

    private static final class GitHubFile { private final String sha; private final String content; private GitHubFile(String sha, String content) { this.sha = sha; this.content = content; } }
    private static final class GitHubResponse { private final int status; private final String body; private GitHubResponse(int status, String body) { this.status = status; this.body = body; } }

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
