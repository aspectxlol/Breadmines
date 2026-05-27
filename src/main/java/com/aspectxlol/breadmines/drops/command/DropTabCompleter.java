package com.aspectxlol.breadmines.drops.command;

import com.aspectxlol.breadmines.drops.DropSystemHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DropTabCompleter - Provides tab completion for /drops and /dropslist commands.
 */
public class DropTabCompleter implements TabCompleter {

    private final JavaPlugin plugin;
    private final DropSystemHandler dropHandler;

    public DropTabCompleter(JavaPlugin plugin, DropSystemHandler dropHandler) {
        this.plugin = plugin;
        this.dropHandler = dropHandler;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("admin.setup")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (label.equalsIgnoreCase("drops")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("create", "set", "update", "delete", "remove", "read", "info", "debug"));
            } else if (args.length == 2) {
                String action = args[0].toLowerCase();
                if (action.equals("delete") || action.equals("remove") || action.equals("read") || action.equals("info")) {
                    try {
                        List<String> blockNames = dropHandler.getAllBlockDropsNames();
                        completions.addAll(blockNames);
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Error fetching block names for completion: " + e.getMessage());
                    }
                } else if (action.equals("create") || action.equals("set") || action.equals("update")) {
                    try {
                        completions.addAll(dropHandler.getMinecraftBlockTypes());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error fetching block types: " + e.getMessage());
                    }
                }
            } else if (args.length == 3) {
                String action = args[0].toLowerCase();
                if (action.equals("create") || action.equals("set") || action.equals("update")) {
                    try {
                        List<String> itemIds = dropHandler.getRegisteredItemIds();
                        completions.addAll(itemIds);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error fetching registry item IDs: " + e.getMessage());
                    }
                }
            }
        } else if (label.equalsIgnoreCase("dropslist")) {
            if (args.length == 1) {
                completions.add("[page]");
            }
        }

        String currentArg = args.length > 0 ? args[args.length - 1].toLowerCase() : "";
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }
}
