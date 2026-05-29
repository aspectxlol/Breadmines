package com.aspectxlol.breadmines.general.recipes.command;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.util.CommandUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RecipeTabCompleter implements TabCompleter {
    private final Breadmines plugin;

    public RecipeTabCompleter(Breadmines plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("breadmines.recipe")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterByPrefix(args[0], Arrays.asList("create", "update", "delete", "remove", "list", "ls", "info", "sync", "debug"));
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            if (action.equals("create") || action.equals("update")) {
                return filterByPrefix(args[1], getRegistryKeys());
            }

            if (action.equals("delete") || action.equals("remove") || action.equals("info")) {
                return filterByPrefix(args[1], plugin.getRecipeManager().getRecipeOutputKeys());
            }

            if (action.equals("debug")) {
                return filterByPrefix(args[1], Arrays.asList("status", "list", "check", "toggle", "inventory"));
            }
        }

        if (args.length == 3 && (action.equals("create") || action.equals("update") || action.equals("delete") || action.equals("remove"))) {
            return List.of("1", "2", "4", "8", "16", "32", "64");
        }

        if (args.length == 3 && (action.equals("delete") || action.equals("remove") || action.equals("info") || action.equals("debug"))) {
            if (action.equals("debug")) {
                return filterByPrefix(args[2], plugin.getRecipeManager().getRecipeOutputKeys());
            }

            return filterByPrefix(args[2], getRegistryKeys());
        }

        if (args.length == 4 && (action.equals("create") || action.equals("update"))) {
            return filterByPrefix(args[3], getRegistryKeys());
        }

        if (args.length == 4 && (action.equals("delete") || action.equals("remove"))) {
            return filterByPrefix(args[3], getRegistryKeys());
        }

        return Collections.emptyList();
    }

    private List<String> getRegistryKeys() {
        List<String> keys = new ArrayList<>();
        plugin.getCustomItemRegistry().getDefinitions().forEach(definition -> keys.add(definition.getId()));
        keys.sort(String::compareTo);
        return keys;
    }

    private List<String> filterByPrefix(String prefix, Collection<String> values) {
        if (prefix == null || prefix.isEmpty()) {
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
