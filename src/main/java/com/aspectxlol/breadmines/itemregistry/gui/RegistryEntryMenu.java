package com.aspectxlol.breadmines.itemregistry.gui;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.itemregistry.CustomItemDefinition;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RegistryEntryMenu {

    private static final int INVENTORY_SIZE = 27;

    static final int SLOT_INFO = 11;
    static final int SLOT_GET_ITEM = 13;
    static final int SLOT_UPDATE_ITEM = 15;
    static final int SLOT_BACK = 18;
    static final int SLOT_RENAME = 22;
    static final int SLOT_CLOSE = 26;

    private final CustomItemRegistry registry;

    public RegistryEntryMenu(Breadmines plugin) {
        this.registry = plugin.getCustomItemRegistry();
    }

    public void open(Player player, String registryKey, int returnPage, RegistrySortMode sortMode, String searchQuery) {
        Optional<CustomItemDefinition> definition = registry.getDefinition(registryKey);
        if (definition.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Registry item not found: " + registryKey);
            return;
        }

        player.openInventory(createInventory(definition.get(), returnPage, sortMode, searchQuery));
    }

    private Inventory createInventory(CustomItemDefinition definition, int returnPage, RegistrySortMode sortMode, String searchQuery) {
        String title = ChatColor.DARK_PURPLE + "Registry: " + ChatColor.AQUA + definition.getId();
        Inventory inventory = Bukkit.createInventory(new RegistryEntryMenuHolder(definition.getId(), returnPage, sortMode, searchQuery), INVENTORY_SIZE, title);
        fillBackground(inventory);
        placeContent(inventory, definition);
        return inventory;
    }

    private void placeContent(Inventory inventory, CustomItemDefinition definition) {
        inventory.setItem(SLOT_INFO, createDisplayItem(definition));
        inventory.setItem(SLOT_GET_ITEM, createButton(Material.CHEST, ChatColor.GREEN + "Get Item", ChatColor.GRAY + "Add to your inventory"));
        inventory.setItem(SLOT_UPDATE_ITEM, createButton(Material.ANVIL, ChatColor.YELLOW + "Update Item", ChatColor.GRAY + "Replace with held item"));
        inventory.setItem(SLOT_RENAME, createButton(Material.NAME_TAG, ChatColor.AQUA + "Rename Key", ChatColor.GRAY + "Change the registry key"));
        inventory.setItem(SLOT_BACK, createButton(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to registry"));
        inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close the menu"));
    }

    private ItemStack createDisplayItem(CustomItemDefinition definition) {
        ItemStack itemStack = definition.getItemStack();
        itemStack.setAmount(1);

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(0, ChatColor.GRAY + "Type: " + ChatColor.AQUA + itemStack.getType().name());
            lore.add(0, ChatColor.GRAY + "Name: " + ChatColor.AQUA + definition.getDisplayName());
            lore.add(0, ChatColor.GRAY + "Key: " + ChatColor.AQUA + definition.getId());
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createPane();
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private ItemStack createButton(Material material, String name, String loreLine) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(loreLine));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack createPane() {
        ItemStack itemStack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
