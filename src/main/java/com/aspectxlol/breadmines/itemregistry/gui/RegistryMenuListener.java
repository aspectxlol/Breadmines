package com.aspectxlol.breadmines.itemregistry.gui;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.itemregistry.CustomItemDefinition;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryMenuListener implements Listener {

    private static final int SEARCH_SIGN_OFFSET_MIN = 2;
    private static final int SEARCH_SIGN_OFFSET_MAX = 4;

    private final Breadmines plugin;
    private final RegistryMenu menu;
    private final RegistryEntryMenu entryMenu;
    private final CustomItemRegistry registry;
    private final Map<UUID, PendingSign> pendingSigns = new ConcurrentHashMap<>();

    public RegistryMenuListener(Breadmines plugin) {
        this.plugin = plugin;
        this.menu = new RegistryMenu(plugin);
        this.entryMenu = new RegistryEntryMenu(plugin);
        this.registry = plugin.getCustomItemRegistry();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() instanceof RegistryEntryMenuHolder entryHolder) {
            handleEntryClick((Player) event.getWhoClicked(), entryHolder, event.getRawSlot());
            event.setCancelled(true);
            return;
        }

        if (!(topInventory.getHolder() instanceof RegistryMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClick().isRightClick() && event.getRawSlot() >= 0 && event.getRawSlot() < RegistryMenu.CONTENT_SLOTS) {
            CustomItemDefinition definition = menu.getDefinitionAt(holder.getPage(), event.getRawSlot(), holder.getSortMode(), holder.getSearchQuery());
            if (definition != null) {
                entryMenu.open(player, definition.getId(), holder.getPage(), holder.getSortMode(), holder.getSearchQuery());
            }
            return;
        }

        handleBrowserClick(player, holder, event.getRawSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() instanceof RegistryMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        PendingSign pending = pendingSigns.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }

        if (!event.getBlock().getLocation().equals(pending.location)) {
            pendingSigns.put(player.getUniqueId(), pending);
            return;
        }

        String query = buildSearchQuery(event.getLines());
        pending.restore();

        if (pending.action == PendingSignAction.SEARCH) {
            String resolvedQuery = query.isBlank() ? null : query;
            plugin.getServer().getScheduler().runTask(plugin, () -> menu.open(player, 1, pending.sortMode, resolvedQuery));
            return;
        }

        if (pending.action == PendingSignAction.RENAME) {
            if (query.isBlank()) {
                player.sendMessage(ChatColor.RED + "Registry key cannot be empty.");
                plugin.getServer().getScheduler().runTask(plugin, () -> entryMenu.open(player, pending.registryKey, pending.returnPage, pending.sortMode, pending.searchQuery));
                return;
            }

            try {
                CustomItemDefinition updated = registry.renameItem(pending.registryKey, query).orElse(null);
                if (updated == null) {
                    player.sendMessage(ChatColor.RED + "Registry item not found: " + pending.registryKey);
                    plugin.getServer().getScheduler().runTask(plugin, () -> menu.open(player, pending.returnPage, pending.sortMode, pending.searchQuery));
                    return;
                }

                String newKey = updated.getId();
                player.sendMessage(ChatColor.GREEN + "Renamed registry item to " + ChatColor.AQUA + newKey + ChatColor.GREEN + ".");
                plugin.getServer().getScheduler().runTask(plugin, () -> entryMenu.open(player, newKey, pending.returnPage, pending.sortMode, pending.searchQuery));
            } catch (IllegalArgumentException exception) {
                player.sendMessage(ChatColor.RED + exception.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> entryMenu.open(player, pending.registryKey, pending.returnPage, pending.sortMode, pending.searchQuery));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PendingSign pending = pendingSigns.remove(event.getPlayer().getUniqueId());
        if (pending != null) {
            pending.restore();
        }
    }

    private void handleBrowserClick(Player player, RegistryMenuHolder holder, int rawSlot) {
        int currentPage = holder.getPage();
        RegistrySortMode sortMode = holder.getSortMode();
        String searchQuery = holder.getSearchQuery();
        int totalPages = menu.getTotalPages(searchQuery);

        switch (rawSlot) {
            case RegistryMenu.SLOT_PREV_PAGE:
                if (currentPage > 1) {
                    menu.open(player, currentPage - 1, sortMode, searchQuery);
                }
                return;
            case RegistryMenu.SLOT_SEARCH:
                openSearchPrompt(player, sortMode, searchQuery);
                return;
            case RegistryMenu.SLOT_SORT:
                menu.open(player, 1, sortMode.next(), searchQuery);
                return;
            case RegistryMenu.SLOT_RESET:
                menu.open(player, 1, RegistrySortMode.NAME_ASC, null);
                return;
            case RegistryMenu.SLOT_NEXT_PAGE:
                if (currentPage < totalPages) {
                    menu.open(player, currentPage + 1, sortMode, searchQuery);
                }
                return;
            case RegistryMenu.SLOT_CLOSE:
                player.closeInventory();
                return;
            default:
                break;
        }

        if (rawSlot < 0 || rawSlot >= RegistryMenu.CONTENT_SLOTS) {
            return;
        }

        CustomItemDefinition definition = menu.getDefinitionAt(currentPage, rawSlot, sortMode, searchQuery);
        if (definition == null) {
            return;
        }

        ItemStack itemStack = definition.getItemStack();
        itemStack.setAmount(1);

        for (ItemStack leftover : player.getInventory().addItem(itemStack).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void handleEntryClick(Player player, RegistryEntryMenuHolder holder, int rawSlot) {
        String registryKey = holder.getRegistryKey();
        RegistrySortMode sortMode = holder.getSortMode();
        String searchQuery = holder.getSearchQuery();

        switch (rawSlot) {
            case RegistryEntryMenu.SLOT_GET_ITEM:
                registry.createItemStack(registryKey).ifPresentOrElse(itemStack -> {
                    for (ItemStack leftover : player.getInventory().addItem(itemStack).values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }, () -> player.sendMessage(ChatColor.RED + "Registry item not found: " + registryKey));
                return;
            case RegistryEntryMenu.SLOT_UPDATE_ITEM:
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem == null || heldItem.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + "Hold an item to update this registry entry.");
                    return;
                }

                try {
                    CustomItemDefinition updated = registry.updateItem(registryKey, heldItem, player.getName()).orElse(null);
                    if (updated == null) {
                        player.sendMessage(ChatColor.RED + "Registry item not found: " + registryKey);
                        return;
                    }

                    player.sendMessage(ChatColor.GREEN + "Updated registry item " + ChatColor.AQUA + registryKey + ChatColor.GREEN + ".");
                    entryMenu.open(player, registryKey, holder.getReturnPage(), sortMode, searchQuery);
                } catch (IllegalArgumentException exception) {
                    player.sendMessage(ChatColor.RED + exception.getMessage());
                }
                return;
            case RegistryEntryMenu.SLOT_RENAME:
                openRenamePrompt(player, holder);
                return;
            case RegistryEntryMenu.SLOT_BACK:
                menu.open(player, holder.getReturnPage(), sortMode, searchQuery);
                return;
            case RegistryEntryMenu.SLOT_CLOSE:
                player.closeInventory();
                return;
            default:
                break;
        }
    }

    private void openSearchPrompt(Player player, RegistrySortMode sortMode, String currentQuery) {
        PendingSign existing = pendingSigns.remove(player.getUniqueId());
        if (existing != null) {
            existing.restore();
        }

        player.closeInventory();

        Block signBlock = findSignBlock(player);
        if (signBlock == null) {
            player.sendMessage(ChatColor.RED + "No space to open search prompt.");
            return;
        }

        BlockState previousState = signBlock.getState();
        signBlock.setType(Material.OAK_SIGN, false);
        BlockState newState = signBlock.getState();
        if (!(newState instanceof Sign sign)) {
            previousState.update(true, false);
            player.sendMessage(ChatColor.RED + "Search prompt failed to open.");
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

    private void openRenamePrompt(Player player, RegistryEntryMenuHolder holder) {
        PendingSign existing = pendingSigns.remove(player.getUniqueId());
        if (existing != null) {
            existing.restore();
        }

        player.closeInventory();

        Block signBlock = findSignBlock(player);
        if (signBlock == null) {
            player.sendMessage(ChatColor.RED + "No space to open rename prompt.");
            entryMenu.open(player, holder.getRegistryKey(), holder.getReturnPage(), holder.getSortMode(), holder.getSearchQuery());
            return;
        }

        BlockState previousState = signBlock.getState();
        signBlock.setType(Material.OAK_SIGN, false);
        BlockState newState = signBlock.getState();
        if (!(newState instanceof Sign sign)) {
            previousState.update(true, false);
            player.sendMessage(ChatColor.RED + "Rename prompt failed to open.");
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

    private Block findSignBlock(Player player) {
        Block baseBlock = player.getLocation().getBlock();
        for (int offset = SEARCH_SIGN_OFFSET_MIN; offset <= SEARCH_SIGN_OFFSET_MAX; offset++) {
            Block candidate = baseBlock.getRelative(0, offset, 0);
            if (candidate.getType().isAir()) {
                return candidate;
            }
        }

        return null;
    }

    private String buildSearchQuery(String[] lines) {
        if (lines == null || lines.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line == null) {
                continue;
            }

            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(trimmed);
        }

        return builder.toString();
    }

    private enum PendingSignAction {
        SEARCH,
        RENAME
    }

    private static final class PendingSign {
        private final Location location;
        private final BlockState previousState;
        private final PendingSignAction action;
        private final RegistrySortMode sortMode;
        private final String searchQuery;
        private final String registryKey;
        private final int returnPage;

        private PendingSign(Location location, BlockState previousState, PendingSignAction action, RegistrySortMode sortMode, String searchQuery, String registryKey, int returnPage) {
            this.location = location;
            this.previousState = previousState;
            this.action = action;
            this.sortMode = sortMode;
            this.searchQuery = searchQuery;
            this.registryKey = registryKey;
            this.returnPage = returnPage;
        }

        private void restore() {
            previousState.update(true, false);
        }
    }
}