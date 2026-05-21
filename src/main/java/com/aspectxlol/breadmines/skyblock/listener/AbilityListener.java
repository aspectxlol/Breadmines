package com.aspectxlol.breadmines.skyblock.listener;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.skyblock.manager.ManaManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * AbilityListener - Handles right-click ability triggers for custom Necron items.
 * 
 * THREAD SAFETY: 
 * - PlayerInteractEvent fires on main thread (safe)
 * - All player modifications (teleport, damage, potion) happen on main thread
 * - Async mana calculations are already thread-safe via ConcurrentHashMap
 * 
 * DESIGN:
 * - Validates global system enabled state before executing abilities
 * - Detects items by their colored display names (stripped for comparison)
 * - Prevents block interaction when holding custom items
 * - Universal teleport engine with block collision checks
 * - Individual ability effects with particles and sounds
 */
public class AbilityListener implements Listener {

    private final Breadmines plugin;
    private final ManaManager manaManager;

    public AbilityListener(Breadmines plugin) {
        this.plugin = plugin;
        this.manaManager = plugin.getManaManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Cancel if system is disabled
        if (!plugin.isSystemEnabled()) {
            return;
        }

        // Only process right-click actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();

        // Check if player is holding a custom item
        if (heldItem == null || !heldItem.hasItemMeta() || !heldItem.getItemMeta().hasDisplayName()) {
            return;
        }

        String displayName = ChatColor.stripColor(heldItem.getItemMeta().getDisplayName());
        
        // Cancel block interaction/placement
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() != Material.AIR) {
            event.setCancelled(true);
        }

