package com.aspectxlol.breadmines.general.recipes.storage;

import com.aspectxlol.breadmines.general.recipes.RecipeDefinition;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aspectxlol.breadmines.storage.SqliteRepositoryBase;

public final class RecipeRepository extends SqliteRepositoryBase {

    private static final String DB_NAME = "recipes.db";
    private static final String TABLE_NAME = "autocompressor_recipes";
    private static final String UNIQUE_INDEX_NAME = "idx_autocompressor_recipes_conflict";

    public RecipeRepository(JavaPlugin plugin) {
        super(plugin, DB_NAME);
    }

    @Override
    protected void onCreateTables(Statement statement) throws SQLException {
        if (!tableExists(statement)) {
            statement.execute("CREATE TABLE " + TABLE_NAME + " ("
                + "output_key TEXT NOT NULL, "
                + "input_key TEXT NOT NULL, "
                + "input_amount INTEGER NOT NULL, "
                + "created_at_millis INTEGER NOT NULL, "
                + "updated_at_millis INTEGER NOT NULL"
                + ")");
        } else if (needsSchemaUpgrade(statement)) {
            migrateLegacyTable(statement);
        }

        statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS " + UNIQUE_INDEX_NAME + " ON " + TABLE_NAME
            + " (output_key, input_key, input_amount)");
    }

    private boolean tableExists(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_NAME + "'")) {
            return resultSet.next();
        }
    }

    private boolean needsSchemaUpgrade(Statement statement) throws SQLException {
        boolean hasOutputPrimaryKey = false;
        boolean hasInputKey = false;
        boolean hasInputAmount = false;

        try (ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + TABLE_NAME + ")")) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("name");
                int primaryKeyPosition = resultSet.getInt("pk");
                if ("output_key".equalsIgnoreCase(columnName) && primaryKeyPosition > 0) {
                    hasOutputPrimaryKey = true;
                }
                if ("input_key".equalsIgnoreCase(columnName)) {
                    hasInputKey = true;
                }
                if ("input_amount".equalsIgnoreCase(columnName)) {
                    hasInputAmount = true;
                }
            }
        }

        return hasOutputPrimaryKey || !hasInputKey || !hasInputAmount;
    }

    private void migrateLegacyTable(Statement statement) throws SQLException {
        String legacyTable = TABLE_NAME + "_legacy_" + System.currentTimeMillis();
        statement.execute("ALTER TABLE " + TABLE_NAME + " RENAME TO " + legacyTable);

        statement.execute("CREATE TABLE " + TABLE_NAME + " ("
            + "output_key TEXT NOT NULL, "
            + "input_key TEXT NOT NULL, "
            + "input_amount INTEGER NOT NULL, "
            + "created_at_millis INTEGER NOT NULL, "
            + "updated_at_millis INTEGER NOT NULL"
            + ")");

        String legacyQuery = "SELECT output_key, input_key, input_amount, created_at_millis, updated_at_millis FROM " + legacyTable;
        Map<String, RecipeDefinition> deduped = new LinkedHashMap<>();

        try (ResultSet resultSet = statement.executeQuery(legacyQuery)) {
            while (resultSet.next()) {
                RecipeDefinition definition = new RecipeDefinition(
                    resultSet.getString("output_key"),
                    resultSet.getString("input_key"),
                    resultSet.getInt("input_amount"),
                    resultSet.getLong("created_at_millis"),
                    resultSet.getLong("updated_at_millis")
                );
                String key = definition.getOutputKey() + "|" + definition.getInputKey() + "|" + definition.getInputAmount();
                deduped.putIfAbsent(key, definition);
            }
        }

        String insertQuery = "INSERT INTO " + TABLE_NAME + " (output_key, input_key, input_amount, created_at_millis, updated_at_millis) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = dbConnection.prepareStatement(insertQuery)) {
            for (RecipeDefinition definition : deduped.values()) {
                preparedStatement.setString(1, definition.getOutputKey());
                preparedStatement.setString(2, definition.getInputKey());
                preparedStatement.setInt(3, definition.getInputAmount());
                preparedStatement.setLong(4, definition.getCreatedAtMillis());
                preparedStatement.setLong(5, definition.getUpdatedAtMillis());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }

        statement.execute("DROP TABLE " + legacyTable);
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
