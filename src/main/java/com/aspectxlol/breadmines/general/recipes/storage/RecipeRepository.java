package com.aspectxlol.breadmines.general.recipes.storage;

import com.aspectxlol.breadmines.general.recipes.RecipeDefinition;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.aspectxlol.breadmines.storage.SqliteRepositoryBase;

public final class RecipeRepository extends SqliteRepositoryBase {

    private static final String DB_NAME = "recipes.db";
    private static final String TABLE_NAME = "autocompressor_recipes";

    public RecipeRepository(JavaPlugin plugin) {
        super(plugin, DB_NAME);
    }

    @Override
    protected void onCreateTables(Statement statement) throws SQLException {
        statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + "output_key TEXT NOT NULL, "
            + "input_key TEXT NOT NULL, "
            + "input_amount INTEGER NOT NULL, "
            + "created_at_millis INTEGER NOT NULL, "
            + "updated_at_millis INTEGER NOT NULL, "
            + "PRIMARY KEY (output_key, input_key, input_amount)"
            + ")");
    }

    public void upsert(RecipeDefinition recipe) throws SQLException {
        String query = "INSERT INTO " + TABLE_NAME + " (output_key, input_key, input_amount, created_at_millis, updated_at_millis) VALUES (?, ?, ?, ?, ?) "
            + "ON CONFLICT(output_key, input_key, input_amount) DO UPDATE SET "
            + "created_at_millis = MIN(" + TABLE_NAME + ".created_at_millis, excluded.created_at_millis), "
            + "updated_at_millis = excluded.updated_at_millis";

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

    public boolean delete(String outputKey, String inputKey, int inputAmount) throws SQLException {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE output_key = ? AND input_key = ? AND input_amount = ?";

        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, outputKey);
            statement.setString(2, inputKey);
            statement.setInt(3, inputAmount);
            return statement.executeUpdate() > 0;
        }
    }

    public void deleteAll() throws SQLException {
        String query = "DELETE FROM " + TABLE_NAME;

        try (Statement statement = dbConnection.createStatement()) {
            statement.executeUpdate(query);
        }
    }

    public RecipeDefinition find(String outputKey) throws SQLException {
        String query = "SELECT output_key, input_key, input_amount, created_at_millis, updated_at_millis FROM " + TABLE_NAME + " WHERE output_key = ? ORDER BY updated_at_millis DESC, input_key ASC, input_amount ASC LIMIT 1";

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

    public List<RecipeDefinition> findAll(String outputKey) throws SQLException {
        List<RecipeDefinition> definitions = new ArrayList<>();
        String query = "SELECT output_key, input_key, input_amount, created_at_millis, updated_at_millis FROM " + TABLE_NAME + " WHERE output_key = ? ORDER BY updated_at_millis DESC, input_key ASC, input_amount ASC";

        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, outputKey);
            try (ResultSet resultSet = statement.executeQuery()) {
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
        }

        return definitions;
    }

    public List<RecipeDefinition> fetchAll() throws SQLException {
        List<RecipeDefinition> definitions = new ArrayList<>();
        String query = "SELECT output_key, input_key, input_amount, created_at_millis, updated_at_millis FROM " + TABLE_NAME + " ORDER BY output_key, input_key, input_amount";

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
