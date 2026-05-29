package com.aspectxlol.breadmines.drops.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.aspectxlol.breadmines.storage.SqliteRepositoryBase;

public class DropRepository extends SqliteRepositoryBase {

    private static final String DB_NAME = "drops_registry.db";
    private static final String TABLE_NAME = "block_drops";

    public DropRepository(JavaPlugin plugin) {
        super(plugin, DB_NAME);
    }

    @Override
    protected void onCreateTables(Statement stmt) throws SQLException {
        stmt.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "block_name TEXT PRIMARY KEY, " +
                "item_id TEXT NOT NULL" +
                ")");
    }

    public String findItemId(String blockName) throws SQLException {
        String query = "SELECT item_id FROM " + TABLE_NAME + " WHERE block_name = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            pstmt.setString(1, blockName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("item_id");
                }
            }
        }
        return null;
    }

    public void upsert(String blockName, String itemId) throws SQLException {
        String query = "INSERT OR REPLACE INTO " + TABLE_NAME + " (block_name, item_id) VALUES (?, ?)";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            pstmt.setString(1, blockName);
            pstmt.setString(2, itemId);
            pstmt.executeUpdate();
        }
    }

    public void delete(String blockName) throws SQLException {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE block_name = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            pstmt.setString(1, blockName);
            pstmt.executeUpdate();
        }
    }

    public List<String[]> fetchAll() throws SQLException {
        List<String[]> entries = new ArrayList<>();
        String query = "SELECT block_name, item_id FROM " + TABLE_NAME + " ORDER BY block_name";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                entries.add(new String[]{
                        rs.getString("block_name"),
                        rs.getString("item_id")
                });
            }
        }

        return entries;
    }

    public List<String> fetchAllNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String query = "SELECT block_name FROM " + TABLE_NAME + " ORDER BY block_name";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                names.add(rs.getString("block_name"));
            }
        }

        return names;
    }
}
