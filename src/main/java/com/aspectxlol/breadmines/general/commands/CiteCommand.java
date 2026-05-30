package com.aspectxlol.breadmines.general.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class CiteCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /cite <player> <rule>");
            sender.sendMessage(ChatColor.GRAY + "Available rules: " + ChatColor.YELLOW + String.join(", ", RulesCatalog.ruleSuggestions()));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "That player is not online.");
            return true;
        }

        String ruleInput = args[1];
        RulesCatalog.RuleEntry entry = RulesCatalog.findRule(ruleInput).orElse(null);
        if (entry == null) {
            sender.sendMessage(ChatColor.RED + "Unknown rule: " + ruleInput);
            sender.sendMessage(ChatColor.GRAY + "Try one of: " + ChatColor.YELLOW + String.join(", ", RulesCatalog.ruleSuggestions()));
            return true;
        }

        String senderName = sender.getName();
        String citation = RulesCatalog.formatCitation(entry);

        target.sendMessage(ChatColor.RED + "You were cited by " + ChatColor.YELLOW + senderName + ChatColor.RED + ": " + ChatColor.WHITE + entry.displayName());
        target.sendMessage(ChatColor.GRAY + entry.ruleText());
        sender.sendMessage(ChatColor.GREEN + "Cited " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + " for " + ChatColor.WHITE + entry.displayName());
        sender.sendMessage(ChatColor.GRAY + entry.ruleText());

        Bukkit.getLogger().info("[Breadmines] " + senderName + " cited " + target.getName() + " for " + citation);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> names = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
            return names;
        }

        if (args.length == 2) {
            return filterByPrefix(args[1], RulesCatalog.ruleSuggestions());
        }

        return Collections.emptyList();
    }

    private List<String> filterByPrefix(String prefix, List<String> values) {
        if (prefix == null || prefix.isBlank()) {
            return new ArrayList<>(values);
        }

        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                results.add(value);
            }
        }
        return results;
    }
}
