package com.aspectxlol.breadmines.skyblock.manager;

import org.bukkit.entity.Player;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ManaManager - Thread-safe player mana profile manager.
 * Uses ConcurrentHashMap to prevent race conditions in multi-threaded environments.
 * All async threads and the main thread can safely access player mana data simultaneously.
 */
public class ManaManager {

    private final ConcurrentHashMap<String, Double> playerManaMap;
    private final ConcurrentHashMap<String, Double> playerManaRegenMap;
    private final ConcurrentHashMap<String, Double> playerMaxManaMap;
    private static final double DEFAULT_MANA = 500.0;
    private static final double DEFAULT_MAX_MANA = 500.0;
    private static final double MIN_MANA = 0.0;
    private static final double DEFAULT_REGEN = 2.0; // 2 mana per tick

    public ManaManager() {
        this.playerManaMap = new ConcurrentHashMap<>();
        this.playerManaRegenMap = new ConcurrentHashMap<>();
        this.playerMaxManaMap = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves the current mana for a player.
     * If player is not registered, returns 0.
     */
    public double getMana(Player player) {
        return playerManaMap.getOrDefault(player.getUniqueId().toString(), 0.0);
    }

    /**
     * Sets mana for a player to a specific amount.
     * Automatically clamps value between MIN_MANA (0) and player's max mana.
     */
    public void setMana(Player player, double amount) {
        double maxMana = getMaxMana(player);
        double clampedAmount = Math.max(MIN_MANA, Math.min(amount, maxMana));
        playerManaMap.put(player.getUniqueId().toString(), clampedAmount);
    }

    /**
     * Initializes a new player profile with default mana (500) and default regen rate.
     * Called when a player joins the server.
     */
    public void initializePlayerMana(Player player) {
        playerManaMap.put(player.getUniqueId().toString(), DEFAULT_MANA);
        playerManaRegenMap.put(player.getUniqueId().toString(), DEFAULT_REGEN);
        playerMaxManaMap.put(player.getUniqueId().toString(), DEFAULT_MAX_MANA);
    }

    /**
     * Checks if a player has enough mana for an ability.
     * Returns true if current mana >= required amount.
     */
    public boolean hasEnoughMana(Player player, double amount) {
        return getMana(player) >= amount;
    }

    /**
     * Deducts a specific amount of mana from a player.
     * Returns true if deduction was successful, false if insufficient mana.
     * Does not deduct if player lacks sufficient mana.
     */
    public boolean deductMana(Player player, double amount) {
        if (!hasEnoughMana(player, amount)) {
            return false;
        }
        double currentMana = getMana(player);
        setMana(player, currentMana - amount);
        return true;
    }

    /**
     * Adds mana to a player's current total (respects max cap).
     */
    public void addMana(Player player, double amount) {
        double currentMana = getMana(player);
        setMana(player, currentMana + amount);
    }

    /**
     * Removes a player's mana profile when they leave the server.
     * Prevents memory leaks from accumulating offline player data.
     */
    public void removePlayerMana(Player player) {
        playerManaMap.remove(player.getUniqueId().toString());
        playerManaRegenMap.remove(player.getUniqueId().toString());
        playerMaxManaMap.remove(player.getUniqueId().toString());
    }

    /**
     * Gets the max mana for a player.
     * If player is not registered, returns DEFAULT_MAX_MANA (500.0).
     */
    public double getMaxMana(Player player) {
        return playerMaxManaMap.getOrDefault(player.getUniqueId().toString(), DEFAULT_MAX_MANA);
    }

    /**
     * Sets the max mana for a player.
     * If current mana exceeds new max, current mana is clamped to the new max.
     */
    public void setMaxMana(Player player, double maxMana) {
        playerMaxManaMap.put(player.getUniqueId().toString(), Math.max(0, maxMana));
        
        // Clamp current mana to new max if needed
        double currentMana = getMana(player);
        if (currentMana > maxMana) {
            setMana(player, maxMana);
        }
    }

    /**
     * Gets the mana regen rate for a player (amount per tick).
     * If player is not registered, returns DEFAULT_REGEN (2.0).
     */
    public double getManaRegenRate(Player player) {
        return playerManaRegenMap.getOrDefault(player.getUniqueId().toString(), DEFAULT_REGEN);
    }

    /**
     * Sets the mana regen rate for a player (amount per tick).
     * Use 0.0 to disable regeneration, or negative values to drain mana.
     */
    public void setManaRegenRate(Player player, double regenRate) {
        playerManaRegenMap.put(player.getUniqueId().toString(), regenRate);
    }

    /**
     * Applies mana regeneration to a player (called once per tick from a task).
     * Respects max mana cap.
     */
    public void applyManaRegeneration(Player player) {
        double currentMana = getMana(player);
        double regenRate = getManaRegenRate(player);
        setMana(player, currentMana + regenRate);
    }

    /**
     * Clears all player mana data (used for server reset scenarios).
     */
    public void clearAllMana() {
        playerManaMap.clear();
    }
}
