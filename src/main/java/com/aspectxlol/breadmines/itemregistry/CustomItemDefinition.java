package com.aspectxlol.breadmines.itemregistry;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CustomItemDefinition {

    private final String id;
    private final String displayName;
    private final ItemStack itemStack;
    private final long createdAtMillis;
    private final String source;

    public CustomItemDefinition(String id, String displayName, ItemStack itemStack, long createdAtMillis, String source) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack").clone();
        this.createdAtMillis = createdAtMillis;
        this.source = source == null ? "unknown" : source;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public String getSource() {
        return source;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("displayName", displayName);
        data.put("createdAtMillis", createdAtMillis);
        data.put("source", source);
        data.put("item", itemStack.serialize());
        return data;
    }

    @SuppressWarnings("unchecked")
    public static CustomItemDefinition deserialize(Map<String, Object> data) {
        String id = Objects.toString(data.get("id"), null);
        String displayName = Objects.toString(data.get("displayName"), null);
        Number createdAtMillis = (Number) data.getOrDefault("createdAtMillis", System.currentTimeMillis());
        String source = Objects.toString(data.get("source"), "unknown");
        Object rawItem = data.get("item");

        if (id == null || displayName == null || !(rawItem instanceof Map<?, ?> itemMap)) {
            throw new IllegalArgumentException("Invalid custom item definition data");
        }

        ItemStack itemStack = ItemStack.deserialize((Map<String, Object>) itemMap);
        return new CustomItemDefinition(id, displayName, itemStack, createdAtMillis.longValue(), source);
    }
}