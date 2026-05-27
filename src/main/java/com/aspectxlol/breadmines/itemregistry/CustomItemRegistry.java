package com.aspectxlol.breadmines.itemregistry;

import com.aspectxlol.breadmines.Breadmines;
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

public class CustomItemRegistry implements CustomItemRegistryApi {

    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^a-z0-9]+", Pattern.UNICODE_CASE);

    private final Breadmines plugin;
    private final File storageFile;
    private final NamespacedKey registryKey;
    private final Map<String, CustomItemDefinition> definitions = new ConcurrentHashMap<>();

    public CustomItemRegistry(Breadmines plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "custom_item_registry.yml");
        this.registryKey = new NamespacedKey(plugin, "custom_item_id");
        load();
    }

    @Override
    public synchronized CustomItemDefinition registerItem(String name, ItemStack itemStack) {
        return registerItem(name, itemStack, "api");
    }

    public synchronized CustomItemDefinition registerItem(String name, ItemStack itemStack, String source) {
        String normalizedName = normalizeName(name);
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Item registry name cannot be empty");
        }

        ItemStack snapshot = itemStack.clone();
        String displayName = resolveDisplayName(snapshot, normalizedName);
        CustomItemDefinition definition = new CustomItemDefinition(normalizedName, displayName, snapshot, System.currentTimeMillis(), source);
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

        if (!storageFile.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection itemsSection = configuration.getConfigurationSection("items");
        if (itemsSection == null) {
            return;
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
    }

    public synchronized void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for item registry storage.");
            return;
        }

        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection itemsSection = configuration.createSection("items");

        for (CustomItemDefinition definition : getDefinitions()) {
            ConfigurationSection definitionSection = itemsSection.createSection(definition.getId());
            definitionSection.set("id", definition.getId());
            definitionSection.set("displayName", definition.getDisplayName());
            definitionSection.set("createdAtMillis", definition.getCreatedAtMillis());
            definitionSection.set("source", definition.getSource());
            definitionSection.set("item", definition.getItemStack().serialize());
        }

        try {
            configuration.save(storageFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save item registry: " + exception.getMessage());
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
}