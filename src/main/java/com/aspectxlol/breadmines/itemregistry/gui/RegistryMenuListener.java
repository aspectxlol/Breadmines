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

        int rawSlot = event.getRawSlot();
        int currentPage = holder.getPage();
        int totalPages = menu.getTotalPages();

        if (holder.getView() == RegistryMenuView.CONTROLS) {
            handleControlsClick(player, holder, rawSlot);
            return;
        }

        if (rawSlot == 45 && currentPage > 1) {
            menu.open(player, currentPage - 1, holder.getFilter(), holder.getSortMode());
            return;
        }

        if (rawSlot == 46) {
            menu.openControls(player, currentPage, holder.getFilter(), holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 47) {
            menu.open(player, 1, holder.getFilter(), holder.getSortMode().next(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 48) {
            menu.open(player, 1, holder.getFilter().next(), holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 50) {
            menu.open(player, 1, RegistryItemFilter.ALL, RegistrySortMode.NAME_ASC, null);
            return;
        }

        if (rawSlot == 53 && currentPage < totalPages) {
            menu.open(player, currentPage + 1, holder.getFilter(), holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 52) {
            player.closeInventory();
            return;
        }

        if (rawSlot < 0 || rawSlot >= 45) {
            return;
        }

        CustomItemDefinition definition = menu.getDefinitionAt(currentPage, rawSlot, holder.getFilter(), holder.getSortMode(), holder.getSearchQuery());
        if (definition == null) {
            return;
        }

        ItemStack itemStack = definition.getItemStack();
        itemStack.setAmount(1);

        for (ItemStack leftover : player.getInventory().addItem(itemStack).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() instanceof RegistryMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleControlsClick(Player player, RegistryMenuHolder holder, int rawSlot) {
        int currentPage = holder.getPage();

        if (rawSlot == 45) {
            menu.open(player, currentPage, holder.getFilter(), holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 19) {
            menu.open(player, 1, RegistryItemFilter.WEAPON, holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 20) {
            menu.open(player, 1, RegistryItemFilter.ARMOR, holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 21) {
            menu.open(player, 1, RegistryItemFilter.PLAYER_HEAD, holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 22) {
            menu.open(player, 1, RegistryItemFilter.ALL, holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 23) {
            menu.open(player, 1, RegistryItemFilter.ITEM, holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 29) {
            menu.open(player, 1, holder.getFilter(), RegistrySortMode.NAME_ASC, holder.getSearchQuery());
            return;
        }

        if (rawSlot == 30) {
            menu.open(player, 1, holder.getFilter(), RegistrySortMode.NAME_DESC, holder.getSearchQuery());
            return;
        }

        if (rawSlot == 31) {
            menu.open(player, 1, holder.getFilter(), RegistrySortMode.TYPE_ASC, holder.getSearchQuery());
            return;
        }

        if (rawSlot == 32) {
            menu.open(player, 1, holder.getFilter(), RegistrySortMode.TYPE_DESC, holder.getSearchQuery());
            return;
        }

        if (rawSlot == 33) {
            menu.open(player, 1, holder.getFilter(), RegistrySortMode.NEWEST, holder.getSearchQuery());
            return;
        }

        if (rawSlot == 34) {
            menu.open(player, 1, holder.getFilter(), RegistrySortMode.OLDEST, holder.getSearchQuery());
            return;
        }

        if (rawSlot == 49) {
            menu.open(player, currentPage, holder.getFilter(), holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        if (rawSlot == 52) {
            player.closeInventory();
            return;
        }

        if (rawSlot == 53) {
            menu.open(player, currentPage, holder.getFilter(), holder.getSortMode(), holder.getSearchQuery());
        }
    }
}