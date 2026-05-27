package com.aspectxlol.breadmines.itemregistry.gui;

import com.aspectxlol.breadmines.Breadmines;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

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

        if (rawSlot == 45 && currentPage > 1) {
            menu.open(player, currentPage - 1);
            return;
        }

        if (rawSlot == 53 && currentPage < totalPages) {
            menu.open(player, currentPage + 1);
            return;
        }

        if (rawSlot == 52) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() instanceof RegistryMenuHolder) {
            event.setCancelled(true);
        }
    }
}