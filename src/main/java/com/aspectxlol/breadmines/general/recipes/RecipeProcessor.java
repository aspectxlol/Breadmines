package com.aspectxlol.breadmines.general.recipes;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import com.aspectxlol.breadmines.util.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Map;

public final class RecipeProcessor {
    private final Breadmines plugin;
    private final CustomItemRegistry itemRegistry;
    private boolean debugMode;

    public RecipeProcessor(Breadmines plugin, CustomItemRegistry itemRegistry, boolean debugMode) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.debugMode = debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean hasAutoCompressor(PlayerInventory inventory) {
        return InventoryUtils.hasAutoCompressor(inventory, itemRegistry);
    }

    public boolean hasAnyCraftableRecipe(PlayerInventory inventory, List<RecipeDefinition> recipes) {
        for (RecipeDefinition recipe : recipes) {
            if (itemRegistry.createItemStack(recipe.getOutputKey(), false).isEmpty()) continue;
            if (InventoryUtils.countItemsByRegistryKey(inventory, itemRegistry, recipe.getInputKey()) >= recipe.getInputAmount()) return true;
        }
        return false;
    }

    public void processPlayerRecipes(Player player, PlayerInventory inventory, List<RecipeDefinition> recipes) {
        if (player == null || recipes == null || recipes.isEmpty()) return;

        if (debugMode) {
            plugin.getLogger().info("[RECIPE DEBUG] Processing auto compressor for " + player.getName()
                + " with " + recipes.size() + " configured recipes.");
        }

        boolean matchedAnyRecipe = false;

        for (RecipeDefinition recipe : recipes) {
            // output template
            var outputOpt = itemRegistry.createItemStack(recipe.getOutputKey(), false);
            if (outputOpt.isEmpty()) {
                if (debugMode) plugin.getLogger().info("[RECIPE DEBUG] Skipping recipe " + recipe.getOutputKey() + " because output item is not registered.");
                continue;
            }

            ItemStack output = outputOpt.get();
            output.setAmount(1);

            int available = InventoryUtils.countItemsByRegistryKey(inventory, itemRegistry, recipe.getInputKey());
            if (available < recipe.getInputAmount()) {
                if (debugMode) plugin.getLogger().info("[RECIPE DEBUG] Not enough input for " + recipe.getOutputKey() + " (available=" + available + ")");
                continue;
            }

            matchedAnyRecipe = true;
            if (debugMode) plugin.getLogger().info("[RECIPE DEBUG] Matching recipe " + recipe.getOutputKey() + " <= " + recipe.getInputAmount() + "x " + recipe.getInputKey() + " (available=" + available + ")");

            while (available >= recipe.getInputAmount() && canFitOneOutput(inventory, output)) {
                if (!InventoryUtils.consumeItemsByRegistryKey(inventory, itemRegistry, recipe.getInputKey(), recipe.getInputAmount())) {
                    if (debugMode) plugin.getLogger().info("[RECIPE DEBUG] Stopped crafting " + recipe.getOutputKey() + " because the input item could not be consumed.");
                    break;
                }

                var leftovers = inventory.addItem(output.clone());
                if (!leftovers.isEmpty()) leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));

                if (debugMode) plugin.getLogger().info("[RECIPE DEBUG] Crafted 1x " + recipe.getOutputKey() + " for " + player.getName());

                available -= recipe.getInputAmount();
            }

            if (debugMode && !canFitOneOutput(inventory, output)) {
                plugin.getLogger().info("[RECIPE DEBUG] Inventory had no room for " + recipe.getOutputKey() + " after processing.");
            }
        }

        if (debugMode && !matchedAnyRecipe) {
            plugin.getLogger().info("[RECIPE DEBUG] No craftable recipe matched player inventory for " + player.getName() + ".");
        }
    }

    private boolean canFitOneOutput(PlayerInventory inventory, ItemStack output) {
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null) return true;
            if (stack.isSimilar(output) && stack.getAmount() < stack.getMaxStackSize()) return true;
        }
        return false;
    }
}
