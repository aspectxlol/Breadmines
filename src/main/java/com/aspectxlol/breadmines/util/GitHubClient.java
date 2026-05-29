package com.aspectxlol.breadmines.util;

import com.aspectxlol.breadmines.Breadmines;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class GitHubClient {
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_USER_AGENT = "BreadminesRecipesSync";
    private static final int GITHUB_TIMEOUT_MS = 10000;

    private final Breadmines plugin;
    private final Gson gson = new Gson();
    private final String owner;
    private final String repo;
    private final String branch;
    private final String path;
    private final String token;

    public GitHubClient(Breadmines plugin, String owner, String repo, String branch, String path, String token) {
        this.plugin = plugin;
        this.owner = owner;
        this.repo = repo;
        this.branch = branch;
        this.path = path;
        this.token = token;
    }

    public boolean isConfigured() {
        return owner != null && !owner.isBlank()
            && repo != null && !repo.isBlank()
            && branch != null && !branch.isBlank()
            && path != null && !path.isBlank()
            && token != null && !token.isBlank();
    }

    public GitHubFile fetchFile() {
        if (!isConfigured()) return null;
        GitHubResponse response = sendGithubRequest("GET", buildGithubContentUrl(), token, null);
        if (response == null || response.status == 404) return null;
        if (response.status < 200 || response.status >= 300) {
            if (plugin != null) plugin.getLogger().warning("GitHub fetch failed (" + response.status + "): " + response.body);
            return null;
        }

        try {
            JsonObject json = gson.fromJson(response.body, JsonObject.class);
            if (json == null) return null;
            String sha = getJsonString(json, "sha");
            String contentEncoded = getJsonString(json, "content");
            String encoding = getJsonString(json, "encoding");
            if (sha == null || contentEncoded == null || encoding == null) return null;
            String content = contentEncoded;
            if ("base64".equalsIgnoreCase(encoding)) {
                content = new String(Base64.getDecoder().decode(contentEncoded.replaceAll("\\s", "")), StandardCharsets.UTF_8);
            }
            return new GitHubFile(sha, content);
        } catch (Exception e) {
            if (plugin != null) plugin.getLogger().warning("GitHub fetch parse failed: " + e.getMessage());
            return null;
        }
    }

    public boolean pushFile(String json, String sha, String message) {
        if (!isConfigured()) return false;
        String body = buildGithubPutPayload(json, sha, message);
        GitHubResponse response = sendGithubRequest("PUT", buildGithubContentUrl(), token, body);
        if (response == null) return false;
        if (response.status >= 200 && response.status < 300) {
            try {
                JsonObject resp = gson.fromJson(response.body, JsonObject.class);
                if (resp != null && resp.has("content")) {
                    JsonObject content = resp.getAsJsonObject("content");
                    if (content != null && content.has("sha")) {
                        // Optionally log or store sha externally
                    }
                }
            } catch (Exception ignored) {}
            return true;
        }
        if (plugin != null) plugin.getLogger().warning("GitHub push failed (" + response.status + "): " + response.body);
        return false;
    }

    public static boolean isSameJson(String left, String right) {
        if (left == null || right == null) return false;
        try {
            JsonElement l = new Gson().fromJson(left, JsonElement.class);
            JsonElement r = new Gson().fromJson(right, JsonElement.class);
            if (l == null || r == null) return left.equals(right);
            return l.equals(r);
        } catch (Exception e) {
            return left.equals(right);
        }
    }

    private GitHubResponse sendGithubRequest(String method, String url, String token, String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(GITHUB_TIMEOUT_MS);
            connection.setReadTimeout(GITHUB_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", GITHUB_USER_AGENT);
            if (token != null && !token.isBlank()) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = connection.getOutputStream()) { out.write(body.getBytes(StandardCharsets.UTF_8)); }
            }
            int status = connection.getResponseCode();
            try (InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()) {
                String responseBody = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                return new GitHubResponse(status, responseBody);
            }
        } catch (IOException e) {
            if (plugin != null) plugin.getLogger().warning("GitHub request failed: " + e.getMessage());
            return null;
        } finally { if (connection != null) connection.disconnect(); }
    }

    private String buildGithubPutPayload(String json, String sha, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message == null || message.isBlank() ? "Update registry" : message);
        payload.addProperty("content", Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
        payload.addProperty("branch", branch);
        if (sha != null && !sha.isBlank()) payload.addProperty("sha", sha);
        return gson.toJson(payload);
    }

    private String buildGithubContentUrl() {
        try {
            String encodedPath = URLEncoder.encode(path == null ? "" : path, StandardCharsets.UTF_8.name()).replace("+", "%20");
            String encodedBranch = URLEncoder.encode(branch == null ? "" : branch, StandardCharsets.UTF_8.name()).replace("+", "%20");
            return GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/contents/" + encodedPath + "?ref=" + encodedBranch;
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 encoding unsupported", ex);
        }
    }

    private String getJsonString(JsonObject object, String key) { if (object == null || key == null || !object.has(key)) return null; JsonElement v = object.get(key); if (v == null || v.isJsonNull()) return null; return v.getAsString(); }

    public static final class GitHubFile { public final String sha; public final String content; public GitHubFile(String sha, String content) { this.sha = sha; this.content = content; } }
    private static final class GitHubResponse { private final int status; private final String body; private GitHubResponse(int status, String body) { this.status = status; this.body = body; } }
}
