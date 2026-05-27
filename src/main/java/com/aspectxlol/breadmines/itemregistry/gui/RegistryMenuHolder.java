package com.aspectxlol.breadmines.itemregistry.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class RegistryMenuHolder implements InventoryHolder {

    private final int page;

    public RegistryMenuHolder(int page) {
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