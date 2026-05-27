package com.aspectxlol.breadmines.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandUtils {

    private CommandUtils() {
    }

    public static boolean requireOp(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return false;
        }
        return true;
    }

    public static boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }

        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
        return false;
    }

    public static Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return null;
        }
        return (Player) sender;
    }
}
