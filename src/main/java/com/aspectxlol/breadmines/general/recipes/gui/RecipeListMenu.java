package com.aspectxlol.breadmines.general.recipes.gui;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.general.recipes.RecipeDefinition;
import com.aspectxlol.breadmines.general.recipes.RecipeManager;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class RecipeListMenu {

    private static final int INVENTORY_SIZE = 54;
    private static final int CONTENT_SLOTS = 45;

    private final RecipeManager recipeManager;
    private final CustomItemRegistry itemRegistry;

    public RecipeListMenu(Breadmines plugin) {
        this.recipeManager = plugin.getRecipeManager();
        this.itemRegistry = plugin.getCustomItemRegistry();
    }

    public void open(Player player, int page) {
        player.openInventory(createInventory(page));
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil(recipeManager.getRecipes().size() / (double) CONTENT_SLOTS));
    }

    private Inventory createInventory(int page) {
        List<RecipeDefinition> recipes = recipeManager.getRecipes();
        int totalPages = Math.max(1, (int) Math.ceil(recipes.size() / (double) CONTENT_SLOTS));
        int safePage = Math.max(1, Math.min(page, totalPages));

        Inventory inventory = Bukkit.createInventory(new RecipeListMenuHolder(safePage), INVENTORY_SIZE,
            ChatColor.DARK_GREEN + "Recipes " + ChatColor.GRAY + "(" + safePage + "/" + totalPages + ")");

        fillBackground(inventory);
        placeRecipes(inventory, recipes, safePage);
        placeControls(inventory, safePage, totalPages);
        return inventory;
    }

    private void placeRecipes(Inventory inventory, List<RecipeDefinition> recipes, int page) {
        int startIndex = (page - 1) * CONTENT_SLOTS;
        int endIndex = Math.min(recipes.size(), startIndex + CONTENT_SLOTS);

        int slot = 0;
        for (int index = startIndex; index < endIndex; index++) {
            RecipeDefinition recipe = recipes.get(index);
            inventory.setItem(slot++, createRecipeItem(recipe));
        }

        if (recipes.isEmpty()) {
            inventory.setItem(22, createInfoItem(ChatColor.YELLOW + "No recipes configured"));
        }
    }

    private ItemStack createRecipeItem(RecipeDefinition recipe) {
        ItemStack itemStack = itemRegistry.createItemStack(recipe.getOutputKey()).orElseGet(() -> new ItemStack(Material.PAPER));
        itemStack.setAmount(1);

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(0, ChatColor.GRAY + "Input: " + ChatColor.AQUA + recipe.getInputAmount() + "x " + recipe.getInputKey());
            lore.add(0, ChatColor.GRAY + "Output Key: " + ChatColor.GREEN + recipe.getOutputKey());
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    private void placeControls(Inventory inventory, int page, int totalPages) {
        inventory.setItem(45, createArrow(ChatColor.YELLOW + "Previous Page", page > 1 ? ChatColor.GRAY + "Go to page " + (page - 1) : ChatColor.DARK_GRAY + "No previous page"));
        inventory.setItem(49, createInfoItem(ChatColor.GOLD + "Page " + page + ChatColor.GRAY + " / " + totalPages));
        inventory.setItem(52, createButton(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close recipe list"));
        inventory.setItem(53, createArrow(ChatColor.YELLOW + "Next Page", page < totalPages ? ChatColor.GRAY + "Go to page " + (page + 1) : ChatColor.DARK_GRAY + "No next page"));
    }

    private void fillBackground(Inventory inventory) {
        ItemStack pane = createButton(Material.GRAY_STAINED_GLASS_PANE, " ", " ");
        for (int slot = CONTENT_SLOTS; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, pane);
        }
    }

    private ItemStack createArrow(String name, String loreLine) {
        return createButton(Material.ARROW, name, loreLine);
    }

    private ItemStack createInfoItem(String name) {
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack createButton(Material material, String name, String loreLine) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(loreLine));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
