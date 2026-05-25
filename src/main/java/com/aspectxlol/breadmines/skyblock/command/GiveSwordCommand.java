package com.aspectxlol.breadmines.skyblock.command;

import com.aspectxlol.breadmines.skyblock.factory.NecronItemFactory;
import com.aspectxlol.breadmines.util.CommandUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * GiveSwordCommand - Operator-only command for distributing custom Necron swords.
 * Implements /givesword [hyperion|astraea|valkyrie|scylla|aotv]
 */
public class GiveSwordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtils.requireOp(sender)) return true;

        Player player = CommandUtils.requirePlayer(sender);
        if (player == null) return true;

        // Check for arguments
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /givesword <hyperion|astraea|valkyrie|scylla|aotv>");
            return true;
        }

        String swordType = args[0].toLowerCase();
        ItemStack sword = null;

        switch (swordType) {
            case "hyperion":
                sword = NecronItemFactory.createHyperion();
                break;
            case "astraea":
                sword = NecronItemFactory.createAstraea();
                break;
            case "valkyrie":
                sword = NecronItemFactory.createValkyrie();
                break;
            case "scylla":
                sword = NecronItemFactory.createScylla();
                break;
            case "aotv":
                sword = NecronItemFactory.createAotV();
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown sword type: " + swordType);
                player.sendMessage(ChatColor.YELLOW + "Available: hyperion, astraea, valkyrie, scylla, aotv");
                return true;
        }

        // Give the sword to the player
        player.getInventory().addItem(sword);
        player.sendMessage(ChatColor.GREEN + "✓ Received " + ChatColor.stripColor(sword.getItemMeta().getDisplayName()));

        return true;
    }
}
