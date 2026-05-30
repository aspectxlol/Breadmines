package com.aspectxlol.breadmines.itemregistry.command;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.itemregistry.CustomItemDefinition;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import com.aspectxlol.breadmines.itemregistry.gui.RegistryMenu;
import com.aspectxlol.breadmines.util.CommandUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RegistryCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = Arrays.asList("add", "register", "store", "lazyadd", "lazy", "get", "give", "remove", "delete", "rm", "del", "list", "ls", "show", "search", "find", "query", "sync", "debug");

    private final CustomItemRegistry registry;
    private final RegistryMenu menu;

    public RegistryCommand(Breadmines plugin) {
        this.registry = plugin.getCustomItemRegistry();
        this.menu = new RegistryMenu(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtils.requirePermission(sender, "breadmines.registry")) {
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "add":
            case "register":
            case "store":
                return handleAdd(sender, label, args);
            case "lazyadd":
            case "lazy":
                return handleLazyAdd(sender, label);
            case "get":
            case "give":
                return handleGet(sender, label, args);
            case "remove":
            case "delete":
            case "rm":
            case "del":
                return handleRemove(sender, label, args);
            case "list":
            case "ls":
            case "show":
                return handleList(sender);
            case "search":
            case "find":
            case "query":
                return handleSearch(sender, label, args);
            case "sync":
                return handleSync(sender, args);
            case "debug":
                return handleDebug(sender, label, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                sendUsage(sender, label);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(args[0], ROOT_SUBCOMMANDS);
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase(Locale.ROOT);
            if (subcommand.equals("get") || subcommand.equals("give") || subcommand.equals("remove") || subcommand.equals("delete") || subcommand.equals("rm") || subcommand.equals("del")) {
                return filterByPrefix(args[1], getRegistryNames());
            }

            if (subcommand.equals("sync")) {
                return filterByPrefix(args[1], Arrays.asList("push", "pull"));
            }

            if (subcommand.equals("debug")) {
                return filterByPrefix(args[1], Arrays.asList("held"));
            }
        }

        return Collections.emptyList();
    }

    private boolean handleAdd(CommandSender sender, String label, String[] args) {
        Player player = CommandUtils.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " add <name>");
            return true;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "Hold an item in your main hand first.");
            return true;
        }

        String name = joinArgs(args, 1);
        try {
            CustomItemDefinition definition = registry.registerItem(name, heldItem, player.getName());
            sender.sendMessage(ChatColor.GREEN + "✓ Registered item " + ChatColor.AQUA + definition.getId() + ChatColor.GREEN + " from " + definition.getDisplayName());
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    private boolean handleLazyAdd(CommandSender sender, String label) {
        Player player = CommandUtils.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "Hold an item in your main hand first.");
            return true;
        }

        try {
            CustomItemDefinition definition = registry.registerItemFromDisplayName(heldItem, player.getName());
            sender.sendMessage(ChatColor.GREEN + "✓ Lazily registered item " + ChatColor.AQUA + definition.getId() + ChatColor.GREEN + " from held item name.");
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    private boolean handleGet(CommandSender sender, String label, String[] args) {
        Player player = CommandUtils.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " get <name>");
            return true;
        }

        String name = joinArgs(args, 1);
        registry.createItemStack(name).ifPresentOrElse(itemStack -> {
            Collection<ItemStack> leftovers = player.getInventory().addItem(itemStack).values();
            for (ItemStack leftover : leftovers) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }

            sender.sendMessage(ChatColor.GREEN + "✓ Gave " + ChatColor.AQUA + registry.normalizeName(name) + ChatColor.GREEN + " to your inventory.");
        }, () -> sender.sendMessage(ChatColor.RED + "Unknown registry item: " + name));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " remove <name>");
            return true;
        }

        String name = joinArgs(args, 1);
        if (registry.removeItem(name)) {
            sender.sendMessage(ChatColor.GREEN + "✓ Removed registry item " + ChatColor.AQUA + registry.normalizeName(name));
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown registry item: " + name);
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Player player = CommandUtils.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        menu.open(player, 1);
        player.sendMessage(ChatColor.GREEN + "Opened the custom item registry.");
        return true;
    }

    private boolean handleSearch(CommandSender sender, String label, String[] args) {
        Player player = CommandUtils.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " search <terms>");
            return true;
        }

        String query = joinArgs(args, 1);
        menu.open(player, 1, com.aspectxlol.breadmines.itemregistry.gui.RegistrySortMode.NAME_ASC, query);
        sender.sendMessage(ChatColor.GREEN + "Opened search results for " + ChatColor.AQUA + query + ChatColor.GREEN + ".");
        return true;
    }

    private boolean handleSync(CommandSender sender, String[] args) {
        if (!sender.hasPermission("breadmines.registry")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        String mode = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "push";
        sender.sendMessage(ChatColor.GRAY + "Synchronizing registry (" + mode + ")... This may take a moment.");
        try {
            CustomItemRegistry.RegistrySyncResult result;
            if (mode.equals("pull")) {
                result = registry.syncFromGithub();
            } else {
                result = registry.syncNow();
            }

            if (result.loaded) {
                sender.sendMessage(ChatColor.GREEN + "✓ Registry synchronized (mode: " + mode + ", source: " + result.sourceName + ") with " + result.count + " entries.");
            } else {
                sender.sendMessage(ChatColor.RED + "✗ Registry synchronization failed or no data found.");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "✗ Registry sync error: " + e.getMessage());
        }
        return true;
    }

    private boolean handleDebug(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " debug <held>");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug held - inspect the held item and registry match");
            return true;
        }

        String debugAction = args[1].toLowerCase(Locale.ROOT);
        if (!debugAction.equals("held")) {
            sender.sendMessage(ChatColor.RED + "Unknown debug action: " + args[1]);
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " debug held");
            return true;
        }

        Player player = CommandUtils.requirePlayer(sender);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Use this in-game so I can inspect your held item.");
            return true;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        sender.sendMessage(ChatColor.GOLD + "=== Held Item Debug: " + player.getName() + " ===");
        for (String line : buildHeldItemDebugLines(heldItem)) {
            sender.sendMessage(ChatColor.GRAY + line);
        }
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <add|lazyadd|get|remove|delete|list|search> ...");
        sender.sendMessage(ChatColor.YELLOW + "Additional: /" + label + " sync [push|pull] - push local registry or pull from GitHub");
        sender.sendMessage(ChatColor.YELLOW + "Additional: /" + label + " debug held - inspect the held item and registry match");
        sender.sendMessage(ChatColor.GRAY + "Add uses an explicit name. Lazyadd uses the held item's display name. Search ranks exact and partial matches across item id, name, type, and lore.");
        sender.sendMessage(ChatColor.GRAY + "Aliases: /" + label + " register, store, lazy, give, delete, rm, del, ls, show, find, query");
    }

    private List<String> buildHeldItemDebugLines(ItemStack heldItem) {
        List<String> lines = new ArrayList<>();
        if (heldItem == null || heldItem.getType().isAir()) {
            lines.add("Held item: empty");
            return lines;
        }

        lines.add("Material: " + heldItem.getType());
        lines.add("Amount: " + heldItem.getAmount());
        lines.add("Has item meta: " + (heldItem.hasItemMeta() ? "yes" : "no"));
        lines.add("Serialized: " + heldItem.serialize());

        ItemMeta meta = heldItem.getItemMeta();
        if (meta != null) {
            lines.add("Display name: " + (meta.hasDisplayName() ? meta.getDisplayName() : "none"));
            lines.add("Lore: " + (meta.hasLore() ? meta.getLore() : "none"));
            lines.add("Custom model data: " + (meta.hasCustomModelData() ? meta.getCustomModelData() : "none"));
            lines.add("PDC keys: " + meta.getPersistentDataContainer().getKeys());
        }

        String resolvedId = registry.getItemId(heldItem).orElse("none");
        lines.add("Resolved registry id: " + resolvedId);

        registry.getDefinition(heldItem).ifPresentOrElse(definition -> {
            lines.add("In registry: yes");
            lines.add("Registry entry display name: " + definition.getDisplayName());
            lines.add("Registry entry source: " + definition.getSource());
        }, () -> lines.add("In registry: no"));

        return lines;
    }

    private List<String> filterByPrefix(String prefix, Collection<String> values) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>(values);
        }

        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private List<String> getRegistryNames() {
        List<String> names = new ArrayList<>();
        for (CustomItemDefinition definition : registry.getDefinitions()) {
            names.add(definition.getId());
        }
        return names;
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < args.length; index++) {
            if (index > startIndex) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString();
    }
}