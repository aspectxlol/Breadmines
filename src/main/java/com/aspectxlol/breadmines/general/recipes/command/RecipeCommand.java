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

    private static final List<String> SUBCOMMANDS = Arrays.asList("create", "update", "delete", "remove", "list", "ls", "info", "sync", "debug");

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
                case "debug":
                    return handleDebug(sender, label, args);
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

            if (action.equals("debug")) {
                return filterByPrefix(args[1], Arrays.asList("status", "list", "check", "toggle", "inventory"));
            }
        }

        if (args.length == 3 && (action.equals("create") || action.equals("update") || action.equals("delete") || action.equals("remove"))) {
            return List.of("1", "2", "4", "8", "16", "32", "64");
        }

        if (args.length == 3 && (action.equals("delete") || action.equals("remove") || action.equals("info") || action.equals("debug"))) {
            if (action.equals("debug")) {
                return filterByPrefix(args[2], recipeManager.getRecipeOutputKeys());
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
        List<RecipeDefinition> recipesForOutput = recipeManager.getRecipesForOutput(outputKey);
        sender.sendMessage(ChatColor.GRAY + "Total recipes for output: " + ChatColor.AQUA + recipesForOutput.size());
        return true;
    }

    private boolean handleDelete(CommandSender sender, String label, String[] args) throws SQLException {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " delete <output>");
            return true;
        }

        String outputKey = args[1];
        if (args.length >= 4) {
            int inputAmount;
            try {
                inputAmount = Integer.parseInt(args[2]);
            } catch (NumberFormatException exception) {
                sender.sendMessage(ChatColor.RED + "Input amount must be a whole number.");
                return true;
            }

            String inputKey = args[3];
            if (recipeManager.deleteRecipe(outputKey, inputKey, inputAmount)) {
                sender.sendMessage(ChatColor.GREEN + "✓ Deleted recipe " + ChatColor.AQUA + itemRegistry.normalizeName(outputKey)
                    + ChatColor.GRAY + " <= " + ChatColor.AQUA + inputAmount + "x " + itemRegistry.normalizeName(inputKey));
            } else {
                sender.sendMessage(ChatColor.RED + "No matching recipe found for output " + outputKey + ", input " + inputAmount + "x " + inputKey);
            }
            return true;
        }

        if (recipeManager.deleteRecipe(outputKey)) {
            sender.sendMessage(ChatColor.GREEN + "✓ Deleted all recipes for output key " + ChatColor.AQUA + itemRegistry.normalizeName(outputKey));
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
                if (!recipeManager.isGithubConfigured()) {
                    sender.sendMessage(ChatColor.YELLOW + "⚠ Recipe sync is not configured yet.");
                    for (String issue : recipeManager.getGithubConfigurationIssues()) {
                        sender.sendMessage(ChatColor.YELLOW + " - " + issue);
                    }
                    sender.sendMessage(ChatColor.YELLOW + "Configure `recipes.github.enabled`, `recipes.github.path`, and the shared GitHub owner/repo/branch in `config.yml`.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "⚠ Recipes sync failed. Check your GitHub token, repo access, and network connection.");
                }

                recipeManager.load();
                sender.sendMessage(ChatColor.YELLOW + "Reloaded local recipe database. Total: " + recipeManager.getRecipes().size());
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

        List<RecipeDefinition> recipes = recipeManager.getRecipesForOutput(args[1]);
        if (recipes.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No recipe found for output key " + args[1]);
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "Recipes for " + itemRegistry.normalizeName(args[1]) + ChatColor.GRAY + " (" + recipes.size() + ")");
        for (RecipeDefinition recipe : recipes) {
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.AQUA + recipe.getInputAmount() + "x " + recipe.getInputKey());
        }
        return true;
    }

    private boolean handleDebug(CommandSender sender, String label, String[] args) throws SQLException {
        if (args.length == 1) {
            recipeManager.setDebugMode(!recipeManager.isDebugMode());
            sender.sendMessage(ChatColor.GREEN + "✓ Recipe debug mode " + (recipeManager.isDebugMode() ? "enabled" : "disabled"));
            return true;
        }

        String debugAction = args[1].toLowerCase(Locale.ROOT);
        if (debugAction.equals("toggle")) {
            recipeManager.setDebugMode(!recipeManager.isDebugMode());
            sender.sendMessage(ChatColor.GREEN + "✓ Recipe debug mode " + (recipeManager.isDebugMode() ? "enabled" : "disabled"));
            return true;
        }

        if (debugAction.equals("status")) {
            sender.sendMessage(ChatColor.GOLD + "=== Recipe Debug Status ===");
            sender.sendMessage(ChatColor.GREEN + "Debug mode: " + (recipeManager.isDebugMode() ? "enabled" : "disabled"));
            sender.sendMessage(ChatColor.GREEN + "Total recipes: " + recipeManager.getRecipes().size());
            sender.sendMessage(ChatColor.GREEN + "Recipe outputs: " + recipeManager.getRecipeOutputKeys().size());
            return true;
        }

        if (debugAction.equals("inventory")) {
            Player player = CommandUtils.requirePlayer(sender);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Use this in-game so I can inspect your inventory.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "=== Recipe Inventory Debug: " + player.getName() + " ===");
            for (String line : recipeManager.buildInventoryDebugReport(player)) {
                sender.sendMessage(ChatColor.GRAY + line);
            }
            return true;
        }

        if (debugAction.equals("list")) {
            if (args.length >= 3) {
                List<RecipeDefinition> recipes = recipeManager.getRecipesForOutput(args[2]);
                sender.sendMessage(ChatColor.GOLD + "=== Recipe Debug List: " + itemRegistry.normalizeName(args[2]) + " ===");
                if (recipes.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "No recipes found for that output.");
                    return true;
                }
                for (RecipeDefinition recipe : recipes) {
                    sender.sendMessage(ChatColor.AQUA + recipe.getOutputKey() + ChatColor.GRAY + " <= " + ChatColor.AQUA + recipe.getInputAmount() + "x " + recipe.getInputKey());
                }
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "=== All Recipes ===");
            for (RecipeDefinition recipe : recipeManager.getRecipes()) {
                sender.sendMessage(ChatColor.AQUA + recipe.getOutputKey() + ChatColor.GRAY + " <= " + ChatColor.AQUA + recipe.getInputAmount() + "x " + recipe.getInputKey());
            }
            return true;
        }

        if (debugAction.equals("check")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " debug check <output>");
                return true;
            }

            List<RecipeDefinition> recipes = recipeManager.getRecipesForOutput(args[2]);
            sender.sendMessage(ChatColor.GOLD + "=== Debug Check: " + itemRegistry.normalizeName(args[2]) + " ===");
            if (recipes.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No recipes found for that output.");
                return true;
            }

            for (RecipeDefinition recipe : recipes) {
                sender.sendMessage(ChatColor.GREEN + "Output: " + recipe.getOutputKey());
                sender.sendMessage(ChatColor.GRAY + "Input: " + ChatColor.AQUA + recipe.getInputAmount() + "x " + recipe.getInputKey());
            }
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug - toggle debug mode");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug status - show debug status");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug list [output] - list all recipes or recipes for one output");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug check <output> - inspect recipes for an output");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug inventory - inspect your inventory against all recipes");
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " create <output> <input_amount_needed> <input>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " update <output> <input_amount_needed> <input>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " delete <output>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " delete <output> <input_amount_needed> <input> - delete one exact recipe");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <output>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug - toggle recipe debug mode");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug inventory - inspect your inventory against all recipes");
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
