package com.aspectxlol.breadmines.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class SqliteRepositoryBase {
    protected final JavaPlugin plugin;
    protected final String dbName;
    protected Connection dbConnection;

    protected SqliteRepositoryBase(JavaPlugin plugin, String dbName) {
        this.plugin = plugin;
        this.dbName = dbName;
    }

    public void initialize() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        File dbFile = new File(dataFolder, dbName);
        String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        dbConnection = DriverManager.getConnection(dbUrl);

        try (Statement stmt = dbConnection.createStatement()) {
            onCreateTables(stmt);
        }
    }

    protected abstract void onCreateTables(Statement statement) throws SQLException;

    public void close() {
        if (dbConnection == null) return;
        try {
            dbConnection.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
        }
    }
}
