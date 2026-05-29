package com.aspectxlol.breadmines.drops;

import com.aspectxlol.breadmines.drops.storage.DropRepository;
import com.aspectxlol.breadmines.util.JsonSerializationHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.util.List;

public final class DropJsonSerializer {
    private DropJsonSerializer() {}

    public static String export(List<String[]> entries) {
        JsonObject root = new JsonObject();
        JsonArray items = new JsonArray();
        if (entries != null) {
            for (String[] e : entries) {
                if (e == null || e.length < 2) continue;
                JsonObject obj = new JsonObject();
                obj.addProperty("block_name", e[0]);
                obj.addProperty("item_id", e[1]);
                items.add(obj);
            }
        }
        root.add("items", items);
        return JsonSerializationHelper.toJson(root);
    }

    public static boolean importIntoRepository(String json, DropRepository repository) {
        if (json == null || json.isBlank()) return false;
        try {
            JsonElement root = JsonSerializationHelper.gson().fromJson(json, JsonElement.class);
            if (root == null || root.isJsonNull()) return false;

            JsonArray items = null;
            if (root.isJsonObject() && root.getAsJsonObject().has("items")) {
                items = root.getAsJsonObject().getAsJsonArray("items");
            } else if (root.isJsonArray()) {
                items = root.getAsJsonArray();
            }

            if (items == null) return false;

            for (JsonElement elem : items) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                String blockName = obj.has("block_name") ? obj.get("block_name").getAsString() : null;
                String itemId = obj.has("item_id") ? obj.get("item_id").getAsString() : null;
                if (blockName == null || itemId == null) continue;
                repository.upsert(blockName, itemId);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
