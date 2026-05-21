package com.aspectxlol.breadmines;

import com.aspectxlol.breadmines.drops.DropSystemHandler;
import com.aspectxlol.breadmines.drops.command.DropCommandHandler;
import com.aspectxlol.breadmines.drops.command.DropTabCompleter;
import com.aspectxlol.breadmines.drops.listener.DropBlockListener;
import com.aspectxlol.breadmines.skyblock.command.GiveSwordCommand;
import com.aspectxlol.breadmines.skyblock.command.ManaCommand;
import com.aspectxlol.breadmines.skyblock.listener.AbilityListener;
import com.aspectxlol.breadmines.skyblock.listener.JoinListener;
import com.aspectxlol.breadmines.skyblock.manager.ManaManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;

/**
 * Breadmines - Main plugin class for both Drop System and Breadmines Skyblock.
 * 
 * ARCHITECTURE:
 * - Initializes the Drop System (block drops registry with database)
 * - Initializes the Breadmines Skyblock system (mana, abilities, items)
 * - Delegates to specialized handlers and managers for clean separation of concerns
 */
public final class Breadmines extends JavaPlugin {

    private static Breadmines instance;
    private DropSystemHandler dropHandler;
    private ManaManager manaManager;
    private volatile boolean isSystemEnabled = true;
    private BukkitTask asyncTaskHandle;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize Drop System
        initializeDropSystem();

        // Initialize Breadmines Skyblock System
        initializeSkyblockSystem();

        getLogger().info(ChatColor.GREEN + "================================");
        getLogger().info(ChatColor.GREEN + "Breadmines plugin fully enabled!");
        getLogger().info(ChatColor.GREEN + "================================");
    }

    @Override
    public void onDisable() {
        // Stop async tasks
        if (asyncTaskHandle != null) {
            asyncTaskHandle.cancel();
        }

        // Close database connection
        if (dropHandler != null) {
            dropHandler.closeDatabase();
        }

        getLogger().info(ChatColor.RED + "Breadmines plugin disabled.");
    }

    /**
     * Initializes the Drop System with database, commands, and event listeners.
     */
    private void initializeDropSystem() {
        try {
            dropHandler = new DropSystemHandler(this);
            dropHandler.initializeDatabase();
            getLogger().info("✓ Drop System: Database initialized successfully.");

            // Register drop system commands and listeners
            getCommand("drops").setExecutor(new DropCommandHandler(this, dropHandler));
            getCommand("drops").setTabCompleter(new DropTabCompleter(this, dropHandler));
            getCommand("dropslist").setExecutor(new DropCommandHandler(this, dropHandler));
            getCommand("dropslist").setTabCompleter(new DropTabCompleter(this, dropHandler));

            getServer().getPluginManager().registerEvents(new DropBlockListener(this, dropHandler), this);

            getLogger().info(ChatColor.GREEN + "✓ Drop System initialized successfully!");

        } catch (SQLException e) {
            getLogger().severe("✗ Failed to initialize Drop System: " + e.getMessage());
            setEnabled(false);
        }
    }

    /**
     * Initializes the Breadmines Skyblock System with mana manager, commands, listeners, and async task.
     */
    private void initializeSkyblockSystem() {
        manaManager = new ManaManager();

        // Register Skyblock Commands
        getCommand("mana").setExecutor(new ManaCommand(this));
        getCommand("givesword").setExecutor(new GiveSwordCommand());

        // Register Skyblock Event Listeners
        Bukkit.getPluginManager().registerEvents(new AbilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);

        // Start Async Mana Regeneration & Action Bar Scheduler
        startAsyncManaRegenerator();

        getLogger().info(ChatColor.GREEN + "✓ Breadmines Skyblock system initialized!");
    }

    /**
     * Starts an asynchronous background task that runs every 10 ticks (0.5 seconds).
     * Handles mana regeneration for all online players and sends synchronized action bar HUD.
     */
    private void startAsyncManaRegenerator() {
        asyncTaskHandle = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (!isSystemEnabled) {
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                double currentMana = manaManager.getMana(player);
                if (currentMana < 500.0) {
                    double newMana = Math.min(currentMana + 7.5, 500.0);
                    manaManager.setMana(player, newMana);
                }

                String actionBar = buildActionBarHUD(player);
                Bukkit.getScheduler().runTask(this, () -> {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));
                });
            }
        }, 0L, 10L);
    }

    /**
     * Builds the action bar HUD string with health and mana information.
     * Format: "❤ [Health]/[MaxHealth] ┃ ✎ [Mana]/500"
     */
    private String buildActionBarHUD(Player player) {
        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();
        double mana = manaManager.getMana(player);

        String healthColor;
        if (health <= maxHealth * 0.25) {
            healthColor = ChatColor.RED.toString();
        } else if (health <= maxHealth * 0.5) {
            healthColor = ChatColor.YELLOW.toString();
        } else {
            healthColor = ChatColor.GREEN.toString();
        }

        String manaColor;
        if (mana <= 100.0) {
            manaColor = ChatColor.DARK_AQUA.toString();
        } else {
            manaColor = ChatColor.AQUA.toString();
        }

        return healthColor + "❤ " + String.format("%.1f", health) + "/" + String.format("%.1f", maxHealth) + 
               ChatColor.GRAY + " ┃ " + manaColor + "✎ " + String.format("%.1f", mana) + "/500";
    }

    /**
     * Globally enables the Breadmines Skyblock system.
     */
    public void enableSystem() {
        isSystemEnabled = true;
        getLogger().info(ChatColor.GREEN + "Breadmines Skyblock system ENABLED");
    }

    /**
     * Globally disables the Breadmines Skyblock system.
     */
    public void disableSystem() {
        isSystemEnabled = false;
        getLogger().info(ChatColor.RED + "Breadmines Skyblock system DISABLED");
    }

    /**
     * Checks if the system is currently enabled.
     */
    public boolean isSystemEnabled() {
        return isSystemEnabled;
    }

    /**
     * Returns the ManaManager instance for accessing player mana data.
     */
    public ManaManager getManaManager() {
        return manaManager;
    }

    /**
     * Returns the DropSystemHandler instance.
     */
    public DropSystemHandler getDropHandler() {
        return dropHandler;
    }

    /**
     * Returns the singleton plugin instance.
     */
    public static Breadmines getInstance() {
        return instance;
    }
}
