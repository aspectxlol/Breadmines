package com.aspectxlol.breadmines.skyblock.listener;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.skyblock.manager.ManaManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.entity.Entity;

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
    private BukkitTask etherwarpPreviewTask;

    public AbilityListener(Breadmines plugin) {
        this.plugin = plugin;
        this.manaManager = plugin.getManaManager();
        startEtherwarpPreviewTask();
    }

    /**
     * Starts a repeating task that shows preview particles for Etherwarp when player shifts.
     */
    private void startEtherwarpPreviewTask() {
        etherwarpPreviewTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.isSystemEnabled()) {
                    return;
                }

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player == null || !player.isSneaking()) {
                        continue;
                    }

                    ItemStack heldItem = player.getInventory().getItemInMainHand();
                    if (heldItem == null || !heldItem.hasItemMeta()) {
                        continue;
                    }

                    var itemMeta = heldItem.getItemMeta();
                    if (!itemMeta.hasDisplayName()) {
                        continue;
                    }

                    String displayName = ChatColor.stripColor(itemMeta.getDisplayName());
                    
                    // Show preview only for Aspect of the Void
                    if (!displayName.equals("Aspect of the Void")) {
                        continue;
                    }

                    // Show Ether Warp target particles at crosshair target location
                    Block targetBlock = player.getTargetBlockExact(60, FluidCollisionMode.NEVER);
                    if (targetBlock != null && targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.BARRIER) {
                        Block feetBlock = targetBlock.getRelative(org.bukkit.block.BlockFace.UP);
                        Block headBlock = feetBlock.getRelative(org.bukkit.block.BlockFace.UP);
                        if (!feetBlock.getType().isSolid() && !headBlock.getType().isSolid() && feetBlock.getType() != Material.BARRIER && headBlock.getType() != Material.BARRIER) {
                            Location targetLoc = targetBlock.getLocation().clone().add(0.5, 1.0, 0.5);
                            if (player.getLocation().distance(targetLoc) <= 60.0) {
                                // spawnParticle on the player object means it's client-side only and visible only to them!
                                player.spawnParticle(Particle.ENCHANT, targetLoc, 2, 0.2, 0.2, 0.2, 0.05);
                                player.spawnParticle(Particle.END_ROD, targetLoc, 1, 0.1, 0.1, 0.1, 0.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Update every 2 ticks
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        
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
     * Spawns particles and damages nearby entities.
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
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).damage(24.0);
                if (entity instanceof Player) {
                    // Simulate vanilla TNT knockback mechanics
                    org.bukkit.util.Vector knockback = entity.getLocation().toVector().subtract(playerLoc.toVector());
                    if (knockback.lengthSquared() > 0) {
                        knockback = knockback.normalize().multiply(1.5).setY(0.4);
                        entity.setVelocity(knockback);
                    }
                }
            }
        }

        // Effects
        playerLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, playerLoc, 20, 2.0, 2.0, 2.0, 0.1);
        playerLoc.getWorld().spawnParticle(Particle.EXPLOSION, playerLoc, 15, 1.5, 1.5, 1.5, 0.1);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
    }

    /**
     * Astraea Ability: Wither Healing
     * Applies Regeneration and Absorption to player and nearby players.
     * Plays level-up and villager happy sounds with healing particles.
     * Mana Cost: 150
     */
    private void handleAstraea(Player player, PlayerInteractEvent event) {
        if (!manaManager.deductMana(player, 150.0)) {
            player.sendMessage(ChatColor.RED + "✗ Insufficient mana (need 150)");
            return;
        }

        Location playerLoc = player.getLocation();

        // Apply Regeneration and Absorption for 5 seconds (100 ticks)
        PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 100, 1, true, false);
        PotionEffect absorption = new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, true, false);
        player.addPotionEffect(regen);
        player.addPotionEffect(absorption);

        // Apply to nearby players as well
        for (Entity entity : player.getNearbyEntities(5.0, 5.0, 5.0)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).addPotionEffect(regen);
                ((Player) entity).addPotionEffect(absorption);
            }
        }

        // Effects
        playerLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, playerLoc, 20, 2.0, 2.0, 2.0, 0.1);
        playerLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, playerLoc, 30, 1.5, 1.5, 1.5, 0.2);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_VILLAGER_YES, 0.8f, 1.0f);
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
        Location teleportLoc = calculateTeleportLocation(player, 10.0);
        player.teleport(teleportLoc);

        // Apply Strength I for 3 seconds (60 ticks)
        PotionEffect strength = new PotionEffect(PotionEffectType.STRENGTH, 60, 0, true, false);
        player.addPotionEffect(strength);

        // Apply to nearby players as well
        for (Entity entity : player.getNearbyEntities(5.0, 5.0, 5.0)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).addPotionEffect(strength);
            }
        }

        // Effects
        playerLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, playerLoc, 20, 2.0, 2.0, 2.0, 0.1);
        teleportLoc.getWorld().spawnParticle(Particle.CRIT, teleportLoc, 25, 1.0, 1.0, 1.0, 0.3);
        teleportLoc.getWorld().playSound(teleportLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
    }

    /**
     * Scylla Ability: Wither Swiftness
     * Applies Speed III for 2.5 seconds (50 ticks) to player and nearby players.
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
        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 50, 2, true, false);
        player.addPotionEffect(speed);

        // Apply to nearby players as well
        for (Entity entity : player.getNearbyEntities(5.0, 5.0, 5.0)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).addPotionEffect(speed);
            }
        }

        // Effects
        playerLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, playerLoc, 20, 2.0, 2.0, 2.0, 0.1);
        playerLoc.getWorld().spawnParticle(Particle.CLOUD, playerLoc, 20, 1.5, 1.5, 1.5, 0.2);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_HORSE_GALLOP, 1.0f, 1.0f);
    }

    /**
     * Aspect of the Void Ability: Ether Transmission / Ether Warp (Teleport)
     * 
     * CRITICAL THREAD SAFETY:
     * - Teleportation (player.teleport()) happens on the main thread (we're already here from event)
     * - No async-to-sync scheduling needed since PlayerInteractEvent fires on main thread
     * - Mana deduction uses thread-safe ConcurrentHashMap (safe from async thread)
     * 
     * Features:
     * - RIGHT CLICK: Transmission - Forward dash up to 8 blocks with particle effects
     * - SHIFT + RIGHT CLICK: Ether Warp - Targeted teleport up to 60 blocks with barrier protection
     * - Block collision checks to prevent clipping into solid blocks
     * - Preserves player's current pitch and yaw
     * - Shows preview particles on shift
     */
    private void handleAspectOfTheVoid(Player player, PlayerInteractEvent event) {
        if (player.isSneaking()) {
            // ETHER WARP - Shift + Right Click (60 block max range)
            handleEtherWarp(player, event);
        } else {
            // TRANSMISSION - Right Click (8 block forward dash)
            handleTransmission(player, event);
        }
    }

    /**
     * Transmission Ability: Short-range forward dash
     * Teleports player 8 blocks forward with particle effects.
     * Mana Cost: 45
     */
    private void handleTransmission(Player player, PlayerInteractEvent event) {
        if (!manaManager.deductMana(player, 45.0)) {
            player.sendMessage(ChatColor.RED + "✗ Insufficient mana (need 45)");
            return;
        }

        Location playerLoc = player.getLocation();
        Location teleportLoc = calculateTeleportLocation(player, 8.0);

        // Teleport player
        player.teleport(teleportLoc);

        // Effects
        playerLoc.getWorld().spawnParticle(Particle.PORTAL, playerLoc, 15, 0.5, 0.5, 0.5, 0.5);
        teleportLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, teleportLoc, 15, 0.5, 0.5, 0.5, 0.3);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 1.2f);
        teleportLoc.getWorld().playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
    }

    /**
     * Ether Warp Ability: Long-range targeted teleport
     * Teleports player to targeted block up to 60 blocks away.
     * Blocks barriers and checks for safe landing spots.
     * Mana Cost: 100
     */
    private void handleEtherWarp(Player player, PlayerInteractEvent event) {
        if (!manaManager.deductMana(player, 100.0)) {
            player.sendMessage(ChatColor.RED + "✗ Insufficient mana (need 100)");
            return;
        }

        Location playerLoc = player.getLocation();
        Block targetBlock = player.getTargetBlockExact(60, org.bukkit.FluidCollisionMode.NEVER);

        // Validate target block
        if (targetBlock == null || targetBlock.getType() == Material.AIR || targetBlock.getType() == Material.BARRIER) {
            manaManager.addMana(player, 100.0);
            player.sendMessage(ChatColor.RED + "✗ No valid target block found");
            return;
        }

        // Check if landing zone is safe (2 blocks of air above)
        Block feetBlock = targetBlock.getRelative(org.bukkit.block.BlockFace.UP);
        Block headBlock = feetBlock.getRelative(org.bukkit.block.BlockFace.UP);

        if (feetBlock.getType().isSolid() || headBlock.getType().isSolid() || feetBlock.getType() == Material.BARRIER || headBlock.getType() == Material.BARRIER) {
            manaManager.addMana(player, 100.0);
            player.sendMessage(ChatColor.RED + "✗ Landing zone is obstructed");
            return;
        }

        // Teleport location is right on top of the block
        Location targetLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
        if (playerLoc.distance(targetLoc) > 60.0) {
            manaManager.addMana(player, 100.0);
            player.sendMessage(ChatColor.RED + "✗ Target is too far away");
            return;
        }

        targetLoc.setYaw(player.getLocation().getYaw());
        targetLoc.setPitch(player.getLocation().getPitch());

        // Effects Before
        playerLoc.getWorld().spawnParticle(Particle.PORTAL, playerLoc, 20, 0.8, 0.8, 0.8, 0.6);
        playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.3f);
        
        // Teleport player
        player.teleport(targetLoc);

        // Effects After
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 12, 0.5, 0.5, 0.5, 0.3);
        targetLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, targetLoc, 20, 0.8, 0.8, 0.8, 0.4);
        targetLoc.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 15, 0.5, 0.5, 0.5, 0.2);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.1f);
    }

    /**
     * Simple, reliable teleport location calculation.
     * Steps forward 0.5 blocks at a time until a solid block is hit.
     * Allows air teleportation.
     */
    private Location calculateTeleportLocation(Player player, double maxDist) {
        Location loc = player.getLocation().clone();
        org.bukkit.util.Vector dir = loc.getDirection().normalize();

        Location lastSafe = loc.clone();

        for (double d = 0.5; d <= maxDist; d += 0.5) {
            Location check = loc.clone().add(dir.clone().multiply(d));
            
            Block feet = check.getBlock();
            Block head = check.clone().add(0, 1.0, 0).getBlock();

            // Stop if we hit a solid block
            if (feet.getType().isSolid() || head.getType().isSolid() || feet.getType() == Material.BARRIER || head.getType() == Material.BARRIER) {
                break;
            }

            lastSafe = check;
        }

        lastSafe.setYaw(loc.getYaw());
        lastSafe.setPitch(loc.getPitch());
        return lastSafe;
    }
}
