package com.aspectxlol.breadmines.itemregistry;

import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

public interface CustomItemRegistryApi {

    CustomItemDefinition registerItem(String name, ItemStack itemStack);

    Optional<CustomItemDefinition> getDefinition(String name);

    Optional<CustomItemDefinition> getDefinition(ItemStack itemStack);

    Optional<ItemStack> createItemStack(String name);

    boolean removeItem(String name);

    Collection<CustomItemDefinition> getDefinitions();

    boolean contains(String name);

    String normalizeName(String name);
}