package com.aspectxlol.breadmines.itemregistry;

import com.aspectxlol.breadmines.Breadmines;
import com.google.gson.Gson;
import com.aspectxlol.breadmines.util.JsonSerializationHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import com.aspectxlol.breadmines.util.GitHubClient;
import com.aspectxlol.breadmines.util.GitHubSyncer;

public class CustomItemRegistry implements CustomItemRegistryApi {

    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^a-z0-9]+", Pattern.UNICODE_CASE);
    private static final String DEFAULT_STORAGE_FILE = "custom_item_registry.json";
    private static final String LEGACY_STORAGE_FILE = "custom_item_registry.yml";
    private static final String SECRETS_FILE = "secrets.yml";
    private static final String DEFAULT_GITHUB_BRANCH = "main";
    private static final String DEFAULT_GITHUB_PATH = "data/item_registry.json";
    
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final Breadmines plugin;
    private final File storageFile;
    private final File legacyStorageFile;
    private final File secretsFile;
    private final NamespacedKey registryKey;
    private final Map<String, CustomItemDefinition> definitions = new ConcurrentHashMap<>();
    private final Gson gson;

    private final com.aspectxlol.breadmines.config.GitHubConfig githubConfig;
    private volatile String lastSyncedSha;
    private final GitHubClient githubClient;

    private enum RegistrySyncSource { NONE, GITHUB, LOCAL, LEGACY }

    public static final class RegistrySyncResult {
        public final boolean loaded;
        public final String sourceName;
        public final int count;

        private RegistrySyncResult(boolean loaded, String sourceName, int count) {
            this.loaded = loaded;
            this.sourceName = sourceName;
            this.count = count;
        }
    }

    public CustomItemRegistry(Breadmines plugin) {
        this.plugin = plugin;
        String storageFileName = plugin.getConfig().getString("registry.storage.file", DEFAULT_STORAGE_FILE);
        this.storageFile = new File(plugin.getDataFolder(), storageFileName);
        this.legacyStorageFile = new File(plugin.getDataFolder(), LEGACY_STORAGE_FILE);
        this.secretsFile = new File(plugin.getDataFolder(), SECRETS_FILE);
        this.registryKey = new NamespacedKey(plugin, "custom_item_id");
        this.gson = JsonSerializationHelper.gson();

        com.aspectxlol.breadmines.config.GitHubConfig gh = new com.aspectxlol.breadmines.config.GitHubConfig(plugin, "registry", DEFAULT_GITHUB_PATH);
        this.githubConfig = gh;
        this.githubClient = new GitHubClient(plugin, gh.getOwner(), gh.getRepo(), gh.getBranch(), gh.getPath(), gh.getToken());
    }

    @Override
    public synchronized CustomItemDefinition registerItem(String name, ItemStack itemStack) {
        return registerItem(name, itemStack, "api");
    }

    @Override
    public synchronized CustomItemDefinition registerItemFromDisplayName(ItemStack itemStack) {
        return registerItemFromDisplayName(itemStack, "api");
    }

    public synchronized CustomItemDefinition registerItem(String name, ItemStack itemStack, String source) {
        String normalizedName = normalizeName(name);
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Item registry name cannot be empty");
        }

        if (definitions.containsKey(normalizedName)) {
            throw new IllegalArgumentException("Duplicate registry key: " + normalizedName);
        }

        ItemStack snapshot = itemStack.clone();
        String displayName = resolveDisplayName(snapshot, normalizedName);
        CustomItemDefinition definition = new CustomItemDefinition(normalizedName, displayName, snapshot, System.currentTimeMillis(), source);
        definitions.put(normalizedName, definition);
        save("Add registry key: " + normalizedName);
        return definition;
    }

    public synchronized CustomItemDefinition registerItemFromDisplayName(ItemStack itemStack, String source) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Held item is empty or invalid.");
        }

        if (!itemStack.hasItemMeta()) {
            throw new IllegalArgumentException("Item must have a display name to be registered.");
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            throw new IllegalArgumentException("Item must have a display name to be registered.");
        }

        String normalizedName = normalizeName(ChatColor.stripColor(meta.getDisplayName()));
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Item display name is problematic after sanitizing.");
        }

        if (definitions.containsKey(normalizedName)) {
            throw new IllegalArgumentException("Duplicate registry key: " + normalizedName);
        }

        ItemStack snapshot = itemStack.clone();
        CustomItemDefinition definition = new CustomItemDefinition(normalizedName, resolveDisplayName(snapshot, normalizedName), snapshot, System.currentTimeMillis(), source);
        definitions.put(normalizedName, definition);
        save("Add registry key: " + normalizedName);
        return definition;
    }

    @Override
    public Optional<CustomItemDefinition> getDefinition(String name) {
        return Optional.ofNullable(definitions.get(normalizeName(name)));
    }

    @Override
    public Optional<CustomItemDefinition> getDefinition(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return Optional.empty();
        }

        return getItemId(itemStack).flatMap(this::getDefinition);
    }

    @Override
    public Optional<ItemStack> createItemStack(String name) {
        return getDefinition(name).map(definition -> {
            ItemStack itemStack = definition.getItemStack();
            applyRegistryTag(itemStack, definition.getId());
            return itemStack;
        });
    }

    public synchronized Optional<CustomItemDefinition> updateItem(String name, ItemStack itemStack, String source) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Held item is empty or invalid.");
        }

        String normalizedName = normalizeName(name);
        CustomItemDefinition existing = definitions.get(normalizedName);
        if (existing == null) {
            return Optional.empty();
        }

        ItemStack snapshot = itemStack.clone();
        String displayName = resolveDisplayName(snapshot, normalizedName);
        String resolvedSource = source == null || source.isBlank() ? existing.getSource() : source;
        CustomItemDefinition updated = new CustomItemDefinition(existing.getId(), displayName, snapshot, existing.getCreatedAtMillis(), resolvedSource);
        definitions.put(normalizedName, updated);
        save("Update registry key: " + normalizedName);
        return Optional.of(updated);
    }

    public synchronized Optional<CustomItemDefinition> renameItem(String oldName, String newName) {
        String normalizedOldName = normalizeName(oldName);
        CustomItemDefinition existing = definitions.get(normalizedOldName);
        if (existing == null) {
            return Optional.empty();
        }

        String normalizedNewName = normalizeName(newName);
        if (normalizedNewName.isEmpty()) {
            throw new IllegalArgumentException("Registry key cannot be empty.");
        }

        if (normalizedOldName.equals(normalizedNewName)) {
            return Optional.of(existing);
        }

        if (definitions.containsKey(normalizedNewName)) {
            throw new IllegalArgumentException("Registry key already exists: " + normalizedNewName);
        }

        CustomItemDefinition renamed = new CustomItemDefinition(normalizedNewName, existing.getDisplayName(), existing.getItemStack(), existing.getCreatedAtMillis(), existing.getSource());
        definitions.remove(normalizedOldName);
        definitions.put(normalizedNewName, renamed);
        save("Rename registry key: " + normalizedOldName + " -> " + normalizedNewName);
        return Optional.of(renamed);
    }

    @Override
    public synchronized boolean removeItem(String name) {
        String normalizedName = normalizeName(name);
        CustomItemDefinition removed = definitions.remove(normalizedName);
        if (removed == null) {
            return false;
        }

        save("Remove registry key: " + normalizedName);
        return true;
    }

    @Override
    public List<CustomItemDefinition> getDefinitions() {
        TreeMap<String, CustomItemDefinition> sorted = new TreeMap<>(definitions);
        return Collections.unmodifiableList(new ArrayList<>(sorted.values()));
    }

    @Override
    public boolean contains(String name) {
        return definitions.containsKey(normalizeName(name));
    }

    @Override
    public String normalizeName(String name) {
        return com.aspectxlol.breadmines.util.NormalizationUtils.normalizeName(name);
    }

    public Optional<String> getItemId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = container.get(registryKey, PersistentDataType.STRING);
        if (id != null && !id.isBlank()) {
            return Optional.of(id);
        }

        String exactId = findDefinitionIdByExactStack(itemStack);
        if (exactId != null && !exactId.isBlank()) {
            return Optional.of(exactId);
        }

        String fallbackId = findDefinitionIdBySimilarity(itemStack);
        return Optional.ofNullable(fallbackId);
    }

    private String findDefinitionIdByExactStack(ItemStack itemStack) {
        ItemStack normalizedCandidate = itemStack.clone();
        normalizedCandidate.setAmount(1);

        for (CustomItemDefinition definition : definitions.values()) {
            ItemStack definitionStack = definition.getItemStack();
            if (definitionStack == null) {
                continue;
            }

            ItemStack normalizedDefinition = definitionStack.clone();
            normalizedDefinition.setAmount(1);
            if (normalizedCandidate.serialize().equals(normalizedDefinition.serialize())) {
                return definition.getId();
            }
        }

        return null;
    }

    private String findDefinitionIdBySimilarity(ItemStack itemStack) {
        for (CustomItemDefinition definition : definitions.values()) {
            ItemStack definitionStack = definition.getItemStack();
            if (definitionStack == null) {
                continue;
            }

            if (itemStack.isSimilar(definitionStack)) {
                return definition.getId();
            }
        }

        return null;
    }

    public boolean isRegistryItem(ItemStack itemStack) {
        return getItemId(itemStack).isPresent();
    }

    public synchronized void load() {
        syncInternal(githubConfig.isEnabled() && githubConfig.isSyncOnStartup());
    }

    public synchronized RegistrySyncResult syncNow() {
        return syncInternal(true);
    }

    private RegistrySyncResult syncInternal(boolean allowGithub) {
        definitions.clear();

        boolean loaded = false;
        RegistrySyncSource source = RegistrySyncSource.NONE;
        // Attempt GitHub sync/import first when allowed
        if (allowGithub && githubConfig.isEnabled()) {
            GitHubSyncer syncer = new GitHubSyncer(plugin, githubClient);
            GitHubSyncer.SyncResult res = syncer.sync(() -> RegistryJsonExporter.export(getDefinitions()), (json) -> {
                boolean ok = loadFromJson(json);
                if (ok) writeLocalJson(json);
                return ok;
            }, "Sync registry (initial push)", "Sync registry (push local after import failure)");

            if (res.importedRemote) {
                lastSyncedSha = res.remoteSha;
                loaded = true;
                source = RegistrySyncSource.GITHUB;
            }
            // if res.success && !importedRemote then either pushed or no-op; continue to local fallback
        }

        if (!loaded && storageFile.exists()) {
            loaded = loadFromJsonFile(storageFile);
            if (loaded) {
                source = RegistrySyncSource.LOCAL;
            }
        }

        if (!loaded && legacyStorageFile.exists()) {
            loaded = loadFromLegacyYaml(legacyStorageFile);
            if (loaded) {
                save("Migrate legacy registry");
                source = RegistrySyncSource.LEGACY;
            }
        }

        return new RegistrySyncResult(loaded, source.name(), definitions.size());
    }

    public synchronized void save() {
        save("Update custom item registry");
    }

    public synchronized void save(String commitMessage) {
        if (!ensureDataFolder()) {
            return;
        }

        String json = buildJsonPayload();
        writeLocalJson(json);

        if (githubConfig.isEnabled() && githubConfig.isSyncOnSave()) {
            String payload = json;
            String msg = commitMessage == null || commitMessage.isBlank() ? "Update custom item registry" : commitMessage;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                GitHubSyncer syncer = new GitHubSyncer(plugin, githubClient);
                syncer.pushLocal(() -> payload, msg);
            });
        }
    }

    private boolean ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for item registry storage.");
            return false;
        }
        return true;
    }

    private void writeLocalJson(String json) {
        if (!ensureDataFolder()) {
            return;
        }

        try {
            Files.writeString(storageFile.toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save item registry: " + exception.getMessage());
        }
    }

    private boolean loadFromJsonFile(File file) {
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return loadFromJson(json);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to read item registry: " + exception.getMessage());
            return false;
        }
    }

    private boolean loadFromJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }

        JsonElement root;
        try {
            root = gson.fromJson(json, JsonElement.class);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to parse registry json: " + exception.getMessage());
            return false;
        }

        if (root == null || root.isJsonNull()) {
            return false;
        }

        JsonArray items = null;
        if (root.isJsonArray()) {
            items = root.getAsJsonArray();
        } else if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            if (object.has("items") && object.get("items").isJsonArray()) {
                items = object.getAsJsonArray("items");
            }
        }

        if (items == null) {
            return false;
        }

        for (JsonElement element : items) {
            try {
                Map<String, Object> data = gson.fromJson(element, MAP_TYPE);
                if (data == null) {
                    continue;
                }

                CustomItemDefinition definition = CustomItemDefinition.deserialize(data);
                definitions.put(definition.getId(), definition);
            } catch (Exception exception) {
                plugin.getLogger().warning("Skipping invalid registry entry: " + exception.getMessage());
            }
        }

        return true;
    }

    private boolean loadFromLegacyYaml(File legacyFile) {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(legacyFile);
        ConfigurationSection itemsSection = configuration.getConfigurationSection("items");
        if (itemsSection == null) {
            return false;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection definitionSection = itemsSection.getConfigurationSection(key);
            if (definitionSection == null) {
                continue;
            }

            Map<String, Object> serialized = new LinkedHashMap<>();
            serialized.put("id", definitionSection.getString("id", key));
            serialized.put("displayName", definitionSection.getString("displayName", key));
            serialized.put("createdAtMillis", definitionSection.getLong("createdAtMillis", System.currentTimeMillis()));
            serialized.put("source", definitionSection.getString("source", "unknown"));

            ConfigurationSection itemSection = definitionSection.getConfigurationSection("item");
            if (itemSection == null) {
                continue;
            }

            serialized.put("item", itemSection.getValues(true));

            try {
                CustomItemDefinition definition = CustomItemDefinition.deserialize(serialized);
                definitions.put(definition.getId(), definition);
            } catch (Exception exception) {
                plugin.getLogger().warning("Skipping invalid registry entry '" + key + "': " + exception.getMessage());
            }
        }

        return true;
    }

    private String buildJsonPayload() {
        return RegistryJsonExporter.export(getDefinitions());
    }

    

    private String resolveDisplayName(ItemStack itemStack, String fallbackName) {
        if (!itemStack.hasItemMeta()) {
            return fallbackName;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return fallbackName;
        }

        return ChatColor.stripColor(meta.getDisplayName());
    }

    private void applyRegistryTag(ItemStack itemStack, String id) {
        if (!itemStack.hasItemMeta()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(registryKey, PersistentDataType.STRING, id);
        itemStack.setItemMeta(meta);
    }

    
}
