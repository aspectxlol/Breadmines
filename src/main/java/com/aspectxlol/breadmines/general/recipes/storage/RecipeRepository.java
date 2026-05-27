package com.aspectxlol.breadmines.general.recipes.storage;

import com.aspectxlol.breadmines.general.recipes.RecipeDefinition;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class RecipeRepository {

    private static final String DB_NAME = "recipes.db";
    private static final String TABLE_NAME = "autocompressor_recipes";

    private final JavaPlugin plugin;
    private Connection dbConnection;

    public RecipeRepository(JavaPlugin plugin) {
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

        try (Statement statement = dbConnection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + "output_key TEXT PRIMARY KEY, "
                + "input_key TEXT NOT NULL, "
                + "input_amount INTEGER NOT NULL, "
                + "created_at_millis INTEGER NOT NULL, "
                + "updated_at_millis INTEGER NOT NULL"
                + ")");
        }
    }

    public void close() {
        if (dbConnection == null) {
            return;
        }

        try {
            dbConnection.close();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to close recipe database: " + exception.getMessage());
        }
    }

    public void upsert(RecipeDefinition recipe) throws SQLException {
        String query = "INSERT OR REPLACE INTO " + TABLE_NAME + " (output_key, input_key, input_amount, created_at_millis, updated_at_millis) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, recipe.getOutputKey());
            statement.setString(2, recipe.getInputKey());
            statement.setInt(3, recipe.getInputAmount());
            statement.setLong(4, recipe.getCreatedAtMillis());
            statement.setLong(5, recipe.getUpdatedAtMillis());
            statement.executeUpdate();
        }
    }

    public boolean delete(String outputKey) throws SQLException {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE output_key = ?";

        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, outputKey);
            return statement.executeUpdate() > 0;
        }
    }

    public RecipeDefinition find(String outputKey) throws SQLException {
        String query = "SELECT output_key, input_key, input_amount, created_at_millis, updated_at_millis FROM " + TABLE_NAME + " WHERE output_key = ?";

        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, outputKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new RecipeDefinition(
                    resultSet.getString("output_key"),
                    resultSet.getString("input_key"),
                    resultSet.getInt("input_amount"),
                    resultSet.getLong("created_at_millis"),
                    resultSet.getLong("updated_at_millis")
                );
            }
        }
    }

    public List<RecipeDefinition> fetchAll() throws SQLException {
        List<RecipeDefinition> definitions = new ArrayList<>();
        String query = "SELECT output_key, input_key, input_amount, created_at_millis, updated_at_millis FROM " + TABLE_NAME + " ORDER BY output_key";

        try (Statement statement = dbConnection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                definitions.add(new RecipeDefinition(
                    resultSet.getString("output_key"),
                    resultSet.getString("input_key"),
                    resultSet.getInt("input_amount"),
                    resultSet.getLong("created_at_millis"),
                    resultSet.getLong("updated_at_millis")
                ));
            }
        }

        return definitions;
    }
}
