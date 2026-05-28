package com.aspectxlol.breadmines.itemregistry.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class RegistryEntryMenuHolder implements InventoryHolder {

    private final String registryKey;
    private final int returnPage;
    private final RegistrySortMode sortMode;
    private final String searchQuery;

    public RegistryEntryMenuHolder(String registryKey, int returnPage, RegistrySortMode sortMode, String searchQuery) {
        this.registryKey = registryKey;
        this.returnPage = returnPage;
        this.sortMode = sortMode;
        this.searchQuery = searchQuery;
    }

    public String getRegistryKey() {
        return registryKey;
    }

    public int getReturnPage() {
        return returnPage;
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
