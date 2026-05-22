package com.aspectxlol.breadmines.skyblock.factory;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

/**
 * NecronItemFactory - Produces custom Necron mythic swords and items.
 * All items are formatted with colored names, lore descriptions, and ability information.
 * 
 * DESIGN NOTE: Static methods allow direct instantiation without factory object creation,
 * keeping memory footprint minimal and improving cache locality.
 */
public class NecronItemFactory {

    /**
     * Creates the Hyperion sword - AoE Magic Damage focused weapon.
     * Material: IRON_SWORD | Mana Cost: 150 | Ability: Wither Implosion
     */
    public static ItemStack createHyperion() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lHyperion"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7The legendary blade of the Wither King"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Focuses on devastating Area of Effect magic"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&c&l✦ WITHER IMPLOSION"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right Click to detonate a burst of"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7wither energy around you, damaging"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7enemies within 5 blocks and applying"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7knockback. Affects nearby allies."));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8Mana Cost: &b150"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&5&lMYTHIC SWORD"));

        meta.setLore(lore);
        sword.setItemMeta(meta);

        return sword;
    }

    /**
     * Creates the Astraea sword - Tank/Defense focused weapon.
     * Material: DIAMOND_SWORD | Mana Cost: 150 | Ability: Wither Healing
     */
    public static ItemStack createAstraea() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lAstraea"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7The divine protector's blade"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Channels healing energy for the party"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&a&l✦ WITHER HEALING"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right Click to channel restorative"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7power, granting Regeneration II and"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Absorption II for 5 seconds to you"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7and nearby allies within 5 blocks."));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8Mana Cost: &b150"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&5&lMYTHIC SWORD"));

        meta.setLore(lore);
        sword.setItemMeta(meta);

        return sword;
    }

    /**
     * Creates the Valkyrie sword - Single-Target DPS focused weapon.
     * Material: DIAMOND_SWORD | Mana Cost: 150 | Ability: Ferocious Dash
     */
    public static ItemStack createValkyrie() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lValkyrie"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7The warrior's blade of strength"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Imbued with the fury of battle"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&e&l✦ FEROCIOUS DASH"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right Click to surge forward with"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7battle lust, granting Strength I"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7for 3 seconds to you and nearby"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7allies within 5 blocks. Deal more"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7damage with each strike!"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8Mana Cost: &b150"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&5&lMYTHIC SWORD"));

        meta.setLore(lore);
        sword.setItemMeta(meta);

        return sword;
    }

    /**
     * Creates the Scylla sword - Crit/Speed focused weapon.
     * Material: DIAMOND_SWORD | Mana Cost: 150 | Ability: Wither Swiftness
     */
    public static ItemStack createScylla() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lScylla"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7The speedster's weapon"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Strikes with unparalleled swiftness"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&6&l✦ WITHER SWIFTNESS"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right Click to channel velocity,"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7granting Speed III for 2.5 seconds"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7to you and nearby allies within"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&75 blocks. Dash across the field!"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8Mana Cost: &b150"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&5&lMYTHIC SWORD"));

        meta.setLore(lore);
        sword.setItemMeta(meta);

        return sword;
    }

    /**
     * Creates the Aspect of the Void shovel - Traversal utility item.
     * Material: DIAMOND_SHOVEL | Mana Cost: 45/100 | Abilities: Ether Transmission / Ether Warp
     */
    public static ItemStack createAotV() {
        ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL, 1);
        ItemMeta meta = shovel.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lAspect of the Void"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7The void itself flows through"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7your hands, granting teleportation"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&d&l&l✦ ETHER TRANSMISSION"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right Click to dash forward up to"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&78 blocks, leaving a trail of void"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7energy in your wake."));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8Mana Cost: &b45"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&d&l&l✦ ETHER WARP"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Shift + Right Click to target a"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7block up to 60 blocks away and"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7instantly teleport to it. Safe"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7landing guaranteed."));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8Mana Cost: &b100"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&5&lEPIC SHOVEL"));

        meta.setLore(lore);
        shovel.setItemMeta(meta);

        return shovel;
    }
}
