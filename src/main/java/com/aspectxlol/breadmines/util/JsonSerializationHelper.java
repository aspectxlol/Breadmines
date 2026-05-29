package com.aspectxlol.breadmines.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public final class JsonSerializationHelper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonSerializationHelper() {}

    public static Gson gson() { return GSON; }

    public static String toJson(JsonElement element) { return GSON.toJson(element); }

    public static String toJson(Object obj) { return GSON.toJson(obj); }
}
