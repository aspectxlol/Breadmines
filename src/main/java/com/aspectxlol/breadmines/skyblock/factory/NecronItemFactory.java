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

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5Hyperion"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Focuses on AoE Magic Damage"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&c&l⚔ Ability: Wither Implosion"));
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

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5Astraea"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Focuses on Tank/Defense"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&a&l⚔ Ability: Wither Healing"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8Mana Cost: &b150"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&5&lMYTHIC SWORD"));

        meta.setLore(lore);
        sword.setItemMeta(meta);

        return sword;
    }

    /**
     * Creates the Valkyrie sword - Single-Target DPS focused weapon.
     * Material: DIAMOND_SWORD | Mana Cost: 150 | Ability: Ferocious Dash (+50% Ferocity Buff)
     */
    public static ItemStack createValkyrie() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5Valkyrie"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Focuses on Single-Target DPS"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&e&l⚔ Ability: Ferocious Dash"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8(+50% Ferocity Buff)"));
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

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5Scylla"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Focuses on Crit/Speed"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&6&l⚔ Ability: Wither Swiftness"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8Mana Cost: &b150"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&5&lMYTHIC SWORD"));

        meta.setLore(lore);
        sword.setItemMeta(meta);

        return sword;
    }

    /**
     * Creates the Aspect of the Void shovel - Traversal utility item.
     * Material: DIAMOND_SHOVEL | Mana Cost: 45 | Ability: Ether Transmission (Teleport)
     */
    public static ItemStack createAotV() {
        ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL, 1);
        ItemMeta meta = shovel.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5Aspect of the Void"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Traversal & Agility Utility"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&d&l⚔ Ability: Ether Transmission"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8(Safe teleport forward)"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8Mana Cost: &b45"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&5&lEPIC SHOVEL"));

        meta.setLore(lore);
        shovel.setItemMeta(meta);

        return shovel;
    }
}
