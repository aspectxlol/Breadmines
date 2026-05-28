package com.aspectxlol.breadmines.general.recipes;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.general.recipes.storage.RecipeRepository;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RecipeManager {

    private static final String AUTO_COMPRESSOR_KEY = "auto_compressor";

    private final Breadmines plugin;
    private final CustomItemRegistry itemRegistry;
    private final RecipeRepository repository;
    private final Map<String, RecipeDefinition> recipes = new ConcurrentHashMap<>();

    public RecipeManager(Breadmines plugin) {
        this.plugin = plugin;
        this.itemRegistry = plugin.getCustomItemRegistry();
        this.repository = new RecipeRepository(plugin);
    }

    public void initialize() throws SQLException {
        repository.initialize();
        load();
    }

    public void shutdown() {
        repository.close();
    }

    public synchronized void load() throws SQLException {
        recipes.clear();
        for (RecipeDefinition definition : repository.fetchAll()) {
            recipes.put(definition.getOutputKey(), definition);
        }
    }

    public synchronized RecipeDefinition createOrUpdateRecipe(String outputKey, int inputAmount, String inputKey) throws SQLException {
        String normalizedOutputKey = itemRegistry.normalizeName(outputKey);
        String normalizedInputKey = itemRegistry.normalizeName(inputKey);

        if (normalizedOutputKey.isEmpty() || normalizedInputKey.isEmpty()) {
            throw new IllegalArgumentException("Recipe output and input keys must not be empty.");
        }

        if (inputAmount <= 0) {
            throw new IllegalArgumentException("Input amount must be greater than 0.");
        }

        if (!itemRegistry.contains(normalizedOutputKey)) {
            throw new IllegalArgumentException("Unknown output item key: " + normalizedOutputKey);
        }

        if (!itemRegistry.contains(normalizedInputKey)) {
            throw new IllegalArgumentException("Unknown input item key: " + normalizedInputKey);
        }

        if (normalizedOutputKey.equals(normalizedInputKey) && inputAmount <= 1) {
            throw new IllegalArgumentException("Recipe would loop forever. Use input amount > 1 for same input/output key.");
        }

        long now = System.currentTimeMillis();
        RecipeDefinition existing = recipes.get(normalizedOutputKey);
        long createdAt = existing != null ? existing.getCreatedAtMillis() : now;

        RecipeDefinition recipe = new RecipeDefinition(normalizedOutputKey, normalizedInputKey, inputAmount, createdAt, now);
        repository.upsert(recipe);
        recipes.put(normalizedOutputKey, recipe);
        return recipe;
    }

    public synchronized boolean deleteRecipe(String outputKey) throws SQLException {
        String normalizedOutputKey = itemRegistry.normalizeName(outputKey);
        boolean removed = repository.delete(normalizedOutputKey);
        if (removed) {
            recipes.remove(normalizedOutputKey);
        }
        return removed;
    }

    public RecipeDefinition findRecipe(String outputKey) {
        return recipes.get(itemRegistry.normalizeName(outputKey));
    }

    public List<RecipeDefinition> getRecipes() {
        List<RecipeDefinition> list = new ArrayList<>(recipes.values());
        list.sort(Comparator.comparing(RecipeDefinition::getOutputKey));
        return list;
    }

    public List<String> getRecipeOutputKeys() {
        List<String> keys = new ArrayList<>(recipes.keySet());
        keys.sort(String::compareTo);
        return keys;
    }

    public void processAutoCompressors() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            PlayerInventory inventory = player.getInventory();
            if (!hasAutoCompressor(inventory)) {
                continue;
            }

            processPlayerRecipes(player, inventory);
        }
    }

    private boolean hasAutoCompressor(PlayerInventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (isRegistryKey(itemStack, AUTO_COMPRESSOR_KEY)) {
                return true;
            }
        }

        for (ItemStack armorItem : inventory.getArmorContents()) {
            if (isRegistryKey(armorItem, AUTO_COMPRESSOR_KEY)) {
                return true;
            }
        }

        return isRegistryKey(inventory.getItemInOffHand(), AUTO_COMPRESSOR_KEY);
    }

    private boolean isRegistryKey(ItemStack itemStack, String key) {
        if (itemStack == null) {
            return false;
        }

        return itemRegistry.getItemId(itemStack)
            .map(itemRegistry::normalizeName)
            .map(normalized -> normalized.equals(key))
            .orElse(false);
    }

    private void processPlayerRecipes(Player player, PlayerInventory inventory) {
        for (RecipeDefinition recipe : getRecipes()) {
            Optional<ItemStack> outputTemplate = itemRegistry.createItemStack(recipe.getOutputKey());
            if (outputTemplate.isEmpty()) {
                continue;
            }

            ItemStack output = outputTemplate.get();
            output.setAmount(1);

            int available = countItemsByRegistryKey(inventory, recipe.getInputKey());
            if (available < recipe.getInputAmount()) {
                continue;
            }

            while (available >= recipe.getInputAmount() && canFitOneOutput(inventory, output)) {
                if (!consumeItemsByRegistryKey(inventory, recipe.getInputKey(), recipe.getInputAmount())) {
                    break;
                }

                Map<Integer, ItemStack> leftovers = inventory.addItem(output.clone());
                if (!leftovers.isEmpty()) {
                    leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                }

                available -= recipe.getInputAmount();
            }
        }
    }

    private int countItemsByRegistryKey(PlayerInventory inventory, String key) {
        int total = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null) {
                continue;
            }

            if (isRegistryKey(stack, key)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private boolean consumeItemsByRegistryKey(PlayerInventory inventory, String key, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || !isRegistryKey(stack, key)) {
                continue;
            }

            int stackAmount = stack.getAmount();
            if (stackAmount <= remaining) {
                contents[slot] = null;
                remaining -= stackAmount;
            } else {
                stack.setAmount(stackAmount - remaining);
                contents[slot] = stack;
                remaining = 0;
            }

            if (remaining == 0) {
                inventory.setStorageContents(contents);
                return true;
            }
        }

        inventory.setStorageContents(contents);
        return false;
    }

    private boolean canFitOneOutput(PlayerInventory inventory, ItemStack output) {
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null) {
                return true;
            }

            if (stack.isSimilar(output) && stack.getAmount() < stack.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }
}
