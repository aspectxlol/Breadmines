package com.aspectxlol.breadmines.itemregistry.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class RegistryMenuHolder implements InventoryHolder {

    private final RegistryMenuView view;
    private final int page;
    private final RegistrySortMode sortMode;
    private final String searchQuery;

    public RegistryMenuHolder(RegistryMenuView view, int page, RegistrySortMode sortMode) {
        this(view, page, sortMode, null);
    }

    public RegistryMenuHolder(RegistryMenuView view, int page, RegistrySortMode sortMode, String searchQuery) {
        this.view = view;
        this.page = page;
        this.sortMode = sortMode;
        this.searchQuery = searchQuery;
    }

    public RegistryMenuView getView() {
        return view;
    }

    public int getPage() {
        return page;
    }

    public RegistrySortMode getSortMode() {
        return sortMode;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}