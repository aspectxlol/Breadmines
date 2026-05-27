package com.aspectxlol.breadmines.itemregistry;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.itemregistry.storage.ItemRegistryRepository;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CustomItemRegistry implements CustomItemRegistryApi {

    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^a-z0-9]+", Pattern.UNICODE_CASE);

    private final Breadmines plugin;
    private final NamespacedKey registryKey;
    private final ItemRegistryRepository repository;
    private final Map<String, CustomItemDefinition> definitions = new ConcurrentHashMap<>();

    public CustomItemRegistry(Breadmines plugin, ItemRegistryRepository repository) {
        this.plugin = plugin;
        this.registryKey = new NamespacedKey(plugin, "custom_item_id");
        this.repository = repository;
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
        persistDefinition(definition);
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

        removeDefinition(normalizedName);
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

        try {
            for (ItemRegistryRepository.RegistryRow row : repository.fetchAll()) {
                CustomItemDefinition definition = new CustomItemDefinition(row.registryKey(), row.displayName(), row.itemStack(), row.createdAtMillis(), row.source());
                definitions.put(definition.getId(), definition);
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to load custom item registry: " + exception.getMessage());
            return;
        }
    }

    public void save() {
        // No-op retained for lifecycle compatibility; data is persisted per change.
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

    private void persistDefinition(CustomItemDefinition definition) {
        try {
            repository.upsert(definition.getId(), definition.getDisplayName(), definition.getCreatedAtMillis(), definition.getSource(), definition.getItemStack());
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to save custom item registry entry '" + definition.getId() + "': " + exception.getMessage());
        }
    }

    private void removeDefinition(String id) {
        try {
            repository.delete(id);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to delete custom item registry entry '" + id + "': " + exception.getMessage());
        }
    }
}