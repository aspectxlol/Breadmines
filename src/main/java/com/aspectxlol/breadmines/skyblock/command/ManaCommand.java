package com.aspectxlol.breadmines.skyblock.command;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.util.CommandUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * ManaCommand - Operator-only command for controlling the Breadmines Skyblock system.
 * Implements /mana <enable|disable> to toggle mana regeneration and ability execution.
 */
public class ManaCommand implements CommandExecutor {

    private final Breadmines plugin;

    public ManaCommand(Breadmines plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtils.requireOp(sender)) return true;

        // Check for arguments
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mana <enable|disable>");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        if (subcommand.equals("enable")) {
            plugin.enableSystem();
            sender.sendMessage(ChatColor.GREEN + "✓ Breadmines Skyblock system ENABLED");
            sender.sendMessage(ChatColor.GRAY + "Mana regeneration and abilities are now active.");
            return true;
        }

        if (subcommand.equals("disable")) {
            plugin.disableSystem();
            sender.sendMessage(ChatColor.RED + "✗ Breadmines Skyblock system DISABLED");
            sender.sendMessage(ChatColor.GRAY + "Mana regeneration and abilities are now inactive.");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /mana <enable|disable>");
        return true;
    }
}
