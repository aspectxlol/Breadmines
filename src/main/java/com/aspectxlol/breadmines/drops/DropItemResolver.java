package com.aspectxlol.breadmines.drops;

import ch.njol.skript.variables.Variables;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DropItemResolver {

    private final JavaPlugin plugin;

    public DropItemResolver(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Material resolveCustomMaterial(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        String normalized = itemId.trim().toUpperCase(Locale.ROOT)
                .replace("minecraft:", "")
                .replace("-", "_")
                .replace(" ", "_");

        return Material.matchMaterial(normalized);
    }

    public Object getSkriptItemValue(String itemId) {
        try {
            Object itemsRegistry = Variables.getVariable("items::*", null, false);
            if (itemsRegistry instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) itemsRegistry;
                return itemMap.get(itemId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch Skript alias value for " + itemId + ": " + e.getMessage());
        }
        return null;
    }

    public List<String> getSkriptItemIds() {
        List<String> itemIds = new ArrayList<>();

        try {
            Object itemsRegistry = Variables.getVariable("items::*", null, false);

            if (itemsRegistry != null) {
                if (itemsRegistry instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) itemsRegistry;
                    itemIds.addAll(itemMap.keySet());
                } else if (itemsRegistry instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> itemList = (List<Object>) itemsRegistry;
                    for (Object item : itemList) {
                        if (item != null) {
                            itemIds.add(item.toString());
                        }
                    }
                } else if (itemsRegistry instanceof String[]) {
                    String[] itemArray = (String[]) itemsRegistry;
                    itemIds.addAll(Arrays.asList(itemArray));
                }
            }

            itemIds.sort(String::compareTo);

            if (itemIds.isEmpty()) {
                plugin.getLogger().info("No items found in Skript registry {items::*}. Using fallback suggestions.");
                itemIds.addAll(Arrays.asList("custom_item_1", "custom_item_2", "custom_item_3"));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch Skript variable {items::*}: " + e.getMessage());
            itemIds.addAll(Arrays.asList("custom_item_1", "custom_item_2", "custom_item_3"));
        }

        return itemIds;
    }
}
