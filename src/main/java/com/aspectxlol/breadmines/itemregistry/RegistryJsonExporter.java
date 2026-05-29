package com.aspectxlol.breadmines.itemregistry;

import com.google.gson.Gson;
import com.aspectxlol.breadmines.util.JsonSerializationHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RegistryJsonExporter {
    private static final Gson GSON = JsonSerializationHelper.gson();

    private RegistryJsonExporter() {}

    public static String export(List<CustomItemDefinition> definitions) {
        var root = new java.util.LinkedHashMap<String, Object>();
        root.put("schema", 1);
        List<Map<String, Object>> items = new ArrayList<>();
        for (CustomItemDefinition d : definitions) items.add(d.serialize());
        root.put("items", items);
        return JsonSerializationHelper.toJson(root);
    }
}
