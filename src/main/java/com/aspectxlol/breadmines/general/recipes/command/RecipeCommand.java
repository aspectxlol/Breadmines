package com.aspectxlol.breadmines.general.recipes.command;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.general.recipes.RecipeDefinition;
import com.aspectxlol.breadmines.general.recipes.RecipeManager;
import com.aspectxlol.breadmines.general.recipes.gui.RecipeListMenu;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import com.aspectxlol.breadmines.util.CommandUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RecipeCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("create", "update", "delete", "remove", "list", "ls", "info", "sync");

    private final Breadmines plugin;
    private final RecipeManager recipeManager;
    private final CustomItemRegistry itemRegistry;
    private final RecipeListMenu recipeListMenu;

    public RecipeCommand(Breadmines plugin) {
        this.plugin = plugin;
        this.recipeManager = plugin.getRecipeManager();
        this.itemRegistry = plugin.getCustomItemRegistry();
        this.recipeListMenu = new RecipeListMenu(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtils.requirePermission(sender, "breadmines.recipe")) {
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "create":
                case "update":
                    return handleCreateOrUpdate(sender, label, args);
                case "sync":
                    return handleSync(sender);
                case "delete":
                case "remove":
                    return handleDelete(sender, label, args);
                case "list":
                case "ls":
                    return handleList(sender);
                case "info":
                    return handleInfo(sender, label, args);
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown action: " + args[0]);
                    sendUsage(sender, label);
                    return true;
            }
        } catch (SQLException exception) {
            sender.sendMessage(ChatColor.RED + "Recipe database error: " + exception.getMessage());
            plugin.getLogger().severe("Recipe command SQL error: " + exception.getMessage());
            return true;
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("breadmines.recipe")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterByPrefix(args[0], SUBCOMMANDS);
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            if (action.equals("create") || action.equals("update")) {
                return filterByPrefix(args[1], getRegistryKeys());
            }

            if (action.equals("delete") || action.equals("remove") || action.equals("info")) {
                return filterByPrefix(args[1], recipeManager.getRecipeOutputKeys());
            }
        }

        if (args.length == 3 && (action.equals("create") || action.equals("update"))) {
            return List.of("1", "2", "4", "8", "16", "32", "64");
        }

        if (args.length == 4 && (action.equals("create") || action.equals("update"))) {
            return filterByPrefix(args[3], getRegistryKeys());
        }

        return Collections.emptyList();
    }

    private boolean handleCreateOrUpdate(CommandSender sender, String label, String[] args) throws SQLException {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " create <output> <input_amount_needed> <input>");
            return true;
        }

        String outputKey = args[1];
        int inputAmount;
        try {
            inputAmount = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Input amount must be a whole number.");
            return true;
        }

        String inputKey = args[3];
        RecipeDefinition recipe = recipeManager.createOrUpdateRecipe(outputKey, inputAmount, inputKey);

        sender.sendMessage(ChatColor.GREEN + "✓ Recipe saved: " + ChatColor.AQUA + recipe.getOutputKey() + ChatColor.GRAY
            + " <= " + ChatColor.AQUA + recipe.getInputAmount() + "x " + recipe.getInputKey());
        return true;
    }

    private boolean handleDelete(CommandSender sender, String label, String[] args) throws SQLException {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " delete <output>");
            return true;
        }

        String outputKey = args[1];
        if (recipeManager.deleteRecipe(outputKey)) {
            sender.sendMessage(ChatColor.GREEN + "✓ Deleted recipe for output key " + ChatColor.AQUA + itemRegistry.normalizeName(outputKey));
        } else {
            sender.sendMessage(ChatColor.RED + "No recipe found for output key " + outputKey);
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        Player player = CommandUtils.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        recipeListMenu.open(player, 1);
        player.sendMessage(ChatColor.GREEN + "Opened recipe list.");
        return true;
    }

    private boolean handleSync(CommandSender sender) {
        if (!CommandUtils.requirePermission(sender, "breadmines.recipe")) {
            return true;
        }

        try {
            boolean ok = recipeManager.syncWithGithub();
            if (ok) {
                sender.sendMessage(ChatColor.GREEN + "✓ Recipes synchronized with GitHub. Total: " + recipeManager.getRecipes().size());
            } else {
                // fallback: reload local DB
                recipeManager.load();
                sender.sendMessage(ChatColor.YELLOW + "⚠ Recipes sync not configured or skipped; reloaded local DB. Total: " + recipeManager.getRecipes().size());
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Recipe database error: " + e.getMessage());
            plugin.getLogger().severe("Recipe sync SQL error: " + e.getMessage());
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " info <output>");
            return true;
        }

        RecipeDefinition recipe = recipeManager.findRecipe(args[1]);
        if (recipe == null) {
            sender.sendMessage(ChatColor.RED + "No recipe found for output key " + args[1]);
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "Recipe " + recipe.getOutputKey());
        sender.sendMessage(ChatColor.GRAY + "Input: " + ChatColor.AQUA + recipe.getInputAmount() + "x " + recipe.getInputKey());
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " create <output> <input_amount_needed> <input>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " update <output> <input_amount_needed> <input>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " delete <output>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <output>");
    }

    private List<String> getRegistryKeys() {
        List<String> keys = new ArrayList<>();
        itemRegistry.getDefinitions().forEach(definition -> keys.add(definition.getId()));
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
