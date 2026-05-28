package com.aspectxlol.breadmines.skyblock.enchantments;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.skyblock.manager.ManaManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SkyblockEnchantmentManager implements SkyblockEnchantmentApi {

    private static final double BASE_MAX_MANA = 500.0;

    private final Breadmines plugin;
    private final NamespacedKey enchantmentsKey;

    public SkyblockEnchantmentManager(Breadmines plugin) {
        this.plugin = plugin;
        this.enchantmentsKey = new NamespacedKey(plugin, "skyblock_enchantments");
    }

    @Override
    public ItemStack setEnchantment(ItemStack itemStack, SkyblockEnchantmentType type, int level) {
        if (itemStack == null || type == null) {
            return itemStack;
        }

        ItemStack result = itemStack.clone();
        Map<SkyblockEnchantmentType, Integer> enchantments = new LinkedHashMap<>(getEnchantments(result));
        if (level <= 0) {
            enchantments.remove(type);
        } else {
            enchantments.put(type, type.clampLevel(level));
        }

        writeEnchantments(result, enchantments);
        return result;
    }

    @Override
    public ItemStack removeEnchantment(ItemStack itemStack, SkyblockEnchantmentType type) {
        return setEnchantment(itemStack, type, 0);
    }

    @Override
    public int getEnchantmentLevel(ItemStack itemStack, SkyblockEnchantmentType type) {
        if (itemStack == null || type == null) {
            return 0;
        }

        return getEnchantments(itemStack).getOrDefault(type, 0);
    }

    @Override
    public Map<SkyblockEnchantmentType, Integer> getEnchantments(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return Collections.emptyMap();
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return Collections.emptyMap();
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String encoded = container.get(enchantmentsKey, PersistentDataType.STRING);
        if (encoded == null || encoded.isBlank()) {
            return Collections.emptyMap();
        }

        Map<SkyblockEnchantmentType, Integer> enchantments = new EnumMap<>(SkyblockEnchantmentType.class);
        String[] entries = encoded.split("\\|");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                continue;
            }

            SkyblockEnchantmentType type = SkyblockEnchantmentType.fromKey(parts[0]);
            if (type == null) {
                continue;
            }

            try {
                int level = Integer.parseInt(parts[1]);
                if (level > 0) {
                    enchantments.put(type, type.clampLevel(level));
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed entry.
            }
        }

        return enchantments;
    }

    @Override
    public double getBigBrainBonus(ItemStack itemStack) {
        int level = getEnchantmentLevel(itemStack, SkyblockEnchantmentType.BIG_BRAIN);
        return SkyblockEnchantmentType.BIG_BRAIN.getMaxManaBonus(level);
    }

    @Override
    public double getManaRegenBonusPerSecond(ItemStack itemStack) {
        int level = getEnchantmentLevel(itemStack, SkyblockEnchantmentType.MANA_REGEN);
        return SkyblockEnchantmentType.MANA_REGEN.getManaRegenBonusPerSecond(level);
    }

    @Override
    public double getEffectiveMaxMana(Player player) {
        return BASE_MAX_MANA + getTotalBigBrainBonus(player);
    }

    @Override
    public double getEffectiveManaRegenPerSecond(Player player, double baseManaRegenPerSecond) {
        return baseManaRegenPerSecond + getTotalManaRegenBonus(player);
    }

    @Override
    public void refreshPlayerStats(Player player, double baseManaRegenPerSecond) {
        ManaManager manaManager = plugin.getManaManager();
        double maxMana = getEffectiveMaxMana(player);
        double regenPerSecond = getEffectiveManaRegenPerSecond(player, baseManaRegenPerSecond);

        manaManager.setMaxMana(player, maxMana);
        manaManager.setManaRegenRate(player, regenPerSecond / 2.0);
    }

    @Override
    public void refreshAllOnlinePlayers(double baseManaRegenPerSecond) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            refreshPlayerStats(player, baseManaRegenPerSecond);
        }
    }

    private double getTotalBigBrainBonus(Player player) {
        return getAllRelevantItems(player).stream()
            .mapToDouble(this::getBigBrainBonus)
            .sum();
    }

    private double getTotalManaRegenBonus(Player player) {
        return getAllRelevantItems(player).stream()
            .mapToDouble(this::getManaRegenBonusPerSecond)
            .sum();
    }

    private Collection<ItemStack> getAllRelevantItems(Player player) {
        List<ItemStack> items = new ArrayList<>();

        if (player.getInventory() != null) {
            Collections.addAll(items, player.getInventory().getStorageContents());

            EntityEquipment equipment = player.getEquipment();
            if (equipment != null) {
                Collections.addAll(items, equipment.getArmorContents());
                items.add(equipment.getItemInOffHand());
            }
        }

        items.removeIf(itemStack -> itemStack == null || itemStack.getType().isAir());
        return items;
    }

    private void writeEnchantments(ItemStack itemStack, Map<SkyblockEnchantmentType, Integer> enchantments) {
        if (!itemStack.hasItemMeta()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        if (enchantments.isEmpty()) {
            meta.getPersistentDataContainer().remove(enchantmentsKey);
        } else {
            meta.getPersistentDataContainer().set(enchantmentsKey, PersistentDataType.STRING, encodeEnchantments(enchantments));
        }

        itemStack.setItemMeta(meta);
    }

    private String encodeEnchantments(Map<SkyblockEnchantmentType, Integer> enchantments) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<SkyblockEnchantmentType, Integer> entry : enchantments.entrySet()) {
            if (builder.length() > 0) {
                builder.append('|');
            }

            builder.append(entry.getKey().getKey().toLowerCase(Locale.ROOT))
                .append(':')
                .append(entry.getValue());
        }
        return builder.toString();
    }
}