        // Route to appropriate ability handler
        switch (displayName) {
            case "Hyperion":
                handleHyperion(player, event);
                break;
            case "Astraea":
                handleAstraea(player, event);
                break;
            case "Valkyrie":
                handleValkyrie(player, event);
                break;
            case "Scylla":
                handleScylla(player, event);
                break;
            case "Aspect of the Void":
                handleAspectOfTheVoid(player, event);
                break;
        }
    }

    /**
     * Hyperion Ability: Wither Implosion
     * Spawns AoE particles, plays explosion sound, and damages non-player entities within 5-block radius by 24 damage (12 hearts).
     * Mana Cost: 150
     */
    private void handleHyperion(Player player, PlayerInteractEvent event) {
        if (!manaManager.deductMana(player, 150.0)) {
            player.sendMessage(ChatColor.RED + "✗ Insufficient mana (need 150)");
            return;
        }

        Location playerLoc = player.getLocation();

        // Damage entities in 5-block radius
        for (Entity entity : player.getNearbyEntities(5.0, 5.0, 5.0)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                ((LivingEntity) entity).damage(24.0);
            }
        }

        // Effects - All scheduled on main thread (already on main thread from event)
        playerLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, playerLoc, 20, 2.0, 2.0, 2.0, 0.1);
        playerLoc.getWorld().spawnParticle(Particle.EXPLOSION, playerLoc, 15, 1.5, 1.5, 1.5, 0.1);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        player.sendMessage(ChatColor.LIGHT_PURPLE + "⚔ Hyperion: Wither Implosion activated!");
    }

    /**
     * Astraea Ability: Wither Healing
     * Instantly heals the player by 4 health points (2 hearts).
     * Plays level-up and villager happy sounds with healing particles.
     * Mana Cost: 150
     */
    private void handleAstraea(Player player, PlayerInteractEvent event) {
        if (!manaManager.deductMana(player, 150.0)) {
            player.sendMessage(ChatColor.RED + "✗ Insufficient mana (need 150)");
            return;
        }

        Location playerLoc = player.getLocation();

        // Heal player (4 health = 2 hearts)
        double newHealth = Math.min(player.getHealth() + 4.0, player.getMaxHealth());
        player.setHealth(newHealth);

        // Effects
        playerLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, playerLoc, 30, 1.5, 1.5, 1.5, 0.2);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_VILLAGER_YES, 0.8f, 1.0f);

        player.sendMessage(ChatColor.GREEN + "⚔ Astraea: Wither Healing activated!");
    }

    /**
     * Valkyrie Ability: Ferocious Dash
     * Applies Strength I for 3 seconds (60 ticks).
     * Plays iron golem attack sound with critical particle effects.
     * Mana Cost: 150
     */
    private void handleValkyrie(Player player, PlayerInteractEvent event) {
        if (!manaManager.deductMana(player, 150.0)) {
            player.sendMessage(ChatColor.RED + "✗ Insufficient mana (need 150)");
            return;
        }

        Location playerLoc = player.getLocation();

        // Apply Strength I for 3 seconds (60 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, true, false));

        // Effects
        playerLoc.getWorld().spawnParticle(Particle.CRIT, playerLoc, 25, 1.0, 1.0, 1.0, 0.3);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);

        player.sendMessage(ChatColor.YELLOW + "⚔ Valkyrie: Ferocious Dash activated!");
    }

    /**
     * Scylla Ability: Wither Swiftness
     * Applies Speed III for 2.5 seconds (50 ticks).
     * Plays horse gallop sound with cloud particles.
     * Mana Cost: 150
     */
    private void handleScylla(Player player, PlayerInteractEvent event) {
        if (!manaManager.deductMana(player, 150.0)) {
            player.sendMessage(ChatColor.RED + "✗ Insufficient mana (need 150)");
            return;
        }

        Location playerLoc = player.getLocation();

        // Apply Speed III for 2.5 seconds (50 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50, 2, true, false));

        // Effects
        playerLoc.getWorld().spawnParticle(Particle.CLOUD, playerLoc, 20, 1.5, 1.5, 1.5, 0.2);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_HORSE_GALLOP, 1.0f, 1.0f);

        player.sendMessage(ChatColor.AQUA + "⚔ Scylla: Wither Swiftness activated!");
    }

    /**
     * Aspect of the Void Ability: Ether Transmission (Teleport)
     * 
     * CRITICAL THREAD SAFETY:
     * - Teleportation (player.teleport()) happens on the main thread (we're already here from event)
     * - No async-to-sync scheduling needed since PlayerInteractEvent fires on main thread
     * - Mana deduction uses thread-safe ConcurrentHashMap (safe from async thread)
     * 
     * Features:
     * - Universal teleport engine with 8-block line-of-sight raycast
     * - Block collision checks to prevent clipping into solid blocks
     * - Preserves player head pitch and yaw
     * - Plays portal particles and enderman teleport sound
     * - Mana Cost: 45
     */
    private void handleAspectOfTheVoid(Player player, PlayerInteractEvent event) {
        if (!manaManager.deductMana(player, 45.0)) {
            player.sendMessage(ChatColor.RED + "✗ Insufficient mana (need 45)");
            return;
        }

        Location playerLoc = player.getLocation();
        Location teleportLoc = calculateTeleportLocation(player);

        if (teleportLoc == null) {
            // Refund mana if teleport is blocked
            manaManager.addMana(player, 45.0);
            player.sendMessage(ChatColor.RED + "✗ Cannot teleport: Obstruction detected");
            return;
        }

        // Preserve pitch and yaw
        teleportLoc.setPitch(player.getLocation().getPitch());
        teleportLoc.setYaw(player.getLocation().getYaw());

        // Teleport player
        player.teleport(teleportLoc);

        // Effects
        playerLoc.getWorld().spawnParticle(Particle.PORTAL, playerLoc, 15, 0.5, 0.5, 0.5, 0.5);
        teleportLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, teleportLoc, 15, 0.5, 0.5, 0.5, 0.3);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 1.2f);
        teleportLoc.getWorld().playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);

        player.sendMessage(ChatColor.DARK_PURPLE + "⚔ Aspect of the Void: Ether Transmission activated!");
    }

    /**
     * Calculates safe teleport location using raycast.
     * Scans 8 blocks forward in player's look direction.
     * Returns null if obstruction detected (prevents clipping).
     */
    private Location calculateTeleportLocation(Player player) {
        Location startLoc = player.getLocation().clone();
        Vector direction = startLoc.getDirection().normalize();

        // Raycast 8 blocks forward
        for (int i = 1; i <= 8; i++) {
            Location checkLoc = startLoc.add(direction.clone().multiply(i));
            Block block = checkLoc.getBlock();
            Block blockAbove = checkLoc.clone().add(0, 1, 0).getBlock();

            // Check if both current and above blocks are non-solid (safe to stand)
            if (!isSolid(block) && !isSolid(blockAbove)) {
                // Ground check: ensure there's a solid block below
                Block blockBelow = checkLoc.clone().add(0, -1, 0).getBlock();
                if (isSolid(blockBelow)) {
                    checkLoc.setY(checkLoc.getY() + 0.5); // Add small offset for player feet
                    return checkLoc;
                }
            }
        }

        return null; // No safe location found
    }

    /**
     * Checks if a block is solid and should block teleportation.
     * Ignores water, lava, and other non-blocking materials.
     */
    private boolean isSolid(Block block) {
        Material type = block.getType();
        return type.isSolid() && type != Material.WATER && type != Material.LAVA;
    }
}
