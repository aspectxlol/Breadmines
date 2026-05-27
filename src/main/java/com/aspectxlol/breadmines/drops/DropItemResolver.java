package com.aspectxlol.breadmines.drops;

import com.aspectxlol.breadmines.itemregistry.CustomItemDefinition;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistryApi;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DropItemResolver {

    private final CustomItemRegistryApi itemRegistry;

    public DropItemResolver(CustomItemRegistryApi itemRegistry) {
        this.itemRegistry = itemRegistry;
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

    public Optional<ItemStack> resolveItemStack(String itemId, int amount) {
        if (itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }

        Optional<ItemStack> registryItem = itemRegistry.createItemStack(itemId).map(itemStack -> {
            itemStack.setAmount(Math.max(1, amount));
            return itemStack;
        });

        if (registryItem.isPresent()) {
            return registryItem;
        }

        Material material = resolveCustomMaterial(itemId);
        if (material == null) {
            return Optional.empty();
        }

        return Optional.of(new ItemStack(material, Math.max(1, amount)));
    }

    public List<String> getRegisteredItemIds() {
        List<String> itemIds = new ArrayList<>();

        for (CustomItemDefinition definition : itemRegistry.getDefinitions()) {
            itemIds.add(definition.getId());
        }

        itemIds.sort(String::compareTo);

        return itemIds;
    }
}
