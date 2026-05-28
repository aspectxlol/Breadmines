package com.aspectxlol.breadmines.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class GitHubConfig {

    private final boolean enabled;
    private final String owner;
    private final String repo;
    private final String branch;
    private final String path;
    private final String token;
    private final boolean syncOnStartup;
    private final boolean syncOnSave;

    public GitHubConfig(JavaPlugin plugin, String sectionPrefix, String defaultPath) {
        // Global fallback: top-level 'github' section
        String globalPrefix = "github";

        String enabledKey = sectionPrefix + ".github.enabled";
        String ownerKey = sectionPrefix + ".github.owner";
        String repoKey = sectionPrefix + ".github.repo";
        String branchKey = sectionPrefix + ".github.branch";
        String pathKey = sectionPrefix + ".github.path";
        String tokenKey = sectionPrefix + ".github.token";
        String syncStartupKey = sectionPrefix + ".github.syncOnStartup";
        String syncSaveKey = sectionPrefix + ".github.syncOnSave";

        boolean cfgEnabled = plugin.getConfig().getBoolean(enabledKey, plugin.getConfig().getBoolean(globalPrefix + ".enabled", false));
        String cfgOwner = plugin.getConfig().getString(ownerKey, plugin.getConfig().getString(globalPrefix + ".owner", ""));
        String cfgRepo = plugin.getConfig().getString(repoKey, plugin.getConfig().getString(globalPrefix + ".repo", ""));
        String cfgBranch = plugin.getConfig().getString(branchKey, plugin.getConfig().getString(globalPrefix + ".branch", "main"));
        String cfgPath = plugin.getConfig().getString(pathKey, plugin.getConfig().getString(globalPrefix + ".path", defaultPath == null ? "" : defaultPath));
        String cfgToken = plugin.getConfig().getString(tokenKey, plugin.getConfig().getString(globalPrefix + ".token", ""));
        boolean cfgSyncStartup = plugin.getConfig().getBoolean(syncStartupKey, plugin.getConfig().getBoolean(globalPrefix + ".syncOnStartup", false));
        boolean cfgSyncSave = plugin.getConfig().getBoolean(syncSaveKey, plugin.getConfig().getBoolean(globalPrefix + ".syncOnSave", false));

        // fallback to secrets.yml for token if blank
        if (cfgToken == null || cfgToken.isBlank()) {
            File secretsFile = new File(plugin.getDataFolder(), "secrets.yml");
            if (secretsFile.exists()) {
                YamlConfiguration secrets = YamlConfiguration.loadConfiguration(secretsFile);
                // check section-specific then global
                String secToken = secrets.getString(tokenKey, secrets.getString(globalPrefix + ".token", ""));
                if (secToken != null && !secToken.isBlank()) cfgToken = secToken.trim();
            }
        }

        this.enabled = cfgEnabled;
        this.owner = cfgOwner == null ? "" : cfgOwner.trim();
        this.repo = cfgRepo == null ? "" : cfgRepo.trim();
        this.branch = cfgBranch == null ? "main" : cfgBranch.trim();
        this.path = cfgPath == null ? "" : cfgPath.trim();
        this.token = cfgToken == null ? "" : cfgToken.trim();
        this.syncOnStartup = cfgSyncStartup;
        this.syncOnSave = cfgSyncSave;
    }

    public boolean isEnabled() { return enabled; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public String getBranch() { return branch; }
    public String getPath() { return path; }
    public String getToken() { return token; }
    public boolean isSyncOnStartup() { return syncOnStartup; }
    public boolean isSyncOnSave() { return syncOnSave; }
}
