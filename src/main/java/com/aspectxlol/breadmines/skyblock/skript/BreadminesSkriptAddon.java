package com.aspectxlol.breadmines.skyblock.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Skript Addon for Breadmines Mana System
 * Provides Skript expressions and effects for accessing and modifying player mana.
 */
public class BreadminesSkriptAddon {

    private static SkriptAddon addon;

    /**
     * Registers all Skript syntax (expressions and effects) for the Breadmines mana system.
     * Call this from your plugin's onEnable() method.
     */
    public static void registerSkriptSyntax(JavaPlugin plugin) {
        if (addon == null) {
            addon = Skript.registerAddon(plugin);
        }

        try {
            // Register expressions
            addon.loadClasses("com.aspectxlol.breadmines.skyblock.skript.expressions");
            
            // Register effects
            addon.loadClasses("com.aspectxlol.breadmines.skyblock.skript.effects");
            
            plugin.getLogger().info("✓ Breadmines Skript addon loaded successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("✗ Failed to register Breadmines Skript syntax!");
            e.printStackTrace();
        }
    }
}
