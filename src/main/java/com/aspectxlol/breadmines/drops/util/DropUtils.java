package com.aspectxlol.breadmines.drops.util;

import java.util.Locale;

public final class DropUtils {

    private DropUtils() {
    }

    public static String normalizeBlockName(String blockName) {
        if (blockName == null) {
            return "";
        }
        return blockName.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }
}
