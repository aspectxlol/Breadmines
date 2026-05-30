package com.aspectxlol.breadmines.general.commands;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.ui.MenuItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class RulesCommand implements CommandExecutor, Listener {
    private static final int INVENTORY_SIZE = 27;
    private static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&6&lServer Rules");

    @SuppressWarnings("unused")
    private final Breadmines plugin;

    public RulesCommand(Breadmines plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        player.openInventory(createInventory());
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof RulesMenuHolder)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RulesMenuHolder) {
            event.setCancelled(true);
        }
    }

    private Inventory createInventory() {
        Inventory inventory = Bukkit.createInventory(new RulesMenuHolder(), INVENTORY_SIZE, TITLE);

        for (RulesCatalog.RuleSection section : RulesCatalog.sections()) {
            inventory.setItem(section.slot, MenuItemFactory.createButton(section.icon, section.title, section.rules));
        }

        return inventory;
    }

    private static final class RulesMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
