package com.aspectxlol.breadmines.ui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class MenuItemFactory {
    private MenuItemFactory() {}

    public static ItemStack createArrow(Material material, String name, String loreLine) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(loreLine));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public static ItemStack createInfoItem(String name) {
        ItemStack itemStack = new ItemStack(Material.PAPER);
        var meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public static ItemStack createButton(Material material, String name, String loreLine) {
        return createButton(material, name, List.of(loreLine));
    }

    public static ItemStack createButton(Material material, String name, String loreLineOne, String loreLineTwo) {
        return createButton(material, name, List.of(loreLineOne, loreLineTwo));
    }

    public static ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack itemStack = new ItemStack(material);
        var meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public static ItemStack createPane() {
        ItemStack itemStack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
