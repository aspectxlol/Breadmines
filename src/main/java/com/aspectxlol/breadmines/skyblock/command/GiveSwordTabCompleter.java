package com.aspectxlol.breadmines.skyblock.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GiveSwordTabCompleter implements TabCompleter {

    private static final List<String> SWORDS = Arrays.asList(
            "hyperion",
            "astraea",
            "valkyrie",
            "scylla",
            "aotv"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            return List.of();
        }

        if (args.length != 1) {
            return List.of();
        }

        String current = args[0].toLowerCase();
        return SWORDS.stream()
                .filter(option -> option.startsWith(current))
                .collect(Collectors.toList());
    }
}
