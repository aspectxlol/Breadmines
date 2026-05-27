package com.aspectxlol.breadmines.itemregistry.storage;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class ItemRegistryRepository {

    private static final String DB_NAME = "item_registry.db";
    private static final String TABLE_NAME = "custom_items";

    private final JavaPlugin plugin;
    private Connection dbConnection;

    public ItemRegistryRepository(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, DB_NAME);
        String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        dbConnection = DriverManager.getConnection(dbUrl);

        try (Statement stmt = dbConnection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "registry_key TEXT PRIMARY KEY, " +
                "display_name TEXT NOT NULL, " +
                "created_at_millis INTEGER NOT NULL, " +
                "source TEXT NOT NULL, " +
                "item_blob TEXT NOT NULL" +
                ")");
        }
    }

    public void close() {
        if (dbConnection == null) {
            return;
        }

        try {
            dbConnection.close();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to close item registry database: " + exception.getMessage());
        }
    }

    public void upsert(String registryKey, String displayName, long createdAtMillis, String source, ItemStack itemStack) throws SQLException {
        String query = "INSERT OR REPLACE INTO " + TABLE_NAME + " (registry_key, display_name, created_at_millis, source, item_blob) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, registryKey);
            statement.setString(2, displayName);
            statement.setLong(3, createdAtMillis);
            statement.setString(4, source);
            statement.setString(5, encodeItemStack(itemStack));
            statement.executeUpdate();
        }
    }

    public void delete(String registryKey) throws SQLException {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE registry_key = ?";

        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, registryKey);
            statement.executeUpdate();
        }
    }

    public RegistryRow find(String registryKey) throws SQLException {
        String query = "SELECT registry_key, display_name, created_at_millis, source, item_blob FROM " + TABLE_NAME + " WHERE registry_key = ?";

        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, registryKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new RegistryRow(
                    resultSet.getString("registry_key"),
                    resultSet.getString("display_name"),
                    resultSet.getLong("created_at_millis"),
                    resultSet.getString("source"),
                    decodeItemStack(resultSet.getString("item_blob"))
                );
            }
        }
    }

    public List<RegistryRow> fetchAll() throws SQLException {
        List<RegistryRow> rows = new ArrayList<>();
        String query = "SELECT registry_key, display_name, created_at_millis, source, item_blob FROM " + TABLE_NAME + " ORDER BY registry_key";

        try (Statement statement = dbConnection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                rows.add(new RegistryRow(
                    resultSet.getString("registry_key"),
                    resultSet.getString("display_name"),
                    resultSet.getLong("created_at_millis"),
                    resultSet.getString("source"),
                    decodeItemStack(resultSet.getString("item_blob"))
                ));
            }
        }

        return rows;
    }

    public List<String> fetchAllNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String query = "SELECT registry_key FROM " + TABLE_NAME + " ORDER BY registry_key";

        try (Statement statement = dbConnection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                names.add(resultSet.getString("registry_key"));
            }
        }

        return names;
    }

    private String encodeItemStack(ItemStack itemStack) throws SQLException {
        try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
             java.io.ObjectOutputStream objectOutputStream = new java.io.ObjectOutputStream(byteOutputStream)) {
            objectOutputStream.writeObject(itemStack);
            objectOutputStream.flush();
            return Base64.getEncoder().encodeToString(byteOutputStream.toByteArray());
        } catch (IOException exception) {
            throw new SQLException("Failed to serialize registry item", exception);
        }
    }

    private ItemStack decodeItemStack(String encoded) throws SQLException {
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             java.io.ObjectInputStream objectInputStream = new java.io.ObjectInputStream(byteInputStream)) {
            return (ItemStack) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            throw new SQLException("Failed to deserialize registry item", exception);
        }
    }

    public record RegistryRow(String registryKey, String displayName, long createdAtMillis, String source, ItemStack itemStack) {}
}