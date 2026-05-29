package com.aspectxlol.breadmines.drops.util;

import java.util.Locale;

public final class DropUtils {

    private DropUtils() {
    }

    public static String normalizeBlockName(String blockName) {
        return com.aspectxlol.breadmines.util.NormalizationUtils.normalizeName(blockName);
    }
}
