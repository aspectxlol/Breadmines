package com.aspectxlol.breadmines.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class NormalizationUtils {
    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^a-z0-9]+", Pattern.UNICODE_CASE);
    private NormalizationUtils() {}

    public static String normalizeName(String name) {
        if (name == null) return "";
        String normalized = NORMALIZE_PATTERN.matcher(name.trim().toLowerCase(Locale.ROOT)).replaceAll("_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized;
    }
}
