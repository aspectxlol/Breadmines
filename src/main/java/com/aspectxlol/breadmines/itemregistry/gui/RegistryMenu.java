package com.aspectxlol.breadmines.itemregistry.gui;

import com.aspectxlol.breadmines.Breadmines;
import com.aspectxlol.breadmines.itemregistry.CustomItemDefinition;
import com.aspectxlol.breadmines.itemregistry.CustomItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.aspectxlol.breadmines.ui.MenuItemFactory;
import com.aspectxlol.breadmines.ui.MenuUtils;

public final class RegistryMenu {

    private static final int INVENTORY_SIZE = 54;
    static final int CONTENT_SLOTS = 45;

    static final int SLOT_PREV_PAGE = 45;
    static final int SLOT_SEARCH = 46;
    static final int SLOT_SORT = 47;
    static final int SLOT_QUERY = 48;
    static final int SLOT_PAGE_INFO = 49;
    static final int SLOT_RESET = 50;
    static final int SLOT_FILLER = 51;
    static final int SLOT_CLOSE = 52;
    static final int SLOT_NEXT_PAGE = 53;

    private final CustomItemRegistry registry;

    public RegistryMenu(Breadmines plugin) {
        this.registry = plugin.getCustomItemRegistry();
    }

    public void open(Player player, int page) {
        player.openInventory(createBrowserInventory(page, RegistrySortMode.NAME_ASC, null));
    }

    public void open(Player player, int page, RegistrySortMode sortMode) {
        player.openInventory(createBrowserInventory(page, sortMode, null));
    }

    public void open(Player player, int page, RegistrySortMode sortMode, String searchQuery) {
        player.openInventory(createBrowserInventory(page, sortMode, searchQuery));
    }

    public Inventory createBrowserInventory(int page, RegistrySortMode sortMode) {
        return createBrowserInventory(page, sortMode, null);
    }

    public Inventory createBrowserInventory(int page, RegistrySortMode sortMode, String searchQuery) {
        List<CustomItemDefinition> definitions = getVisibleDefinitions(sortMode, searchQuery);
        int totalPages = calculateTotalPages(definitions.size());
        int safePage = clampPage(page, totalPages);

        Inventory inventory = Bukkit.createInventory(new RegistryMenuHolder(safePage, sortMode, searchQuery), INVENTORY_SIZE, buildTitle(safePage, totalPages, searchQuery));
        fillBackground(inventory);
        placeItems(inventory, definitions, safePage);
        placeControls(inventory, safePage, totalPages, sortMode, searchQuery);
        return inventory;
    }

    public int getTotalPages() {
        return calculateTotalPages(registry.getDefinitions().size());
    }

    public int getTotalPages(String searchQuery) {
        return calculateTotalPages(getVisibleDefinitions(RegistrySortMode.NAME_ASC, searchQuery).size());
    }

    private void placeItems(Inventory inventory, List<CustomItemDefinition> definitions, int page) {
        int startIndex = (page - 1) * CONTENT_SLOTS;
        int endIndex = Math.min(definitions.size(), startIndex + CONTENT_SLOTS);

        int slot = 0;
        for (int index = startIndex; index < endIndex; index++) {
            CustomItemDefinition definition = definitions.get(index);
            ItemStack displayItem = definition.getItemStack();
            displayItem.setAmount(1);

            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(0, ChatColor.DARK_GRAY + "Registry Key: " + ChatColor.AQUA + definition.getId());
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }

            inventory.setItem(slot++, displayItem);
        }

