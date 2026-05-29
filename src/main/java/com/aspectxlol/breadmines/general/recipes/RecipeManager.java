package com.aspectxlol.breadmines.general.recipes;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.general.recipes.storage.RecipeRepository;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import com.aspectxlol.breadmines.util.GitHubClient;
import com.aspectxlol.breadmines.util.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RecipeManager {

    private static final String AUTO_COMPRESSOR_KEY = "auto_compressor";
    // GitHub syncing is delegated to GitHubClient

    private final Breadmines plugin;
    private final CustomItemRegistry itemRegistry;
    private final RecipeRepository repository;
    private final Map<String, List<RecipeDefinition>> recipesByOutput = new HashMap<>();

    private final com.aspectxlol.breadmines.config.GitHubConfig githubConfig;
    private volatile String lastSyncedSha;
    private final GitHubClient githubClient;
    private final RecipeProcessor recipeProcessor;

    private boolean debugMode;

    public RecipeManager(Breadmines plugin) {
        this.plugin = plugin;
        this.itemRegistry = plugin.getCustomItemRegistry();
        this.repository = new RecipeRepository(plugin);

        com.aspectxlol.breadmines.config.GitHubConfig gh = new com.aspectxlol.breadmines.config.GitHubConfig(plugin, "recipes", "data/recipes.json");
        this.githubConfig = gh;
        this.githubClient = new GitHubClient(plugin, gh.getOwner(), gh.getRepo(), gh.getBranch(), gh.getPath(), gh.getToken());
        this.recipeProcessor = new RecipeProcessor(plugin, itemRegistry, false);
    }

    public void initialize() throws SQLException {
        repository.initialize();
        load();
        if (githubConfig.isEnabled() && githubConfig.isSyncOnStartup()) {
            syncWithGithub();
        }
    }

    public void shutdown() {
        repository.close();
    }

    public synchronized void load() throws SQLException {
        recipesByOutput.clear();
        for (RecipeDefinition definition : repository.fetchAll()) {
            addRecipeToCache(definition);
        }
    }

    public synchronized RecipeDefinition createOrUpdateRecipe(String outputKey, int inputAmount, String inputKey) throws SQLException {
        String normalizedOutputKey = normalizeKey(outputKey);
        String normalizedInputKey = normalizeKey(inputKey);

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
        RecipeDefinition existing = findExactRecipe(normalizedOutputKey, normalizedInputKey, inputAmount);
        long createdAt = existing != null ? existing.getCreatedAtMillis() : now;

        RecipeDefinition recipe = new RecipeDefinition(normalizedOutputKey, normalizedInputKey, inputAmount, createdAt, now);
        repository.upsert(recipe);
        replaceRecipeInCache(recipe);

        if (githubConfig.isEnabled() && githubConfig.isSyncOnSave()) {
            String message = (existing == null ? "Create recipe: " : "Update recipe: ")
                + normalizedOutputKey + " <= " + inputAmount + "x " + normalizedInputKey;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                com.aspectxlol.breadmines.util.GitHubSyncer syncer = new com.aspectxlol.breadmines.util.GitHubSyncer(plugin, githubClient);
                String payload = RecipeJsonSerializer.export(getRecipes());
                syncer.pushLocal(() -> payload, message);
            });
        }

        return recipe;
    }

    public synchronized boolean deleteRecipe(String outputKey) throws SQLException {
        String normalizedOutputKey = normalizeKey(outputKey);
        boolean removed = repository.delete(normalizedOutputKey);
        if (!removed) {
            return false;
        }

        recipesByOutput.remove(normalizedOutputKey);
        if (githubConfig.isEnabled() && githubConfig.isSyncOnSave()) {
            String message = "Delete recipe: " + normalizedOutputKey;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                com.aspectxlol.breadmines.util.GitHubSyncer syncer = new com.aspectxlol.breadmines.util.GitHubSyncer(plugin, githubClient);
                String payload = RecipeJsonSerializer.export(getRecipes());
                syncer.pushLocal(() -> payload, message);
            });
        }

        return true;
    }

    public synchronized boolean deleteRecipe(String outputKey, String inputKey, int inputAmount) throws SQLException {
        String normalizedOutputKey = normalizeKey(outputKey);
        String normalizedInputKey = normalizeKey(inputKey);

        boolean removed = repository.delete(normalizedOutputKey, normalizedInputKey, inputAmount);
        if (!removed) {
            return false;
        }

        List<RecipeDefinition> recipes = recipesByOutput.get(normalizedOutputKey);
        if (recipes != null) {
            recipes.removeIf(recipe -> recipe.getInputKey().equals(normalizedInputKey)
                && recipe.getInputAmount() == inputAmount);
            if (recipes.isEmpty()) {
                recipesByOutput.remove(normalizedOutputKey);
            }
        }

        if (githubConfig.isEnabled() && githubConfig.isSyncOnSave()) {
            String message = "Delete recipe: " + normalizedOutputKey + " <= " + inputAmount + "x " + normalizedInputKey;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                com.aspectxlol.breadmines.util.GitHubSyncer syncer = new com.aspectxlol.breadmines.util.GitHubSyncer(plugin, githubClient);
                String payload = RecipeJsonSerializer.export(getRecipes());
                syncer.pushLocal(() -> payload, message);
            });
        }

        return true;
    }

    public RecipeDefinition findRecipe(String outputKey) {
        return getRecipesForOutput(outputKey).stream().findFirst().orElse(null);
    }

    public synchronized List<RecipeDefinition> getRecipesForOutput(String outputKey) {
        List<RecipeDefinition> recipes = recipesByOutput.get(normalizeKey(outputKey));
        if (recipes == null || recipes.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecipeDefinition> copy = new ArrayList<>(recipes);
        copy.sort(recipeComparator());
        return Collections.unmodifiableList(copy);
    }

    public synchronized List<RecipeDefinition> getRecipes() {
        if (recipesByOutput.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecipeDefinition> allRecipes = new ArrayList<>();
        recipesByOutput.values().forEach(allRecipes::addAll);
        allRecipes.sort(recipeComparator());
        return Collections.unmodifiableList(allRecipes);
    }

    public synchronized List<String> getRecipeOutputKeys() {
        List<String> keys = new ArrayList<>(recipesByOutput.keySet());
        keys.sort(String::compareTo);
        return Collections.unmodifiableList(keys);
    }

    public boolean isGithubConfigured() {
        return githubClient.isConfigured();
    }

    public List<String> getGithubConfigurationIssues() {
        List<String> issues = new ArrayList<>();
        if (!githubConfig.isEnabled()) {
            issues.add("recipes.github.enabled is false");
        }
        if (githubConfig.getToken() == null || githubConfig.getToken().isBlank()) {
            issues.add("GitHub token is missing");
        }
        if (githubConfig.getOwner() == null || githubConfig.getOwner().isBlank()) {
            issues.add("GitHub owner is missing");
        }
        if (githubConfig.getRepo() == null || githubConfig.getRepo().isBlank()) {
            issues.add("GitHub repo is missing");
        }
        if (githubConfig.getBranch() == null || githubConfig.getBranch().isBlank()) {
            issues.add("GitHub branch is missing");
        }
        if (githubConfig.getPath() == null || githubConfig.getPath().isBlank()) {
            issues.add("GitHub path is missing");
        }
        return issues;
    }

    public List<String> buildInventoryDebugReport(Player player) {
        List<String> lines = new ArrayList<>();
        if (player == null) {
            lines.add("No player selected.");
            return lines;
        }

        PlayerInventory inventory = player.getInventory();
        boolean hasCompressor = hasAutoCompressor(inventory);

        lines.add("Player: " + player.getName());
        lines.add("Auto compressor present: " + (hasCompressor ? "yes" : "no"));
        lines.add("Total recipes loaded: " + getRecipes().size());

        if (!hasCompressor) {
            lines.add("No autocompressor found in inventory, armor, or offhand.");
            return lines;
        }

        for (RecipeDefinition recipe : getRecipes()) {
            int available = InventoryUtils.countItemsByRegistryKey(inventory, itemRegistry, recipe.getInputKey());
            boolean outputRegistered = itemRegistry.createItemStack(recipe.getOutputKey()).isPresent();
            boolean matches = available >= recipe.getInputAmount() && outputRegistered;
            lines.add(recipe.getOutputKey() + " <= " + recipe.getInputAmount() + "x " + recipe.getInputKey()
                + " | available=" + available
                + " | outputRegistered=" + (outputRegistered ? "yes" : "no")
                + " | " + (matches ? "MATCH" : "no match"));
        }

        return lines;
    }

    public synchronized boolean isDebugMode() {
        return debugMode;
    }

    public synchronized void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        if (this.recipeProcessor != null) this.recipeProcessor.setDebugMode(debugMode);
    }

    public void processAutoCompressors() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player == null) {
                continue;
            }

            PlayerInventory inventory = player.getInventory();
            boolean hasCompressor = recipeProcessor.hasAutoCompressor(inventory);
            if (debugMode) {
                plugin.getLogger().info("[RECIPE DEBUG] Tick scan for " + player.getName()
                    + ": autocompressor=" + (hasCompressor ? "yes" : "no")
                    + ", craftable=" + (recipeProcessor.hasAnyCraftableRecipe(inventory, getRecipes()) ? "yes" : "no"));
            }

            if (!hasCompressor) continue;

            recipeProcessor.processPlayerRecipes(player, inventory, getRecipes());
        }
    }

    public synchronized boolean syncWithGithub() {
        if (!githubConfig.isEnabled() || !githubClient.isConfigured()) {
            plugin.getLogger().warning("Recipes GitHub sync not configured; skipping.");
            return false;
        }

        com.aspectxlol.breadmines.util.GitHubSyncer syncer = new com.aspectxlol.breadmines.util.GitHubSyncer(plugin, githubClient);
        com.aspectxlol.breadmines.util.GitHubSyncer.SyncResult res = syncer.sync(
            () -> RecipeJsonSerializer.export(getRecipes()),
            (json) -> RecipeJsonSerializer.importIntoRepository(json, repository),
            "Sync recipes (initial push)",
            "Sync recipes (push local after import failure)"
        );

        if (res.importedRemote && res.remoteSha != null) {
            try {
                load();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to reload recipes after import: " + e.getMessage());
            }
        }

        if (!res.success) {
            plugin.getLogger().warning("Recipes sync failed or skipped.");
        }

        return res.success;
    }

    

    private void addRecipeToCache(RecipeDefinition definition) {
        recipesByOutput.computeIfAbsent(definition.getOutputKey(), k -> new ArrayList<>()).add(definition);
    }

    private void replaceRecipeInCache(RecipeDefinition definition) {
        List<RecipeDefinition> recipes = recipesByOutput.computeIfAbsent(definition.getOutputKey(), k -> new ArrayList<>());
        recipes.removeIf(recipe -> recipe.getInputKey().equals(definition.getInputKey())
            && recipe.getInputAmount() == definition.getInputAmount());
        recipes.add(definition);
        recipes.sort(recipeComparator());
    }

    private RecipeDefinition findExactRecipe(String outputKey, String inputKey, int inputAmount) {
        List<RecipeDefinition> recipes = recipesByOutput.get(outputKey);
        if (recipes == null) {
            return null;
        }
        return recipes.stream()
            .filter(recipe -> recipe.getInputKey().equals(inputKey) && recipe.getInputAmount() == inputAmount)
            .findFirst()
            .orElse(null);
    }

    private Comparator<RecipeDefinition> recipeComparator() {
        return Comparator
            .comparing(RecipeDefinition::getOutputKey)
            .thenComparing(RecipeDefinition::getInputKey)
            .thenComparingInt(RecipeDefinition::getInputAmount);
    }

    public boolean hasAutoCompressor(PlayerInventory inventory) {
        return InventoryUtils.hasAutoCompressor(inventory, itemRegistry);
    }

    

    private String normalizeKey(String key) {
        return itemRegistry.normalizeName(key == null ? "" : key);
    }

    
}
