package com.aspectxlol.breadmines.general.recipes.gui;

import com.aspectxlol.breadmines.Breadmines;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class RecipeListMenuListener implements Listener {

    private final RecipeListMenu menu;

    public RecipeListMenuListener(Breadmines plugin) {
        this.menu = new RecipeListMenu(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof RecipeListMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        int page = holder.getPage();
        int totalPages = menu.getTotalPages();

        if (rawSlot == 45 && page > 1) {
            menu.open(player, page - 1);
            return;
        }

        if (rawSlot == 53 && page < totalPages) {
            menu.open(player, page + 1);
            return;
        }

        if (rawSlot == 52) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RecipeListMenuHolder) {
            event.setCancelled(true);
        }
    }
}
