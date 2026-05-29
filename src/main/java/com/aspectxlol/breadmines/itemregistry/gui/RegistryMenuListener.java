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
import com.aspectxlol.breadmines.itemregistry.gui.PendingSign;
import com.aspectxlol.breadmines.itemregistry.gui.PendingSignAction;
import com.aspectxlol.breadmines.itemregistry.gui.RegistrySignPrompter;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryMenuListener implements Listener {

    private static final int SEARCH_SIGN_OFFSET_MIN = 2;
    private static final int SEARCH_SIGN_OFFSET_MAX = 4;

    private final Breadmines plugin;
    private final RegistryMenu menu;
    private final RegistryEntryMenu entryMenu;
    private final CustomItemRegistry registry;
    private final Map<UUID, PendingSign> pendingSigns = new ConcurrentHashMap<>();
    private final RegistrySignPrompter signPrompter;

    public RegistryMenuListener(Breadmines plugin) {
        this.plugin = plugin;
        this.menu = new RegistryMenu(plugin);
        this.entryMenu = new RegistryEntryMenu(plugin);
        this.registry = plugin.getCustomItemRegistry();
        this.signPrompter = new RegistrySignPrompter(plugin);
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

        String query = RegistrySignPrompter.buildSearchQuery(event.getLines());
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
                if (currentPage > 1) menu.open(player, currentPage - 1, sortMode, searchQuery);
                return;
            case RegistryMenu.SLOT_SEARCH:
                signPrompter.openSearchPrompt(player, pendingSigns, sortMode, searchQuery);
                return;
            case RegistryMenu.SLOT_SORT:
                menu.open(player, 1, sortMode.next(), searchQuery);
                return;
            case RegistryMenu.SLOT_RESET:
                menu.open(player, 1, RegistrySortMode.NAME_ASC, null);
                return;
            case RegistryMenu.SLOT_NEXT_PAGE:
                if (currentPage < totalPages) menu.open(player, currentPage + 1, sortMode, searchQuery);
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
                signPrompter.openRenamePrompt(player, pendingSigns, holder);
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

    
}