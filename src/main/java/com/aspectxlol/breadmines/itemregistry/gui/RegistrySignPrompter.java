package com.aspectxlol.breadmines.itemregistry.gui;

import com.aspectxlol.breadmines.Breadmines;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public final class RegistrySignPrompter {
    private final Breadmines plugin;
    private final RegistryMenu menu;
    private final RegistryEntryMenu entryMenu;

    public RegistrySignPrompter(Breadmines plugin) {
        this.plugin = plugin;
        this.menu = new RegistryMenu(plugin);
        this.entryMenu = new RegistryEntryMenu(plugin);
    }

    public void openSearchPrompt(Player player, Map<UUID, PendingSign> pendingSigns, RegistrySortMode sortMode, String currentQuery) {
        PendingSign existing = pendingSigns.remove(player.getUniqueId());
        if (existing != null) existing.restore();

        player.closeInventory();

        Block signBlock = findSignBlock(player);
        if (signBlock == null) {
            player.sendMessage("§cNo space to open search prompt.");
            return;
        }

        BlockState previousState = signBlock.getState();
        signBlock.setType(Material.OAK_SIGN, false);
        BlockState newState = signBlock.getState();
        if (!(newState instanceof Sign sign)) {
            previousState.update(true, false);
            player.sendMessage("§cSearch prompt failed to open.");
            return;
        }

        sign.setLine(0, "Search");
        sign.setLine(1, currentQuery == null ? "" : currentQuery);
        sign.setLine(2, "");
        sign.setLine(3, "");
        sign.update(true, false);

        pendingSigns.put(player.getUniqueId(), new PendingSign(signBlock.getLocation(), previousState, PendingSignAction.SEARCH, sortMode, null, null, 1));
        player.openSign(sign);
    }

    public void openRenamePrompt(Player player, Map<UUID, PendingSign> pendingSigns, RegistryEntryMenuHolder holder) {
        PendingSign existing = pendingSigns.remove(player.getUniqueId());
        if (existing != null) existing.restore();

        player.closeInventory();

        Block signBlock = findSignBlock(player);
        if (signBlock == null) {
            player.sendMessage("§cNo space to open rename prompt.");
            entryMenu.open(player, holder.getRegistryKey(), holder.getReturnPage(), holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        BlockState previousState = signBlock.getState();
        signBlock.setType(Material.OAK_SIGN, false);
        BlockState newState = signBlock.getState();
        if (!(newState instanceof Sign sign)) {
            previousState.update(true, false);
            player.sendMessage("§cRename prompt failed to open.");
            entryMenu.open(player, holder.getRegistryKey(), holder.getReturnPage(), holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        sign.setLine(0, "Rename Key");
        sign.setLine(1, holder.getRegistryKey());
        sign.setLine(2, "");
        sign.setLine(3, "");
        sign.update(true, false);

        pendingSigns.put(player.getUniqueId(), new PendingSign(signBlock.getLocation(), previousState, PendingSignAction.RENAME, holder.getSortMode(), holder.getSearchQuery(), holder.getRegistryKey(), holder.getReturnPage()));
        player.openSign(sign);
    }

    public static String buildSearchQuery(String[] lines) {
        if (lines == null || lines.length == 0) return "";
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(trimmed);
        }
        return builder.toString();
    }

    private Block findSignBlock(Player player) {
        Block baseBlock = player.getLocation().getBlock();
        for (int offset = 2; offset <= 4; offset++) {
            Block candidate = baseBlock.getRelative(0, offset, 0);
            if (candidate.getType().isAir()) {
                return candidate;
            }
        }

        return null;
    }
}
