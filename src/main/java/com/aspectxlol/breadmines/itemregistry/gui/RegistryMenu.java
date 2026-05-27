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

public final class RegistryMenu {

    private static final int INVENTORY_SIZE = 54;
    private static final int CONTENT_SLOTS = 45;

    private final CustomItemRegistry registry;

    public RegistryMenu(Breadmines plugin) {
        this.registry = plugin.getCustomItemRegistry();
    }

    public void open(Player player, int page) {
        player.openInventory(createInventory(page));
    }

    public Inventory createInventory(int page) {
        List<CustomItemDefinition> definitions = new ArrayList<>(registry.getDefinitions());
        int totalPages = Math.max(1, (int) Math.ceil(definitions.size() / (double) CONTENT_SLOTS));
        int safePage = Math.max(1, Math.min(page, totalPages));

        Inventory inventory = Bukkit.createInventory(new RegistryMenuHolder(safePage), INVENTORY_SIZE, buildTitle(safePage, totalPages));
        fillBackground(inventory);
        placeItems(inventory, definitions, safePage);
        placeControls(inventory, safePage, totalPages);
        return inventory;
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil(registry.getDefinitions().size() / (double) CONTENT_SLOTS));
    }

    private void placeItems(Inventory inventory, List<CustomItemDefinition> definitions, int page) {
        int startIndex = (page - 1) * CONTENT_SLOTS;
        int endIndex = Math.min(definitions.size(), startIndex + CONTENT_SLOTS);

        int slot = 0;
        for (int index = startIndex; index < endIndex; index++) {
            inventory.setItem(slot++, definitions.get(index).getItemStack());
        }

        if (definitions.isEmpty()) {
            inventory.setItem(22, createInfoItem(ChatColor.YELLOW + "No custom items registered"));
        }
    }

    private void placeControls(Inventory inventory, int page, int totalPages) {
        inventory.setItem(45, createArrow(Material.ARROW, ChatColor.YELLOW + "Previous Page", page > 1 ? ChatColor.GRAY + "Go to page " + (page - 1) : ChatColor.DARK_GRAY + "No previous page"));
        inventory.setItem(49, createInfoItem(ChatColor.GOLD + "Page " + page + ChatColor.GRAY + " / " + totalPages));
        inventory.setItem(52, createButton(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close the registry menu"));
        inventory.setItem(53, createArrow(Material.ARROW, ChatColor.YELLOW + "Next Page", page < totalPages ? ChatColor.GRAY + "Go to page " + (page + 1) : ChatColor.DARK_GRAY + "No next page"));

        for (int slot : new int[] {46, 47, 48, 50, 51}) {
            inventory.setItem(slot, createPane());
        }
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createPane();
        for (int slot = CONTENT_SLOTS; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private String buildTitle(int page, int totalPages) {
        return ChatColor.DARK_PURPLE + "Custom Items " + ChatColor.GRAY + "(" + page + "/" + totalPages + ")";
    }

    private ItemStack createArrow(Material material, String name, String loreLine) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(loreLine));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack createInfoItem(String name) {
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
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