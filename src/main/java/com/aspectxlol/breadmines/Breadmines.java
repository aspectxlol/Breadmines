package com.aspectxlol.breadmines;

import com.aspectxlol.breadmines.drops.DropSystemHandler;
import com.aspectxlol.breadmines.drops.command.DropCommandHandler;
import com.aspectxlol.breadmines.drops.command.DropTabCompleter;
import com.aspectxlol.breadmines.drops.listener.DropBlockListener;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistryApi;
import com.aspectxlol.breadmines.itemregistry.command.RegistryCommand;
import com.aspectxlol.breadmines.itemregistry.gui.RegistryMenuListener;
import com.aspectxlol.breadmines.skyblock.enchantments.SkyblockEnchantmentApi;
import com.aspectxlol.breadmines.skyblock.enchantments.SkyblockEnchantmentManager;
import com.aspectxlol.breadmines.skyblock.command.GiveSwordCommand;
import com.aspectxlol.breadmines.skyblock.command.GiveSwordTabCompleter;
import com.aspectxlol.breadmines.skyblock.command.ManaCommand;
import com.aspectxlol.breadmines.skyblock.listener.AbilityListener;
import com.aspectxlol.breadmines.skyblock.listener.JoinListener;
import com.aspectxlol.breadmines.skyblock.manager.ManaManager;
import com.aspectxlol.breadmines.util.CommandUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.ServicePriority;
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
    private CustomItemRegistry customItemRegistry;
    private com.aspectxlol.breadmines.itemregistry.storage.ItemRegistryRepository itemRegistryRepository;
    private SkyblockEnchantmentManager skyblockEnchantmentManager;
    private ManaManager manaManager;
    private volatile boolean isSystemEnabled = true;
    private BukkitTask asyncTaskHandle;
    private double manaRegenPerTick = 7.5; // Adjustable for dev testing

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

        if (customItemRegistry != null) {
            customItemRegistry.save();
        }

        if (itemRegistryRepository != null) {
            itemRegistryRepository.close();
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
        skyblockEnchantmentManager = new SkyblockEnchantmentManager(this);
        getServer().getServicesManager().register(SkyblockEnchantmentApi.class, skyblockEnchantmentManager, this, ServicePriority.Normal);

        initializeItemRegistry();

        // Register Skyblock Commands
        getCommand("mana").setExecutor(new ManaCommand(this));
        getCommand("givesword").setExecutor(new GiveSwordCommand());
        getCommand("givesword").setTabCompleter(new GiveSwordTabCompleter());
        
        // Register Dev Commands
        getCommand("manaspeed").setExecutor(this);

        // Register Skyblock Event Listeners
        Bukkit.getPluginManager().registerEvents(new AbilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);

        // Register Skript Addon (if Skript is installed)
        try {
            com.aspectxlol.breadmines.skyblock.skript.BreadminesSkriptAddon.registerSkriptSyntax(this);
        } catch (Exception e) {
            getLogger().warning("Skript not found or addon failed to load. Skript integration disabled.");
        }

        // Start Async Mana Regeneration & Action Bar Scheduler
        startSkyblockStatUpdater();
        startAsyncManaRegenerator();

        getLogger().info(ChatColor.GREEN + "✓ Breadmines Skyblock system initialized!");
    }

    /**
     * Initializes the custom item registry service and command surface.
     */
    private void initializeItemRegistry() {
        itemRegistryRepository = new com.aspectxlol.breadmines.itemregistry.storage.ItemRegistryRepository(this);
        try {
            itemRegistryRepository.initialize();
        } catch (SQLException exception) {
            getLogger().severe("✗ Failed to initialize item registry database: " + exception.getMessage());
            setEnabled(false);
            return;
        }

        customItemRegistry = new CustomItemRegistry(this, itemRegistryRepository);
        customItemRegistry.load();
        getServer().getServicesManager().register(CustomItemRegistryApi.class, customItemRegistry, this, ServicePriority.Normal);

        RegistryCommand registryCommand = new RegistryCommand(this);
        getCommand("registry").setExecutor(registryCommand);
        getCommand("registry").setTabCompleter(registryCommand);

        Bukkit.getPluginManager().registerEvents(new RegistryMenuListener(this), this);

        getLogger().info(ChatColor.GREEN + "✓ Custom item registry initialized!");
    }

    /**
     * Updates player mana-related stats from enchantments on the main thread.
     */
    private void startSkyblockStatUpdater() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!isSystemEnabled || skyblockEnchantmentManager == null || manaManager == null) {
                return;
            }

            skyblockEnchantmentManager.refreshAllOnlinePlayers(getBaseManaRegenPerSecond());
        }, 0L, 20L);
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
                double maxMana = manaManager.getMaxMana(player);
                if (currentMana < maxMana) {
                    double newMana = Math.min(currentMana + manaRegenPerTick, maxMana);
                    manaManager.setMana(player, newMana);
                }

                String actionBar = buildActionBarHUD(player);
                Bukkit.getScheduler().runTask(this, () -> {
                    if (player != null && player.isOnline()) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));
                    }
                });
            }
        }, 0L, 10L);
    }

    /**
     * Builds the action bar HUD string with health and mana information.
     * Format: "❤ [Health]/[MaxHealth] ┃ ✎ [Mana]/[MaxMana]"
     * Health always shown in red, Mana always shown in blue
     */
    @SuppressWarnings("deprecation")
    private String buildActionBarHUD(Player player) {
        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();
        double mana = manaManager.getMana(player);
        double maxMana = manaManager.getMaxMana(player);

        return ChatColor.RED + "❤ " + String.format("%.0f", health) + "/" + String.format("%.0f", maxHealth) + 
               ChatColor.GRAY + " ┃ " + ChatColor.BLUE + "✎ " + String.format("%.0f", mana) + "/" + String.format("%.0f", maxMana);
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
     * Returns the custom item registry implementation.
     */
    public CustomItemRegistry getCustomItemRegistry() {
        return customItemRegistry;
    }

    /**
     * Returns the skyblock enchantment manager implementation.
     */
    public SkyblockEnchantmentManager getSkyblockEnchantmentManager() {
        return skyblockEnchantmentManager;
    }

    /**
     * Returns the enchantment API for other plugins.
     */
    public SkyblockEnchantmentApi getSkyblockEnchantmentApi() {
        return skyblockEnchantmentManager;
    }

    /**
     * Returns the base mana regeneration rate in mana per second.
     */
    public double getBaseManaRegenPerSecond() {
        return manaRegenPerTick * 2.0;
    }

    /**
     * Returns the singleton plugin instance.
     */
    /**
     * Dev command to adjust mana regeneration speed
     * Usage: /manaspeed <value>
     * Example: /manaspeed 15 (sets mana regen to 15 per 10 ticks)
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("manaspeed")) {
            return false;
        }

        if (!CommandUtils.requireOp(sender)) return true;

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Current mana regen speed: " + manaRegenPerTick + " per 10 ticks");
            sender.sendMessage(ChatColor.YELLOW + "Usage: /manaspeed <value>");
            return true;
        }

        try {
            double newSpeed = Double.parseDouble(args[0]);
            if (newSpeed < 0) {
                sender.sendMessage(ChatColor.RED + "Mana regen speed must be positive!");
                return true;
            }
            manaRegenPerTick = newSpeed;
            sender.sendMessage(ChatColor.GREEN + "✓ Mana regen speed set to " + newSpeed + " per 10 ticks");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number: " + args[0]);
            return true;
        }
    }

    public static Breadmines getInstance() {
        return instance;
    }
}
