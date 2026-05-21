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
    private static final double DEFAULT_MANA = 500.0;
    private static final double MAX_MANA = 500.0;
    private static final double MIN_MANA = 0.0;

    public ManaManager() {
        this.playerManaMap = new ConcurrentHashMap<>();
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
     * Automatically clamps value between MIN_MANA (0) and MAX_MANA (500).
     */
    public void setMana(Player player, double amount) {
        double clampedAmount = Math.max(MIN_MANA, Math.min(amount, MAX_MANA));
        playerManaMap.put(player.getUniqueId().toString(), clampedAmount);
    }

    /**
     * Initializes a new player profile with default mana (500).
     * Called when a player joins the server.
     */
    public void initializePlayerMana(Player player) {
        playerManaMap.put(player.getUniqueId().toString(), DEFAULT_MANA);
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
    }

    /**
     * Clears all player mana data (used for server reset scenarios).
     */
    public void clearAllMana() {
        playerManaMap.clear();
    }
}
