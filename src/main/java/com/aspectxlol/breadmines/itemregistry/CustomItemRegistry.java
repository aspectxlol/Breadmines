package com.aspectxlol.breadmines.itemregistry;

import com.aspectxlol.breadmines.Breadmines;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CustomItemRegistry implements CustomItemRegistryApi {

    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^a-z0-9]+", Pattern.UNICODE_CASE);
    private static final String DEFAULT_STORAGE_FILE = "custom_item_registry.json";
    private static final String LEGACY_STORAGE_FILE = "custom_item_registry.yml";
    private static final String SECRETS_FILE = "secrets.yml";
    private static final String DEFAULT_GITHUB_BRANCH = "main";
    private static final String DEFAULT_GITHUB_PATH = "data/item_registry.json";
    private static final int GITHUB_TIMEOUT_MS = 10000;
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_USER_AGENT = "BreadminesRegistrySync";
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final Breadmines plugin;
    private final File storageFile;
    private final File legacyStorageFile;
    private final File secretsFile;
    private final NamespacedKey registryKey;
    private final Map<String, CustomItemDefinition> definitions = new ConcurrentHashMap<>();
    private final Gson gson;

    private final boolean githubEnabled;
    private final boolean githubSyncOnStartup;
    private final boolean githubSyncOnSave;
    private final String githubOwner;
    private final String githubRepo;
    private final String githubBranch;
    private final String githubPath;
    private final String githubToken;

    private volatile String lastSyncedSha;

    public CustomItemRegistry(Breadmines plugin) {
        this.plugin = plugin;
        String storageFileName = plugin.getConfig().getString("registry.storage.file", DEFAULT_STORAGE_FILE);
        this.storageFile = new File(plugin.getDataFolder(), storageFileName);
        this.legacyStorageFile = new File(plugin.getDataFolder(), LEGACY_STORAGE_FILE);
        this.secretsFile = new File(plugin.getDataFolder(), SECRETS_FILE);
        this.registryKey = new NamespacedKey(plugin, "custom_item_id");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        this.githubEnabled = plugin.getConfig().getBoolean("registry.github.enabled", false);
        this.githubSyncOnStartup = plugin.getConfig().getBoolean("registry.github.syncOnStartup", false);
        this.githubSyncOnSave = plugin.getConfig().getBoolean("registry.github.syncOnSave", false);
        this.githubOwner = sanitizeSegment(plugin.getConfig().getString("registry.github.owner", ""));
        this.githubRepo = sanitizeSegment(plugin.getConfig().getString("registry.github.repo", ""));
        this.githubBranch = sanitizeSegment(plugin.getConfig().getString("registry.github.branch", DEFAULT_GITHUB_BRANCH));
        this.githubPath = sanitizePath(plugin.getConfig().getString("registry.github.path", DEFAULT_GITHUB_PATH));
        this.githubToken = loadGithubToken();
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
        save();
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
        save();
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

    @Override
    public synchronized boolean removeItem(String name) {
        String normalizedName = normalizeName(name);
        CustomItemDefinition removed = definitions.remove(normalizedName);
        if (removed == null) {
            return false;
        }

        save();
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
        if (name == null) {
            return "";
        }

        String normalized = NORMALIZE_PATTERN.matcher(name.trim().toLowerCase(Locale.ROOT)).replaceAll("_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized;
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
        return Optional.ofNullable(id);
    }

    public boolean isRegistryItem(ItemStack itemStack) {
        return getItemId(itemStack).isPresent();
    }

    public void load() {
        definitions.clear();

        boolean loaded = false;
        if (githubEnabled && githubSyncOnStartup) {
            GitHubFile remoteFile = fetchGithubFile();
            if (remoteFile != null && remoteFile.content != null && !remoteFile.content.isBlank()) {
                loaded = loadFromJson(remoteFile.content);
                if (loaded) {
                    lastSyncedSha = remoteFile.sha;
                    writeLocalJson(remoteFile.content);
                }
            }
        }

        if (!loaded && storageFile.exists()) {
            loaded = loadFromJsonFile(storageFile);
        }

        if (!loaded && legacyStorageFile.exists()) {
            loaded = loadFromLegacyYaml(legacyStorageFile);
            if (loaded) {
                save();
            }
        }
    }

    public synchronized void save() {
        if (!ensureDataFolder()) {
            return;
        }

        String json = buildJsonPayload();
        writeLocalJson(json);

        if (githubEnabled && githubSyncOnSave) {
            String payload = json;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> pushGithubFile(payload));
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
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);

        List<Map<String, Object>> items = new ArrayList<>();
        for (CustomItemDefinition definition : getDefinitions()) {
            items.add(definition.serialize());
        }
        root.put("items", items);

        return gson.toJson(root);
    }

    private boolean pushGithubFile(String json) {
        if (!isGithubConfigured()) {
            plugin.getLogger().warning("GitHub registry sync is enabled but repo settings are missing.");
            return false;
        }

        if (githubToken.isBlank()) {
            plugin.getLogger().warning("GitHub registry sync token missing; skipping push.");
            return false;
        }

        GitHubFile remoteFile = fetchGithubFile();
        if (remoteFile != null) {
            if (lastSyncedSha == null || !lastSyncedSha.equals(remoteFile.sha)) {
                if (!isSameJson(remoteFile.content, json)) {
                    plugin.getLogger().warning("Registry sync conflict detected. Remote file changed; skipping push.");
                    return false;
                }

                lastSyncedSha = remoteFile.sha;
                return true;
            }

            if (isSameJson(remoteFile.content, json)) {
                lastSyncedSha = remoteFile.sha;
                return true;
            }
        }

        String body = buildGithubPutPayload(json, remoteFile == null ? null : remoteFile.sha);
        GitHubResponse response = sendGithubRequest("PUT", buildGithubContentUrl(), githubToken, body);
        if (response == null) {
            return false;
        }

        if (response.status >= 200 && response.status < 300) {
            try {
                JsonObject jsonResponse = gson.fromJson(response.body, JsonObject.class);
                if (jsonResponse != null && jsonResponse.has("content")) {
                    JsonObject content = jsonResponse.getAsJsonObject("content");
                    if (content != null && content.has("sha")) {
                        lastSyncedSha = content.get("sha").getAsString();
                    }
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("Registry sync succeeded but could not parse response: " + exception.getMessage());
            }
            return true;
        }

        plugin.getLogger().warning("Registry sync failed (" + response.status + "): " + response.body);
        return false;
    }

    private GitHubFile fetchGithubFile() {
        if (!isGithubConfigured()) {
            return null;
        }

        String token = githubToken.isBlank() ? null : githubToken;
        GitHubResponse response = sendGithubRequest("GET", buildGithubContentUrl(), token, null);
        if (response == null) {
            return null;
        }

        if (response.status == 404) {
            return null;
        }

        if (response.status < 200 || response.status >= 300) {
            plugin.getLogger().warning("Registry sync fetch failed (" + response.status + "): " + response.body);
            return null;
        }

        try {
            JsonObject jsonResponse = gson.fromJson(response.body, JsonObject.class);
            if (jsonResponse == null) {
                return null;
            }

            String sha = getJsonString(jsonResponse, "sha");
            String contentEncoded = getJsonString(jsonResponse, "content");
            String encoding = getJsonString(jsonResponse, "encoding");
            if (contentEncoded == null || encoding == null) {
                return null;
            }

            String content = contentEncoded;
            if ("base64".equalsIgnoreCase(encoding)) {
                String cleaned = contentEncoded.replace("\n", "").replace("\r", "");
                content = new String(Base64.getDecoder().decode(cleaned), StandardCharsets.UTF_8);
            }

            return new GitHubFile(sha, content);
        } catch (Exception exception) {
            plugin.getLogger().warning("Registry sync parse failed: " + exception.getMessage());
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
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String responseBody = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new GitHubResponse(status, responseBody);
        } catch (IOException exception) {
            plugin.getLogger().warning("Registry sync request failed: " + exception.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildGithubPutPayload(String json, String sha) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", "Update custom item registry");
        payload.addProperty("content", Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
        payload.addProperty("branch", githubBranch);
        if (sha != null && !sha.isBlank()) {
            payload.addProperty("sha", sha);
        }
        return gson.toJson(payload);
    }

    private String buildGithubContentUrl() {
        String encodedPath = githubPath.replace(" ", "%20");
        String encodedBranch = githubBranch.replace(" ", "%20");
        return GITHUB_API_BASE + "/repos/" + githubOwner + "/" + githubRepo + "/contents/" + encodedPath + "?ref=" + encodedBranch;
    }

    private boolean isGithubConfigured() {
        return githubEnabled && !githubOwner.isBlank() && !githubRepo.isBlank() && !githubPath.isBlank();
    }

    private String loadGithubToken() {
        String tokenFromSecrets = readTokenFromSecretsFile();
        if (tokenFromSecrets != null && !tokenFromSecrets.isBlank()) {
            return tokenFromSecrets.trim();
        }

        String tokenFromConfig = plugin.getConfig().getString("registry.github.token", "");
        if (tokenFromConfig != null && !tokenFromConfig.isBlank()) {
            plugin.getLogger().warning("GitHub token is set in config.yml. Move it to secrets.yml to keep it private.");
        }
        return tokenFromConfig == null ? "" : tokenFromConfig.trim();
    }

    private String readTokenFromSecretsFile() {
        if (!secretsFile.exists()) {
            return "";
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(secretsFile);
        return configuration.getString("registry.github.token", "");
    }

    private String sanitizeSegment(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String sanitizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String getJsonString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return null;
        }
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        return value.getAsString();
    }

    private boolean isSameJson(String left, String right) {
        if (left == null || right == null) {
            return false;
        }

        try {
            JsonElement leftElement = gson.fromJson(left, JsonElement.class);
            JsonElement rightElement = gson.fromJson(right, JsonElement.class);
            if (leftElement == null || rightElement == null) {
                return left.equals(right);
            }
            return leftElement.equals(rightElement);
        } catch (Exception exception) {
            return left.equals(right);
        }
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

    private static final class GitHubFile {
        private final String sha;
        private final String content;

        private GitHubFile(String sha, String content) {
            this.sha = sha;
            this.content = content;
        }
    }

    private static final class GitHubResponse {
        private final int status;
        private final String body;

        private GitHubResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}
