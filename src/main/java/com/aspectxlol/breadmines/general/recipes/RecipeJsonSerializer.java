package com.aspectxlol.breadmines.general.recipes;

import com.aspectxlol.breadmines.general.recipes.storage.RecipeRepository;
import com.aspectxlol.breadmines.util.JsonSerializationHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class RecipeJsonSerializer {
    private static final Gson GSON = JsonSerializationHelper.gson();

    private RecipeJsonSerializer() {}

    public static String export(List<RecipeDefinition> recipes) {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();

        for (RecipeDefinition recipe : recipes) {
            JsonObject entry = new JsonObject();
            entry.addProperty("output_key", recipe.getOutputKey());
            entry.addProperty("input_key", recipe.getInputKey());
            entry.addProperty("input_amount", recipe.getInputAmount());
            entry.addProperty("created_at_millis", recipe.getCreatedAtMillis());
            entry.addProperty("updated_at_millis", recipe.getUpdatedAtMillis());
            array.add(entry);
        }

        root.add("recipes", array);
        return JsonSerializationHelper.toJson(root);
    }

    public static List<RecipeDefinition> parse(String json) {
        List<RecipeDefinition> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;

        try {
            JsonElement root = GSON.fromJson(json, JsonElement.class);
            if (root == null || root.isJsonNull()) return result;

            JsonArray array = null;
            if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                if (object.has("recipes") && object.get("recipes").isJsonArray()) {
                    array = object.getAsJsonArray("recipes");
                }
            } else if (root.isJsonArray()) {
                array = root.getAsJsonArray();
            }

            if (array == null) return result;

            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject recipeObject = element.getAsJsonObject();
                String output = recipeObject.has("output_key") ? recipeObject.get("output_key").getAsString() : null;
                String input = recipeObject.has("input_key") ? recipeObject.get("input_key").getAsString() : null;
                int amount = recipeObject.has("input_amount") ? recipeObject.get("input_amount").getAsInt() : 1;
                long createdAt = recipeObject.has("created_at_millis") ? recipeObject.get("created_at_millis").getAsLong() : System.currentTimeMillis();
                long updatedAt = recipeObject.has("updated_at_millis") ? recipeObject.get("updated_at_millis").getAsLong() : System.currentTimeMillis();
                if (output == null || input == null) continue;
                result.add(new RecipeDefinition(output, input, amount, createdAt, updatedAt));
            }
        } catch (Exception ignored) {}

        return result;
    }

    public static boolean importIntoRepository(String json, RecipeRepository repository) {
        List<RecipeDefinition> parsed = parse(json);
        if (parsed == null || parsed.isEmpty()) return false;

        try {
            repository.deleteAll();
        } catch (SQLException ignored) {}

        for (RecipeDefinition def : parsed) {
            try { repository.upsert(def); } catch (SQLException ignored) {}
        }

        return true;
    }
}
