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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RegistryCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = Arrays.asList("add", "register", "store", "lazyadd", "lazy", "get", "give", "remove", "delete", "rm", "del", "list", "ls", "show");

    private final Breadmines plugin;
    private final CustomItemRegistry registry;
    private final RegistryMenu menu;

    public RegistryCommand(Breadmines plugin) {
        this.plugin = plugin;
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
        CustomItemDefinition definition = registry.registerItem(name, heldItem, player.getName());
        sender.sendMessage(ChatColor.GREEN + "✓ Registered item " + ChatColor.AQUA + definition.getId() + ChatColor.GREEN + " from " + definition.getDisplayName());
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

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <add|lazyadd|get|remove|delete|list> ...");
        sender.sendMessage(ChatColor.GRAY + "Add uses an explicit name. Lazyadd uses the held item's display name. Aliases: /" + label + " register, store, lazy, give, delete, rm, del, ls, show");
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