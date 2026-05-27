package com.aspectxlol.breadmines.itemregistry.gui;

import com.aspectxlol.breadmines.itemregistry.CustomItemDefinition;

import java.util.Comparator;

public enum RegistrySortMode {
    NAME_ASC("Name A-Z", Comparator.comparing(definition -> definition.getDisplayName().toLowerCase())),
    NAME_DESC("Name Z-A", Comparator.comparing((CustomItemDefinition definition) -> definition.getDisplayName().toLowerCase()).reversed()),
    TYPE_ASC("Type A-Z", Comparator.comparing(definition -> definition.getItemStack().getType().name())),
    TYPE_DESC("Type Z-A", Comparator.comparing((CustomItemDefinition definition) -> definition.getItemStack().getType().name()).reversed()),
    NEWEST("Newest", Comparator.comparingLong(CustomItemDefinition::getCreatedAtMillis).reversed()),
    OLDEST("Oldest", Comparator.comparingLong(CustomItemDefinition::getCreatedAtMillis));

    private final String displayName;
    private final Comparator<CustomItemDefinition> comparator;

    RegistrySortMode(String displayName, Comparator<CustomItemDefinition> comparator) {
        this.displayName = displayName;
        this.comparator = comparator.thenComparing(CustomItemDefinition::getId);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Comparator<CustomItemDefinition> getComparator() {
        return comparator;
    }

    public RegistrySortMode next() {
        RegistrySortMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}