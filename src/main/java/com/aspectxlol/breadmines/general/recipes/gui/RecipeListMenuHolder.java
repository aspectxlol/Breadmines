package com.aspectxlol.breadmines.general.recipes.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class RecipeListMenuHolder implements InventoryHolder {

    private final int page;

    public RecipeListMenuHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
