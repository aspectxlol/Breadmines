package com.aspectxlol.breadmines.drops;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class BlockTypeRegistry {

    private static final Set<Material> EXCLUDED = EnumSet.of(
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.BEDROCK,
            Material.BARRIER,
            Material.END_PORTAL,
            Material.END_PORTAL_FRAME,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.JIGSAW,
            Material.LIGHT,
            Material.REINFORCED_DEEPSLATE
    );

    private static final List<String> BREAKABLE_BLOCKS = buildBreakableBlockTypes();

    private BlockTypeRegistry() {
    }

    private static List<String> buildBreakableBlockTypes() {
        List<String> blocks = Arrays.stream(Material.values())
                .filter(Material::isBlock)
                .filter(material -> !material.isAir())
                .filter(material -> !EXCLUDED.contains(material))
                .map(material -> material.name().toLowerCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.toList());

        return Collections.unmodifiableList(blocks);
    }

    public static List<String> getBreakableBlockTypes() {
        return BREAKABLE_BLOCKS;
    }
}
