package com.aspectxlol.breadmines.skyblock.enchantments;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public interface SkyblockEnchantmentApi {

    ItemStack setEnchantment(ItemStack itemStack, SkyblockEnchantmentType type, int level);

    ItemStack removeEnchantment(ItemStack itemStack, SkyblockEnchantmentType type);

    int getEnchantmentLevel(ItemStack itemStack, SkyblockEnchantmentType type);

    Map<SkyblockEnchantmentType, Integer> getEnchantments(ItemStack itemStack);

    double getBigBrainBonus(ItemStack itemStack);

    double getManaRegenBonusPerSecond(ItemStack itemStack);

    double getEffectiveMaxMana(Player player);

    double getEffectiveManaRegenPerSecond(Player player, double baseManaRegenPerSecond);

    void refreshPlayerStats(Player player, double baseManaRegenPerSecond);

    void refreshAllOnlinePlayers(double baseManaRegenPerSecond);
}