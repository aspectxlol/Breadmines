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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class RegistryMenu {

    private static final int INVENTORY_SIZE = 54;
    private static final int CONTENT_SLOTS = 45;

    private final CustomItemRegistry registry;

    public RegistryMenu(Breadmines plugin) {
        this.registry = plugin.getCustomItemRegistry();
    }

    public void open(Player player, int page) {
        player.openInventory(createBrowserInventory(page, RegistryItemFilter.ALL, RegistrySortMode.NAME_ASC));
    }

    public void open(Player player, int page, RegistryItemFilter filter, RegistrySortMode sortMode) {
        player.openInventory(createBrowserInventory(page, filter, sortMode));
    }

    public void openControls(Player player, int page, RegistryItemFilter filter, RegistrySortMode sortMode) {
        player.openInventory(createControlsInventory(page, filter, sortMode));
    }

    public Inventory createBrowserInventory(int page, RegistryItemFilter filter, RegistrySortMode sortMode) {
        List<CustomItemDefinition> definitions = getFilteredDefinitions(filter, sortMode);
        int totalPages = Math.max(1, (int) Math.ceil(definitions.size() / (double) CONTENT_SLOTS));
        int safePage = Math.max(1, Math.min(page, totalPages));

        Inventory inventory = Bukkit.createInventory(new RegistryMenuHolder(RegistryMenuView.BROWSER, safePage, filter, sortMode), INVENTORY_SIZE, buildTitle(safePage, totalPages, filter, sortMode));
        fillBackground(inventory);
        placeItems(inventory, definitions, safePage);
        placeControls(inventory, safePage, totalPages);
        return inventory;
    }

    public Inventory createControlsInventory(int page, RegistryItemFilter filter, RegistrySortMode sortMode) {
        List<CustomItemDefinition> definitions = getFilteredDefinitions(filter, sortMode);
        int totalPages = Math.max(1, (int) Math.ceil(definitions.size() / (double) CONTENT_SLOTS));
        int safePage = Math.max(1, Math.min(page, totalPages));

        Inventory inventory = Bukkit.createInventory(new RegistryMenuHolder(RegistryMenuView.CONTROLS, safePage, filter, sortMode), INVENTORY_SIZE, ChatColor.DARK_PURPLE + "Search & Sort");
        fillBackground(inventory);
        placeControlsMenu(inventory, safePage, totalPages, filter, sortMode);
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
            CustomItemDefinition definition = definitions.get(index);
            ItemStack displayItem = definition.getItemStack();
            displayItem.setAmount(1);

            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(0, ChatColor.DARK_GRAY + "Registry Key: " + ChatColor.AQUA + definition.getId());
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }

            inventory.setItem(slot++, displayItem);
        }

        if (definitions.isEmpty()) {
            inventory.setItem(22, createInfoItem(ChatColor.YELLOW + "No custom items registered"));
        }
    }

    public CustomItemDefinition getDefinitionAt(int page, int rawSlot, RegistryItemFilter filter, RegistrySortMode sortMode) {
        if (rawSlot < 0 || rawSlot >= CONTENT_SLOTS) {
            return null;
        }

        List<CustomItemDefinition> definitions = getFilteredDefinitions(filter, sortMode);
        int totalPages = Math.max(1, (int) Math.ceil(definitions.size() / (double) CONTENT_SLOTS));
        int safePage = Math.max(1, Math.min(page, totalPages));
        int index = ((safePage - 1) * CONTENT_SLOTS) + rawSlot;

        if (index < 0 || index >= definitions.size()) {
            return null;
        }

        return definitions.get(index);
    }

    private void placeControls(Inventory inventory, int page, int totalPages) {
        inventory.setItem(45, createArrow(Material.ARROW, ChatColor.YELLOW + "Previous Page", page > 1 ? ChatColor.GRAY + "Go to page " + (page - 1) : ChatColor.DARK_GRAY + "No previous page"));
        inventory.setItem(46, createButton(Material.COMPASS, ChatColor.AQUA + "Search Menu", ChatColor.GRAY + "Open the search and filter controls"));
        inventory.setItem(47, createButton(Material.HOPPER, ChatColor.YELLOW + "Sort By", ChatColor.GRAY + "Cycle the current sort mode"));
        inventory.setItem(48, createButton(Material.CHEST, ChatColor.GREEN + "Filter Type", ChatColor.GRAY + "Cycle between weapon, armor, head, and item"));
        inventory.setItem(49, createInfoItem(ChatColor.GOLD + "Page " + page + ChatColor.GRAY + " / " + totalPages));
        inventory.setItem(50, createButton(Material.BOOK, ChatColor.RED + "Reset", ChatColor.GRAY + "Clear filters and sorting"));
        inventory.setItem(51, createPane());
        inventory.setItem(52, createButton(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close the registry menu"));
        inventory.setItem(53, createArrow(Material.ARROW, ChatColor.YELLOW + "Next Page", page < totalPages ? ChatColor.GRAY + "Go to page " + (page + 1) : ChatColor.DARK_GRAY + "No next page"));
    }

    private void placeControlsMenu(Inventory inventory, int page, int totalPages, RegistryItemFilter filter, RegistrySortMode sortMode) {
        inventory.setItem(10, createButton(Material.COMPASS, ChatColor.AQUA + "Search Menu", ChatColor.GRAY + "Browse and refine registry items"));
        inventory.setItem(11, createButton(Material.HOPPER, ChatColor.YELLOW + "Sort By", ChatColor.GRAY + sortMode.getDisplayName()));
        inventory.setItem(12, createButton(Material.CHEST, ChatColor.GREEN + "Filter Type", ChatColor.GRAY + filter.getDisplayName()));
        inventory.setItem(13, createButton(Material.PAPER, ChatColor.GOLD + "Current Page", ChatColor.GRAY + String.valueOf(page) + " / " + totalPages));
        inventory.setItem(19, createButton(Material.IRON_SWORD, ChatColor.RED + "Weapon", ChatColor.GRAY + "Filter weapon items"));
        inventory.setItem(20, createButton(Material.DIAMOND_CHESTPLATE, ChatColor.BLUE + "Armor", ChatColor.GRAY + "Filter armor items"));
        inventory.setItem(21, createButton(Material.PLAYER_HEAD, ChatColor.WHITE + "Player Heads", ChatColor.GRAY + "Filter player head items"));
        inventory.setItem(22, createButton(Material.PAPER, ChatColor.GREEN + "All Items", ChatColor.GRAY + "Show everything"));
        inventory.setItem(23, createButton(Material.BOOK, ChatColor.YELLOW + "Items Only", ChatColor.GRAY + "Filter non-weapon, non-armor, non-head items"));
        inventory.setItem(24, createButton(Material.RAW_IRON, ChatColor.AQUA + "Uncompressed Resource", ChatColor.GRAY + "Filter keys starting with uncompressed_"));
        inventory.setItem(25, createButton(Material.IRON_INGOT, ChatColor.AQUA + "Compressed Resource", ChatColor.GRAY + "Filter keys starting with compressed"));
        inventory.setItem(29, createButton(Material.NAME_TAG, ChatColor.YELLOW + "Name A-Z", ChatColor.GRAY + "Sort by name ascending"));
        inventory.setItem(30, createButton(Material.NAME_TAG, ChatColor.YELLOW + "Name Z-A", ChatColor.GRAY + "Sort by name descending"));
        inventory.setItem(31, createButton(Material.PAPER, ChatColor.AQUA + "Type A-Z", ChatColor.GRAY + "Sort by item type ascending"));
        inventory.setItem(32, createButton(Material.PAPER, ChatColor.AQUA + "Type Z-A", ChatColor.GRAY + "Sort by item type descending"));
        inventory.setItem(33, createButton(Material.CLOCK, ChatColor.LIGHT_PURPLE + "Newest", ChatColor.GRAY + "Sort by newest registered"));
        inventory.setItem(34, createButton(Material.CLOCK, ChatColor.LIGHT_PURPLE + "Oldest", ChatColor.GRAY + "Sort by oldest registered"));
        inventory.setItem(45, createArrow(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to the item browser"));
        inventory.setItem(49, createInfoItem(ChatColor.GOLD + "Controls"));
        inventory.setItem(52, createButton(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close the registry menu"));
        inventory.setItem(53, createArrow(Material.ARROW, ChatColor.YELLOW + "Next Page", page < totalPages ? ChatColor.GRAY + "Go to page " + (page + 1) : ChatColor.DARK_GRAY + "No next page"));
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createPane();
        for (int slot = CONTENT_SLOTS; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private String buildTitle(int page, int totalPages, RegistryItemFilter filter, RegistrySortMode sortMode) {
        return ChatColor.DARK_PURPLE + "Custom Items " + ChatColor.GRAY + "(" + page + "/" + totalPages + ") " + ChatColor.DARK_GRAY + filter.getDisplayName() + " | " + sortMode.getDisplayName();
    }

    private List<CustomItemDefinition> getFilteredDefinitions(RegistryItemFilter filter, RegistrySortMode sortMode) {
        List<CustomItemDefinition> definitions = new ArrayList<>(registry.getDefinitions());
        if (filter != null && filter != RegistryItemFilter.ALL) {
            definitions = definitions.stream()
                .filter(filter::matches)
                .collect(Collectors.toList());
        }

        Comparator<CustomItemDefinition> comparator = sortMode == null ? RegistrySortMode.NAME_ASC.getComparator() : sortMode.getComparator();
        definitions.sort(comparator);
        return definitions;
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