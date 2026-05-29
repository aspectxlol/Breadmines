package com.aspectxlol.breadmines.itemregistry.gui;

import org.bukkit.block.BlockState;
import org.bukkit.Location;

public final class PendingSign {
    public final Location location;
    public final BlockState previousState;
    public final PendingSignAction action;
    public final RegistrySortMode sortMode;
    public final String searchQuery;
    public final String registryKey;
    public final int returnPage;

    public PendingSign(Location location, BlockState previousState, PendingSignAction action, RegistrySortMode sortMode, String searchQuery, String registryKey, int returnPage) {
        this.location = location;
        this.previousState = previousState;
        this.action = action;
        this.sortMode = sortMode;
        this.searchQuery = searchQuery;
        this.registryKey = registryKey;
        this.returnPage = returnPage;
    }

    public void restore() {
        previousState.update(true, false);
    }
}
