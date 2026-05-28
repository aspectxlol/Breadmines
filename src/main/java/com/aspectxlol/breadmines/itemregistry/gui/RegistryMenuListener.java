package com.aspectxlol.breadmines.itemregistry.gui;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.itemregistry.CustomItemDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RegistryMenuListener implements Listener {

    private final RegistryMenu menu;

    public RegistryMenuListener(Breadmines plugin) {
        this.menu = new RegistryMenu(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof RegistryMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (holder.getView() == RegistryMenuView.CONTROLS) {
            handleControlsClick(player, holder, event.getRawSlot());
            return;
        }

        handleBrowserClick(player, holder, event.getRawSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() instanceof RegistryMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleBrowserClick(Player player, RegistryMenuHolder holder, int rawSlot) {
        int currentPage = holder.getPage();
        RegistrySortMode sortMode = holder.getSortMode();
        String searchQuery = holder.getSearchQuery();
        int totalPages = menu.getTotalPages(searchQuery);

        switch (rawSlot) {
            case RegistryMenu.SLOT_PREV_PAGE:
                if (currentPage > 1) {
                    menu.open(player, currentPage - 1, sortMode, searchQuery);
                }
                return;
            case RegistryMenu.SLOT_CONTROLS_MENU:
                menu.openControls(player, currentPage, sortMode, searchQuery);
                return;
            case RegistryMenu.SLOT_SORT_CYCLE:
                menu.open(player, 1, sortMode.next(), searchQuery);
                return;
            case RegistryMenu.SLOT_RESET:
                menu.open(player, 1, RegistrySortMode.NAME_ASC, null);
                return;
            case RegistryMenu.SLOT_NEXT_PAGE:
                if (currentPage < totalPages) {
                    menu.open(player, currentPage + 1, sortMode, searchQuery);
                }
                return;
            case RegistryMenu.SLOT_CLOSE:
                player.closeInventory();
                return;
            default:
                break;
        }

        if (rawSlot < 0 || rawSlot >= RegistryMenu.CONTENT_SLOTS) {
            return;
        }

        CustomItemDefinition definition = menu.getDefinitionAt(currentPage, rawSlot, sortMode, searchQuery);
        if (definition == null) {
            return;
        }

        ItemStack itemStack = definition.getItemStack();
        itemStack.setAmount(1);

        for (ItemStack leftover : player.getInventory().addItem(itemStack).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void handleControlsClick(Player player, RegistryMenuHolder holder, int rawSlot) {
        int currentPage = holder.getPage();
        RegistrySortMode sortMode = holder.getSortMode();
        String searchQuery = holder.getSearchQuery();
        int totalPages = menu.getTotalPages(searchQuery);

        switch (rawSlot) {
            case RegistryMenu.SLOT_PREV_PAGE:
                menu.open(player, currentPage, sortMode, searchQuery);
                return;
            case RegistryMenu.SLOT_SORT_NAME_ASC:
                menu.open(player, 1, RegistrySortMode.NAME_ASC, searchQuery);
                return;
            case RegistryMenu.SLOT_SORT_NAME_DESC:
                menu.open(player, 1, RegistrySortMode.NAME_DESC, searchQuery);
                return;
            case RegistryMenu.SLOT_SORT_TYPE_ASC:
                menu.open(player, 1, RegistrySortMode.TYPE_ASC, searchQuery);
                return;
            case RegistryMenu.SLOT_SORT_TYPE_DESC:
                menu.open(player, 1, RegistrySortMode.TYPE_DESC, searchQuery);
                return;
            case RegistryMenu.SLOT_SORT_NEWEST:
                menu.open(player, 1, RegistrySortMode.NEWEST, searchQuery);
                return;
            case RegistryMenu.SLOT_SORT_OLDEST:
                menu.open(player, 1, RegistrySortMode.OLDEST, searchQuery);
                return;
            case RegistryMenu.SLOT_NEXT_PAGE:
                if (currentPage < totalPages) {
                    menu.open(player, currentPage + 1, sortMode, searchQuery);
                }
                return;
            case RegistryMenu.SLOT_CLOSE:
                player.closeInventory();
                return;
            default:
                break;
        }
    }
}