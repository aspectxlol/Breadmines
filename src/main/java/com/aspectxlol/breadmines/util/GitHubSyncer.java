package com.aspectxlol.breadmines.util;

import com.aspectxlol.breadmines.Breadmines;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Generic GitHub sync helper used by multiple registries.
 */
public final class GitHubSyncer {
    private final Breadmines plugin;
    private final GitHubClient githubClient;
    private volatile String lastSyncedSha;

    public GitHubSyncer(Breadmines plugin, GitHubClient githubClient) {
        this.plugin = plugin;
        this.githubClient = githubClient;
    }

    public static final class SyncResult {
        public final boolean success;
        public final boolean importedRemote;
        public final boolean pushedLocal;
        public final String remoteSha;

        private SyncResult(boolean success, boolean importedRemote, boolean pushedLocal, String remoteSha) {
            this.success = success;
            this.importedRemote = importedRemote;
            this.pushedLocal = pushedLocal;
            this.remoteSha = remoteSha;
        }

        public static SyncResult of(boolean success, boolean importedRemote, boolean pushedLocal, String remoteSha) {
            return new SyncResult(success, importedRemote, pushedLocal, remoteSha);
        }
    }

    /**
     * Synchronize using provided exporter/importer callbacks.
     * exporter: supplies the current local JSON representation (never null)
     * importer: attempts to import remote JSON and returns true on success
     */
    public synchronized SyncResult sync(Supplier<String> exporter, Function<String, Boolean> importer, String initialPushMessage, String conflictPushMessage) {
        if (!githubClient.isConfigured()) {
            plugin.getLogger().warning("GitHub sync not configured; skipping.");
            return SyncResult.of(false, false, false, null);
        }

        GitHubClient.GitHubFile remote = githubClient.fetchFile();
        String localJson = exporter == null ? "" : exporter.get();

        if (remote == null) {
            boolean pushed = githubClient.pushFile(localJson, null, initialPushMessage);
            return SyncResult.of(pushed, false, pushed, null);
        }

        if (lastSyncedSha != null && !lastSyncedSha.equals(remote.sha)) {
            if (!GitHubClient.isSameJson(remote.content, localJson)) {
                plugin.getLogger().warning("Registry sync conflict detected; remote changed. Skipping push.");
                return SyncResult.of(false, false, false, remote.sha);
            }
            lastSyncedSha = remote.sha;
            return SyncResult.of(true, false, false, remote.sha);
        }

        if (!GitHubClient.isSameJson(remote.content, localJson)) {
            boolean imported = false;
            try {
                imported = importer.apply(remote.content);
            } catch (Exception e) {
                plugin.getLogger().warning("Import failed: " + e.getMessage());
            }
            if (imported) {
                lastSyncedSha = remote.sha;
                return SyncResult.of(true, true, false, remote.sha);
            }

            boolean pushed = githubClient.pushFile(localJson, remote.sha, conflictPushMessage);
            return SyncResult.of(pushed, false, pushed, remote.sha);
        }

        lastSyncedSha = remote.sha;
        return SyncResult.of(true, false, false, remote.sha);
    }

    /** Push local JSON to GitHub (no last-sha checks). */
    public boolean pushLocal(Supplier<String> exporter, String message) {
        if (!githubClient.isConfigured()) return false;
        try {
            String payload = exporter == null ? "" : exporter.get();
            return githubClient.pushFile(payload, null, message);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to push local file: " + e.getMessage());
            return false;
        }
    }

    public String getLastSyncedSha() { return lastSyncedSha; }
    public void setLastSyncedSha(String sha) { this.lastSyncedSha = sha; }
}
