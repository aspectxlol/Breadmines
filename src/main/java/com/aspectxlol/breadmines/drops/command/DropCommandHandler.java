package com.aspectxlol.breadmines.drops.command;

import com.aspectxlol.breadmines.drops.DropSystemHandler;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * DropCommandHandler - Processes /drops and /dropslist commands.
 * Handles create, delete, read, and debug operations for block drops.
 */
public class DropCommandHandler implements CommandExecutor {

    private final JavaPlugin plugin;
    private final DropSystemHandler dropHandler;

    public DropCommandHandler(JavaPlugin plugin, DropSystemHandler dropHandler) {
        this.plugin = plugin;
        this.dropHandler = dropHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("admin.setup")) {
            sender.sendMessage("§c✗ You don't have permission to use this command.");
            return true;
        }

        if (label.equalsIgnoreCase("drops")) {
            return handleDropsCommand(sender, args);
        } else if (label.equalsIgnoreCase("dropslist")) {
            return handleDropsListCommand(sender, args);
        }

        return false;
    }

    /**
     * Handle /drops command: create/delete/read/debug operations.
     */
    private boolean handleDropsCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/drops create <block_name> <item_id> - Create a new block drop");
            sender.sendMessage("§e/drops delete <block_name> - Delete a block drop");
            sender.sendMessage("§e/drops read <block_name> - Read a block drop entry");
            return true;
        }

        String action = args[0].toLowerCase();

        try {
            if (action.equals("create") || action.equals("set") || action.equals("update")) {
                if (args.length < 3) {
                    sender.sendMessage("§c✗ Usage: /drops create <block_name> <item_id>");
                    return true;
                }

                String blockName = dropHandler.normalizeBlockName(args[1]);
                String itemId = args[2];

                dropHandler.insertOrUpdateBlockDrop(blockName, itemId);
                sender.sendMessage("§a✓ Block drop registered: " + blockName + " -> " + itemId);

            } else if (action.equals("delete") || action.equals("remove")) {
                if (args.length < 2) {
                    sender.sendMessage("§c✗ Usage: /drops delete <block_name>");
                    return true;
                }

                String blockName = dropHandler.normalizeBlockName(args[1]);
                dropHandler.deleteBlockDrop(blockName);
                sender.sendMessage("§a✓ Block drop deleted: " + blockName);

            } else if (action.equals("read") || action.equals("info")) {
                if (args.length < 2) {
                    sender.sendMessage("§c✗ Usage: /drops read <block_name>");
                    return true;
                }

                String blockName = dropHandler.normalizeBlockName(args[1]);
                String itemId = dropHandler.queryBlockDrop(blockName);

                if (itemId != null) {
                    sender.sendMessage("§a✓ " + blockName + " -> " + itemId);
                } else {
                    sender.sendMessage("§c✗ No entry found for: " + blockName);
                }

            } else if (action.equals("debug")) {
                return handleDebugCommand(sender, args);
            } else {
                sender.sendMessage("§c✗ Unknown action: " + action);
            }

        } catch (SQLException e) {
            sender.sendMessage("§c✗ Database error: " + e.getMessage());
            plugin.getLogger().severe("SQL Error: " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle /drops debug command and debug output.
     */
    private boolean handleDebugCommand(CommandSender sender, String[] args) {
        try {
            if (args.length == 1) {
                dropHandler.setDebugMode(!dropHandler.isDebugMode());
                sender.sendMessage("§a✓ Debug mode " + (dropHandler.isDebugMode() ? "enabled" : "disabled"));
                return true;
            }

            String debugAction = args[1].toLowerCase(Locale.ROOT);
            if (debugAction.equals("status")) {
                sender.sendMessage("§6=== Debug Status ===");
                sender.sendMessage("§aDebug mode: " + (dropHandler.isDebugMode() ? "enabled" : "disabled"));
                sender.sendMessage("§aRegistered blocks: " + dropHandler.getAllBlockDrops().size());
                List<String> itemIds = dropHandler.getSkriptItemIds();
                sender.sendMessage("§aSkript item IDs loaded: " + itemIds.size());
                return true;
            }

            if (debugAction.equals("list")) {
                sender.sendMessage("§6=== Registered Block Drops ===");
                List<String[]> entries = dropHandler.getAllBlockDrops();
                if (entries.isEmpty()) {
                    sender.sendMessage("§cNo registered block drops.");
                    return true;
                }
                for (String[] entry : entries) {
                    sender.sendMessage("§a" + entry[0] + " -> " + entry[1]);
                }
                return true;
            }

            if (debugAction.equals("check")) {
                if (args.length < 3) {
                    sender.sendMessage("§c✗ Usage: /drops debug check <block_name>");
                    return true;
                }
                String blockName = dropHandler.normalizeBlockName(args[2]);
                String itemId = dropHandler.queryBlockDrop(blockName);
                sender.sendMessage("§6=== Debug Check: " + blockName + " ===");
                if (itemId == null) {
                    sender.sendMessage("§cNo registered drop for " + blockName);
                    return true;
                }
                sender.sendMessage("§aDatabase item_id: " + itemId);
                Material material = dropHandler.resolveCustomMaterial(itemId);
                sender.sendMessage("§aResolved Bukkit Material: " + (material != null ? material.name() : "none"));
                Object skriptItem = dropHandler.getSkriptItemValue(itemId);
                sender.sendMessage("§aSkript alias lookup value type: " + (skriptItem != null ? skriptItem.getClass().getSimpleName() : "none"));
                List<String> itemIds = dropHandler.getSkriptItemIds();
                sender.sendMessage("§aSkript item registry contains alias: " + itemIds.contains(itemId));
                return true;
            }

            sender.sendMessage("§e/drops debug - toggle debug mode");
            sender.sendMessage("§e/drops debug status - show debug status");
            sender.sendMessage("§e/drops debug list - list registered blocks");
            sender.sendMessage("§e/drops debug check <block_name> - inspect a registered block entry");
            return true;
        } catch (SQLException e) {
            sender.sendMessage("§c✗ Database error: " + e.getMessage());
            plugin.getLogger().severe("SQL Error in debug command: " + e.getMessage());
            return true;
        }
    }

    /**
     * Handle /dropslist command with pagination.
     */
    private boolean handleDropsListCommand(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage("§c✗ Invalid page number.");
                return true;
            }
        }

        try {
            List<String[]> entries = dropHandler.getAllBlockDrops();
            int entriesPerPage = 10;
            int totalPages = (int) Math.ceil((double) entries.size() / entriesPerPage);

            if (page > totalPages) {
                page = totalPages;
            }
            if (page < 1) {
                page = 1;
            }

            sender.sendMessage("§6=== Block Drops (Page " + page + "/" + totalPages + ") ===");

            int startIdx = (page - 1) * entriesPerPage;
            int endIdx = Math.min(startIdx + entriesPerPage, entries.size());

            for (int i = startIdx; i < endIdx; i++) {
                String[] entry = entries.get(i);
                sender.sendMessage("§a• " + entry[0] + " §7-> §b" + entry[1]);
            }

            if (totalPages > 1) {
                sender.sendMessage("§7Use /dropslist <page> to navigate");
            }

        } catch (SQLException e) {
            sender.sendMessage("§c✗ Database error: " + e.getMessage());
            plugin.getLogger().severe("SQL Error: " + e.getMessage());
        }

        return true;
    }
}
