package com.aspectxlol.breadmines.itemregistry.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class RegistryMenuHolder implements InventoryHolder {

    private final RegistryMenuView view;
    private final int page;
    private final RegistryItemFilter filter;
    private final RegistrySortMode sortMode;

    public RegistryMenuHolder(RegistryMenuView view, int page, RegistryItemFilter filter, RegistrySortMode sortMode) {
        this.view = view;
        this.page = page;
        this.filter = filter;
        this.sortMode = sortMode;
    }

    public RegistryMenuView getView() {
        return view;
    }

    public int getPage() {
        return page;
    }

    public RegistryItemFilter getFilter() {
        return filter;
    }

    public RegistrySortMode getSortMode() {
        return sortMode;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}