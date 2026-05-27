package com.aspectxlol.breadmines.itemregistry.gui;

import com.aspectxlol.breadmines.itemregistry.CustomItemDefinition;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum RegistryItemFilter {
    ALL("All Items") {
        @Override
        public boolean matches(ItemStack itemStack) {
            return true;
        }
    },
    WEAPON("Weapon") {
        @Override
        public boolean matches(ItemStack itemStack) {
            if (itemStack == null) {
                return false;
            }

            String name = itemStack.getType().name();
            return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_BOW")
                || name.endsWith("_CROSSBOW")
                || name.endsWith("_TRIDENT")
                || name.endsWith("_MACE");
        }
    },
    ARMOR("Armor") {
        @Override
        public boolean matches(ItemStack itemStack) {
            if (itemStack == null) {
                return false;
            }

            String name = itemStack.getType().name();
            return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
        }
    },
    PLAYER_HEAD("Player Head") {
        @Override
        public boolean matches(ItemStack itemStack) {
            return itemStack != null && (itemStack.getType() == Material.PLAYER_HEAD || itemStack.getType() == Material.PLAYER_WALL_HEAD);
        }
    },
    ITEM("Item") {
        @Override
        public boolean matches(ItemStack itemStack) {
            return itemStack != null && !WEAPON.matches(itemStack) && !ARMOR.matches(itemStack) && !PLAYER_HEAD.matches(itemStack);
        }
    };

    private final String displayName;

    RegistryItemFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract boolean matches(ItemStack itemStack);

    public boolean matches(CustomItemDefinition definition) {
        return matches(definition == null ? null : definition.getItemStack());
    }

    public RegistryItemFilter next() {
        RegistryItemFilter[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}