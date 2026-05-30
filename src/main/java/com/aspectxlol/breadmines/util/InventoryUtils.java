package com.aspectxlol.breadmines.util;

import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;

import java.util.function.Consumer;

public final class InventoryUtils {
    private InventoryUtils() {}

    public static boolean hasAutoCompressor(PlayerInventory inventory, CustomItemRegistry registry) {
        // Use full contents here so special/extra inventory slots are also detected.
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && isRegistryKey(stack, registry, "auto_compressor")) return true;
        }
        for (ItemStack stack : inventory.getArmorContents()) {
            if (stack != null && isRegistryKey(stack, registry, "auto_compressor")) return true;
        }
        ItemStack off = inventory.getItemInOffHand();
        return off != null && isRegistryKey(off, registry, "auto_compressor");
    }

    public static int countItemsByRegistryKey(PlayerInventory inventory, CustomItemRegistry registry, String key) {
        int total = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack != null && isRegistryKey(stack, registry, key)) total += stack.getAmount();
        }
        for (ItemStack stack : inventory.getArmorContents()) {
            if (stack != null && isRegistryKey(stack, registry, key)) total += stack.getAmount();
        }
        ItemStack off = inventory.getItemInOffHand();
        if (off != null && isRegistryKey(off, registry, key)) total += off.getAmount();
        return total;
    }

    public static boolean consumeItemsByRegistryKey(PlayerInventory inventory, CustomItemRegistry registry, String key, int amount) {
        int remaining = amount;
        remaining = consumeFromArray(inventory.getStorageContents(), inventory::setStorageContents, registry, remaining, key);
        if (remaining == 0) return true;
        remaining = consumeFromArray(inventory.getArmorContents(), inventory::setArmorContents, registry, remaining, key);
        if (remaining == 0) return true;
        ItemStack off = inventory.getItemInOffHand();
        if (off != null && isRegistryKey(off, registry, key)) {
            int stackAmount = off.getAmount();
            if (stackAmount <= remaining) {
                inventory.setItemInOffHand(null);
                remaining -= stackAmount;
            } else {
                off.setAmount(stackAmount - remaining);
                inventory.setItemInOffHand(off);
                remaining = 0;
            }
        }
        return remaining == 0;
    }

    private static int consumeFromArray(ItemStack[] contents, Consumer<ItemStack[]> setter, CustomItemRegistry registry, int amount, String key) {
        int remaining = amount;
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !isRegistryKey(stack, registry, key)) continue;
            int stackAmount = stack.getAmount();
            if (stackAmount <= remaining) {
                contents[i] = null;
                remaining -= stackAmount;
            } else {
                stack.setAmount(stackAmount - remaining);
                contents[i] = stack;
                remaining = 0;
            }
            if (remaining == 0) {
                setter.accept(contents);
                return 0;
            }
        }
        setter.accept(contents);
        return remaining;
    }

    private static boolean isRegistryKey(ItemStack item, CustomItemRegistry registry, String key) {
        if (item == null) return false;
        String normalizedExpectedKey = registry.normalizeName(key);
        boolean byResolvedId = registry.getItemId(item)
            .map(registry::normalizeName)
            .map(n -> n.equals(normalizedExpectedKey))
            .orElse(false);
        if (byResolvedId) {
            return true;
        }

        return registry.getDefinition(normalizedExpectedKey)
            .map(definition -> relaxedDefinitionMatch(item, definition.getItemStack()))
            .orElse(false);
    }

    private static boolean relaxedDefinitionMatch(ItemStack candidate, ItemStack definition) {
        if (candidate == null || definition == null) {
            return false;
        }

        if (candidate.getType() != definition.getType()) {
            return false;
        }

        ItemMeta candidateMeta = candidate.hasItemMeta() ? candidate.getItemMeta() : null;
        ItemMeta definitionMeta = definition.hasItemMeta() ? definition.getItemMeta() : null;

        String candidateName = normalizeDisplayName(candidateMeta);
        String definitionName = normalizeDisplayName(definitionMeta);

        // Only plain items should fall back to material matching.
        if (candidateName.isEmpty() && definitionName.isEmpty()) {
            return true;
        }

        return candidateName.equals(definitionName);
    }

    private static String normalizeDisplayName(ItemMeta meta) {
        if (meta == null || !meta.hasDisplayName()) {
            return "";
        }

        String stripped = ChatColor.stripColor(meta.getDisplayName());
        return stripped == null ? "" : stripped.trim().toLowerCase();
    }
}
