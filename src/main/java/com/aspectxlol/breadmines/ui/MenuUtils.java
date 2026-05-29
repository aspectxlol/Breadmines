package com.aspectxlol.breadmines.ui;

import org.bukkit.inventory.Inventory;

public final class MenuUtils {
    private MenuUtils() {}

    public static void fillBackground(Inventory inventory, int contentSlots, int inventorySize) {
        var filler = MenuItemFactory.createPane();
        for (int slot = contentSlots; slot < inventorySize; slot++) {
            inventory.setItem(slot, filler);
        }
    }

    public static int calculateTotalPages(int totalItems, int contentSlots) {
        return Math.max(1, (int) Math.ceil(totalItems / (double) contentSlots));
    }

    public static int clampPage(int page, int totalPages) {
        return Math.max(1, Math.min(page, totalPages));
    }
}