        if (definitions.isEmpty()) {
            inventory.setItem(22, MenuItemFactory.createInfoItem(ChatColor.YELLOW + "No custom items registered"));
        }
    }

    public CustomItemDefinition getDefinitionAt(int page, int rawSlot, RegistrySortMode sortMode) {
        return getDefinitionAt(page, rawSlot, sortMode, null);
    }

    public CustomItemDefinition getDefinitionAt(int page, int rawSlot, RegistrySortMode sortMode, String searchQuery) {
        if (rawSlot < 0 || rawSlot >= CONTENT_SLOTS) {
            return null;
        }

        List<CustomItemDefinition> definitions = getVisibleDefinitions(sortMode, searchQuery);
        int totalPages = calculateTotalPages(definitions.size());
        int safePage = clampPage(page, totalPages);
        int index = ((safePage - 1) * CONTENT_SLOTS) + rawSlot;

        if (index < 0 || index >= definitions.size()) {
            return null;
        }

        return definitions.get(index);
    }

    private void placeControls(Inventory inventory, int page, int totalPages, RegistrySortMode sortMode, String searchQuery) {
        boolean hasSearchQuery = searchQuery != null && !searchQuery.isBlank();
        String queryValue = hasSearchQuery ? ChatColor.AQUA + searchQuery : ChatColor.DARK_GRAY + "None";
        String queryHint = hasSearchQuery ? "Click to search again" : "Click to search";

        inventory.setItem(SLOT_PREV_PAGE, MenuItemFactory.createArrow(Material.ARROW, ChatColor.YELLOW + "Previous Page", page > 1 ? ChatColor.GRAY + "Go to page " + (page - 1) : ChatColor.DARK_GRAY + "No previous page"));
        inventory.setItem(SLOT_SEARCH, MenuItemFactory.createButton(Material.COMPASS, ChatColor.AQUA + "Search", ChatColor.GRAY + "Open search prompt"));
        inventory.setItem(SLOT_SORT, MenuItemFactory.createButton(Material.HOPPER, ChatColor.YELLOW + "Sort By", ChatColor.GRAY + "Current: " + ChatColor.AQUA + sortMode.getDisplayName(), ChatColor.GRAY + "Click to cycle sort mode"));
        inventory.setItem(SLOT_QUERY, MenuItemFactory.createButton(Material.PAPER, ChatColor.YELLOW + "Search Query", ChatColor.GRAY + "Current: " + queryValue, ChatColor.GRAY + queryHint));
        inventory.setItem(SLOT_PAGE_INFO, MenuItemFactory.createInfoItem(ChatColor.GOLD + "Page " + page + ChatColor.GRAY + " / " + totalPages));
        inventory.setItem(SLOT_RESET, MenuItemFactory.createButton(Material.BOOK, ChatColor.RED + "Reset", ChatColor.GRAY + "Clear search and sorting"));
        inventory.setItem(SLOT_FILLER, MenuItemFactory.createPane());
        inventory.setItem(SLOT_CLOSE, MenuItemFactory.createButton(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close the registry menu"));
        inventory.setItem(SLOT_NEXT_PAGE, MenuItemFactory.createArrow(Material.ARROW, ChatColor.YELLOW + "Next Page", page < totalPages ? ChatColor.GRAY + "Go to page " + (page + 1) : ChatColor.DARK_GRAY + "No next page"));
    }

    private void fillBackground(Inventory inventory) {
        MenuUtils.fillBackground(inventory, CONTENT_SLOTS, INVENTORY_SIZE);
    }

    private String buildTitle(int page, int totalPages, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return ChatColor.DARK_PURPLE + "Custom Items " + ChatColor.GRAY + "(" + page + "/" + totalPages + ")";
        }

        return ChatColor.DARK_PURPLE + "Custom Items " + ChatColor.GRAY + "(" + page + "/" + totalPages + ")" + ChatColor.DARK_GRAY + " Search: " + searchQuery;
    }

    private List<CustomItemDefinition> getVisibleDefinitions(RegistrySortMode sortMode, String searchQuery) {
        List<CustomItemDefinition> definitions = new ArrayList<>(registry.getDefinitions());
        String normalizedQuery = RegistrySearch.normalizeSearchQuery(searchQuery);
        Comparator<CustomItemDefinition> comparator = resolveComparator(sortMode);

        if (!normalizedQuery.isEmpty()) {
            List<String> tokens = tokenizeSearchQuery(normalizedQuery);
            List<ScoredDefinition> scoredDefinitions = new ArrayList<>();
            for (CustomItemDefinition definition : definitions) {
                int score = RegistrySearch.getSearchScore(definition, normalizedQuery, tokens);
                if (score > 0) {
                    scoredDefinitions.add(new ScoredDefinition(definition, score));
                }
            }

            scoredDefinitions.sort(Comparator
                .comparingInt(ScoredDefinition::getScore)
                .reversed()
                .thenComparing(ScoredDefinition::getDefinition, comparator));

            List<CustomItemDefinition> results = new ArrayList<>(scoredDefinitions.size());
            for (ScoredDefinition entry : scoredDefinitions) {
                results.add(entry.getDefinition());
            }
            return results;
        }

        definitions.sort(comparator);
        return definitions;
    }

    private int getSearchScore(CustomItemDefinition definition, String normalizedQuery, List<String> tokens) {
        if (definition == null) {
            return 0;
        }

        ItemStack itemStack = definition.getItemStack();
        String id = normalizeSearchQuery(definition.getId());
        String displayName = normalizeSearchQuery(definition.getDisplayName());
        String material = normalizeSearchQuery(itemStack.getType().name());
        String lore = normalizeSearchQuery(joinLore(itemStack));

        int score = 0;
        score += scoreField(id, normalizedQuery, tokens, 500, 350, 50);
        score += scoreField(displayName, normalizedQuery, tokens, 400, 300, 40);
        score += scoreField(material, normalizedQuery, tokens, 250, 200, 20);
        score += scoreField(lore, normalizedQuery, tokens, 100, 75, 10);
        return score;
    }

    private int scoreField(String field, String normalizedQuery, List<String> tokens, int exactScore, int prefixScore, int tokenScore) {
        if (field == null || field.isEmpty()) {
            return 0;
        }

        int score = 0;
        if (field.equals(normalizedQuery)) {
            score += exactScore;
        } else if (field.startsWith(normalizedQuery)) {
            score += prefixScore;
        } else if (field.contains(normalizedQuery)) {
            score += tokenScore * 2;
        }

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }

            if (field.equals(token)) {
                score += exactScore / 4;
            } else if (field.startsWith(token)) {
                score += prefixScore / 4;
            } else if (field.contains(token)) {
                score += tokenScore;
            }
        }

        return score;
    }

    private String joinLore(ItemStack itemStack) {
        if (itemStack == null) {
            return "";
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return "";
        }

        return String.join(" ", meta.getLore());
    }

    private String normalizeSearchQuery(String value) {
        if (value == null) {
            return "";
        }

        return ChatColor.stripColor(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private List<String> tokenizeSearchQuery(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return List.of();
        }

        String[] parts = normalizedQuery.split("\\s+");
        List<String> tokens = new ArrayList<>();
        Collections.addAll(tokens, parts);
        return tokens;
    }

    

    private int calculateTotalPages(int totalItems) {
        return MenuUtils.calculateTotalPages(totalItems, CONTENT_SLOTS);
    }

    private int clampPage(int page, int totalPages) {
        return MenuUtils.clampPage(page, totalPages);
    }

    private Comparator<CustomItemDefinition> resolveComparator(RegistrySortMode sortMode) {
        RegistrySortMode resolved = sortMode == null ? RegistrySortMode.NAME_ASC : sortMode;
        return resolved.getComparator();
    }

    private static final class ScoredDefinition {
        private final CustomItemDefinition definition;
        private final int score;

        private ScoredDefinition(CustomItemDefinition definition, int score) {
            this.definition = definition;
            this.score = score;
        }

        private CustomItemDefinition getDefinition() {
            return definition;
        }

        private int getScore() {
            return score;
        }
    }
}