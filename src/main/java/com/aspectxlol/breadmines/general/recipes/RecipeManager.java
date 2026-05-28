package com.aspectxlol.breadmines.general.recipes;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.general.recipes.storage.RecipeRepository;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RecipeManager {

    private static final String AUTO_COMPRESSOR_KEY = "auto_compressor";

    private final Breadmines plugin;
    private final CustomItemRegistry itemRegistry;
    private final RecipeRepository repository;
    private final Map<String, RecipeDefinition> recipes = new ConcurrentHashMap<>();
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
    private static final String GITHUB_USER_AGENT = "BreadminesRecipesSync";
    private static final int GITHUB_TIMEOUT_MS = 10000;

    public RecipeManager(Breadmines plugin) {
        this.plugin = plugin;
        this.itemRegistry = plugin.getCustomItemRegistry();
        this.repository = new RecipeRepository(plugin);
        com.aspectxlol.breadmines.config.GitHubConfig gh = new com.aspectxlol.breadmines.config.GitHubConfig(plugin, "recipes", "data/recipes.json");
        this.githubEnabled = gh.isEnabled();
        this.githubSyncOnStartup = gh.isSyncOnStartup();
        this.githubSyncOnSave = gh.isSyncOnSave();
        this.githubOwner = gh.getOwner();
        this.githubRepo = gh.getRepo();
        this.githubBranch = gh.getBranch();
        this.githubPath = gh.getPath();
        this.githubToken = gh.getToken();
    }

    public void initialize() throws SQLException {
        repository.initialize();
        load();
        if (githubEnabled && githubSyncOnStartup) {
            syncWithGithub();
        }
    }

    public void shutdown() {
        repository.close();
    }

    public synchronized void load() throws SQLException {
        recipes.clear();
        for (RecipeDefinition definition : repository.fetchAll()) {
            recipes.put(definition.getOutputKey(), definition);
        }
    }

    public synchronized RecipeDefinition createOrUpdateRecipe(String outputKey, int inputAmount, String inputKey) throws SQLException {
        String normalizedOutputKey = itemRegistry.normalizeName(outputKey);
        String normalizedInputKey = itemRegistry.normalizeName(inputKey);

        if (normalizedOutputKey.isEmpty() || normalizedInputKey.isEmpty()) {
            throw new IllegalArgumentException("Recipe output and input keys must not be empty.");
        }

        if (inputAmount <= 0) {
            throw new IllegalArgumentException("Input amount must be greater than 0.");
        }

        if (!itemRegistry.contains(normalizedOutputKey)) {
            throw new IllegalArgumentException("Unknown output item key: " + normalizedOutputKey);
        }

        if (!itemRegistry.contains(normalizedInputKey)) {
            throw new IllegalArgumentException("Unknown input item key: " + normalizedInputKey);
        }

        if (normalizedOutputKey.equals(normalizedInputKey) && inputAmount <= 1) {
            throw new IllegalArgumentException("Recipe would loop forever. Use input amount > 1 for same input/output key.");
        }

        long now = System.currentTimeMillis();
        RecipeDefinition existing = recipes.get(normalizedOutputKey);
        long createdAt = existing != null ? existing.getCreatedAtMillis() : now;

        RecipeDefinition recipe = new RecipeDefinition(normalizedOutputKey, normalizedInputKey, inputAmount, createdAt, now);
        repository.upsert(recipe);
        recipes.put(normalizedOutputKey, recipe);
        if (githubEnabled && githubSyncOnSave) {
            // push asynchronously
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> pushGithubFile(exportToJson(), null));
        }
        return recipe;
    }

    public synchronized boolean deleteRecipe(String outputKey) throws SQLException {
        String normalizedOutputKey = itemRegistry.normalizeName(outputKey);
        boolean removed = repository.delete(normalizedOutputKey);
        if (removed) {
            recipes.remove(normalizedOutputKey);
        }
        return removed;
    }

    public RecipeDefinition findRecipe(String outputKey) {
        return recipes.get(itemRegistry.normalizeName(outputKey));
    }

    public List<RecipeDefinition> getRecipes() {
        List<RecipeDefinition> list = new ArrayList<>(recipes.values());
        list.sort(Comparator.comparing(RecipeDefinition::getOutputKey));
        return list;
    }

    public List<String> getRecipeOutputKeys() {
        List<String> keys = new ArrayList<>(recipes.keySet());
        keys.sort(String::compareTo);
        return keys;
    }

    public void processAutoCompressors() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            PlayerInventory inventory = player.getInventory();
            if (!hasAutoCompressor(inventory)) {
                continue;
            }

            processPlayerRecipes(player, inventory);
        }
    }

    public synchronized boolean syncWithGithub() {
        if (!isGithubConfigured()) {
            plugin.getLogger().warning("Recipes GitHub sync not configured; skipping.");
            return false;
        }

        GitHubFile remote = fetchGithubFile();
        String localJson = exportToJson();

        if (remote == null) {
            return pushGithubFile(localJson, null);
        }

        if (lastSyncedSha != null && !lastSyncedSha.equals(remote.sha)) {
            if (!isSameJson(remote.content, localJson)) {
                plugin.getLogger().warning("Recipes registry sync conflict detected; remote changed. Skipping push.");
                return false;
            }
            lastSyncedSha = remote.sha;
            return true;
        }

        if (!isSameJson(remote.content, localJson)) {
            boolean imported = importFromJson(remote.content);
            if (imported) {
                lastSyncedSha = remote.sha;
                return true;
            }
            return pushGithubFile(localJson, remote.sha);
        }

        lastSyncedSha = remote.sha;
        return true;
    }

    private String exportToJson() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (RecipeDefinition r : getRecipes()) {
            JsonObject o = new JsonObject();
            o.addProperty("output_key", r.getOutputKey());
            o.addProperty("input_key", r.getInputKey());
            o.addProperty("input_amount", r.getInputAmount());
            o.addProperty("created_at_millis", r.getCreatedAtMillis());
            o.addProperty("updated_at_millis", r.getUpdatedAtMillis());
            arr.add(o);
        }
        root.add("recipes", arr);
        return gson.toJson(root);
    }

    private boolean importFromJson(String json) {
        if (json == null || json.isBlank()) return false;
        try {
            JsonElement root = gson.fromJson(json, JsonElement.class);
            if (root == null || root.isJsonNull()) return false;
            JsonArray arr = null;
            if (root.isJsonObject() && root.getAsJsonObject().has("recipes")) arr = root.getAsJsonObject().getAsJsonArray("recipes");
            else if (root.isJsonArray()) arr = root.getAsJsonArray();
            if (arr == null) return false;
            for (JsonElement e : arr) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                String output = o.has("output_key") ? o.get("output_key").getAsString() : null;
                String input = o.has("input_key") ? o.get("input_key").getAsString() : null;
                int amt = o.has("input_amount") ? o.get("input_amount").getAsInt() : 1;
                if (output == null || input == null) continue;
                RecipeDefinition def = new RecipeDefinition(output, input, amt, o.has("created_at_millis") ? o.get("created_at_millis").getAsLong() : System.currentTimeMillis(), o.has("updated_at_millis") ? o.get("updated_at_millis").getAsLong() : System.currentTimeMillis());
                repository.upsert(def);
                recipes.put(def.getOutputKey(), def);
            }
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to import recipes json: " + ex.getMessage());
            return false;
        }
    }

    private boolean pushGithubFile(String json, String sha) {
        if (!isGithubConfigured()) return false;
        if (githubToken == null || githubToken.isBlank()) return false;
        String body = buildGithubPutPayload(json, sha);
        GitHubResponse response = sendGithubRequest("PUT", buildGithubContentUrl(), githubToken, body);
        if (response == null) return false;
        if (response.status >= 200 && response.status < 300) {
            try {
                JsonObject jsonResponse = gson.fromJson(response.body, JsonObject.class);
                if (jsonResponse != null && jsonResponse.has("content")) {
                    JsonObject content = jsonResponse.getAsJsonObject("content");
                    if (content != null && content.has("sha")) lastSyncedSha = content.get("sha").getAsString();
                }
            } catch (Exception ignored) {}
            return true;
        }
        plugin.getLogger().warning("Recipes sync failed (" + response.status + "): " + response.body);
        return false;
    }

    private GitHubFile fetchGithubFile() {
        if (!isGithubConfigured()) return null;
        String token = githubToken == null || githubToken.isBlank() ? null : githubToken;
        GitHubResponse response = sendGithubRequest("GET", buildGithubContentUrl(), token, null);
        if (response == null) return null;
        if (response.status == 404) return null;
        if (response.status < 200 || response.status >= 300) {
            plugin.getLogger().warning("Recipes sync fetch failed (" + response.status + "): " + response.body);
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
            plugin.getLogger().warning("Recipes sync parse failed: " + e.getMessage());
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
            if (token != null && !token.isBlank()) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = connection.getOutputStream()) { out.write(body.getBytes(StandardCharsets.UTF_8)); }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String responseBody = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new GitHubResponse(status, responseBody);
        } catch (IOException e) {
            plugin.getLogger().warning("Recipes sync request failed: " + e.getMessage());
            return null;
        } finally { if (connection != null) connection.disconnect(); }
    }

    private String buildGithubPutPayload(String json, String sha) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", "Update recipes registry");
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

    private boolean isGithubConfigured() { return githubEnabled && githubOwner != null && !githubOwner.isBlank() && githubRepo != null && !githubRepo.isBlank() && githubPath != null && !githubPath.isBlank(); }

    private String loadGithubToken() {
        String token = plugin.getConfig().getString("recipes.github.token", "");
        if (token == null || token.isBlank()) {
            token = plugin.getConfig().getString("registry.github.token", "");
        }
        return token == null ? "" : token.trim();
    }

    private String getJsonString(JsonObject object, String key) { if (object == null || key == null || !object.has(key)) return null; JsonElement v = object.get(key); if (v == null || v.isJsonNull()) return null; return v.getAsString(); }

    private boolean isSameJson(String left, String right) { if (left == null || right == null) return false; try { JsonElement l = gson.fromJson(left, JsonElement.class); JsonElement r = gson.fromJson(right, JsonElement.class); if (l == null || r == null) return left.equals(right); return l.equals(r); } catch (Exception e) { return left.equals(right); } }

    private static final class GitHubFile { private final String sha; private final String content; private GitHubFile(String sha, String content) { this.sha = sha; this.content = content; } }
    private static final class GitHubResponse { private final int status; private final String body; private GitHubResponse(int status, String body) { this.status = status; this.body = body; } }

    private String sanitizeSegment(String value) { if (value == null) return ""; return value.trim(); }
    private String sanitizePath(String path) { if (path == null) return ""; String normalized = path.trim().replace("\\", "/"); while (normalized.startsWith("/")) normalized = normalized.substring(1); return normalized; }

    private boolean hasAutoCompressor(PlayerInventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (isRegistryKey(itemStack, AUTO_COMPRESSOR_KEY)) {
                return true;
            }
        }

        for (ItemStack armorItem : inventory.getArmorContents()) {
            if (isRegistryKey(armorItem, AUTO_COMPRESSOR_KEY)) {
                return true;
            }
        }

        return isRegistryKey(inventory.getItemInOffHand(), AUTO_COMPRESSOR_KEY);
    }

    private boolean isRegistryKey(ItemStack itemStack, String key) {
        if (itemStack == null) {
            return false;
        }

        return itemRegistry.getItemId(itemStack)
            .map(itemRegistry::normalizeName)
            .map(normalized -> normalized.equals(key))
            .orElse(false);
    }

    private void processPlayerRecipes(Player player, PlayerInventory inventory) {
        for (RecipeDefinition recipe : getRecipes()) {
            Optional<ItemStack> outputTemplate = itemRegistry.createItemStack(recipe.getOutputKey());
            if (outputTemplate.isEmpty()) {
                continue;
            }

            ItemStack output = outputTemplate.get();
            output.setAmount(1);

            int available = countItemsByRegistryKey(inventory, recipe.getInputKey());
            if (available < recipe.getInputAmount()) {
                continue;
            }

            while (available >= recipe.getInputAmount() && canFitOneOutput(inventory, output)) {
                if (!consumeItemsByRegistryKey(inventory, recipe.getInputKey(), recipe.getInputAmount())) {
                    break;
                }

                Map<Integer, ItemStack> leftovers = inventory.addItem(output.clone());
                if (!leftovers.isEmpty()) {
                    leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                }

                available -= recipe.getInputAmount();
            }
        }
    }

    private int countItemsByRegistryKey(PlayerInventory inventory, String key) {
        int total = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null) {
                continue;
            }

            if (isRegistryKey(stack, key)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private boolean consumeItemsByRegistryKey(PlayerInventory inventory, String key, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || !isRegistryKey(stack, key)) {
                continue;
            }

            int stackAmount = stack.getAmount();
            if (stackAmount <= remaining) {
                contents[slot] = null;
                remaining -= stackAmount;
            } else {
                stack.setAmount(stackAmount - remaining);
                contents[slot] = stack;
                remaining = 0;
            }

            if (remaining == 0) {
                inventory.setStorageContents(contents);
                return true;
            }
        }

        inventory.setStorageContents(contents);
        return false;
    }

    private boolean canFitOneOutput(PlayerInventory inventory, ItemStack output) {
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null) {
                return true;
            }

            if (stack.isSimilar(output) && stack.getAmount() < stack.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }
}
