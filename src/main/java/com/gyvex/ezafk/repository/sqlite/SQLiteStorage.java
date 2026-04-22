package com.gyvex.ezafk.repository.sqlite;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.Model;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.DataStore;
import com.github.ezframework.jaloquent.store.sql.JdbcStore;
import com.github.ezframework.javaquerybuilder.query.sql.SqlDialect;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.repository.AfkTimeModel;
import com.gyvex.ezafk.repository.StorageRepository;

import com.gyvex.ezafk.repository.migration.MigrationRunner;
import com.gyvex.ezafk.repository.migration.SqlMigration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite storage backend using Jaloquent's ModelRepository with a JdbcStore
 * backed by a DriverManager JDBC connection.
 */
public class SQLiteStorage implements StorageRepository, DataStore, JdbcStore {

    private static final Map<String, String> TABLE_COLUMNS = Map.of(
            "id",      "TEXT PRIMARY KEY",
            "seconds", "INTEGER NOT NULL DEFAULT 0"
    );

    private Connection connection;
    private ModelRepository<AfkTimeModel> repo;

    @Override
    public void init() throws Exception {
        java.io.File dataFolder = Registry.get().getPlugin().getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        java.io.File dbFile = new java.io.File(dataFolder, "ezafk.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        new MigrationRunner(this,
                new SqlMigration(1, "Create afk_times table",
                        "CREATE TABLE IF NOT EXISTS afk_times " +
                        "(id TEXT PRIMARY KEY, seconds INTEGER NOT NULL DEFAULT 0)")
        ).run();

        TableRegistry.register(AfkTimeModel.TABLE_PREFIX, "afk_times", TABLE_COLUMNS);
        repo = new ModelRepository<>(this, AfkTimeModel.TABLE_PREFIX, AfkTimeModel.FACTORY,
                SqlDialect.SQLITE);
    }

    @Override
    public Map<UUID, Long> loadAll() {
        final Map<UUID, Long> result = new java.util.HashMap<>();
        try {
            for (final AfkTimeModel m : repo.query(Model.queryBuilder().build())) {
                try {
                    result.put(UUID.fromString(m.getId()), m.getSeconds());
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (Exception e) {
            Registry.get().getLogger().warning("SQLite loadAll failed: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void savePlayerAfkTime(UUID player, long seconds) {
        if (player == null) return;
        final AfkTimeModel model = new AfkTimeModel(player.toString());
        model.setSeconds(seconds);
        try {
            repo.save(model);
        } catch (Exception e) {
            Registry.get().getLogger().warning("SQLite save failed: " + e.getMessage());
        }
    }

    @Override
    public void deletePlayer(UUID player) {
        if (player == null || connection == null) return;
        try {
            repo.delete(player.toString());
        } catch (Exception e) {
            Registry.get().getLogger().warning("SQLite delete failed: " + e.getMessage());
        }
    }

    @Override
    public long getPlayerAfkTime(UUID player) {
        if (player == null) return 0L;
        try {
            return repo.find(player.toString())
                    .map(AfkTimeModel::getSeconds)
                    .orElse(0L);
        } catch (Exception e) {
            Registry.get().getLogger().warning("SQLite read failed: " + e.getMessage());
            return 0L;
        }
    }

    @Override
    public void saveAll() {
        // Writes are immediate via repo.save(); nothing to flush.
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // JdbcStore — called by ModelRepository on the SQL path
    // =========================================================================

    @Override
    public List<Map<String, Object>> query(String sql, List<Object> params) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParams(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapResultSet(rs);
            }
        }
    }

    @Override
    public int executeUpdate(String sql, List<Object> params) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParams(stmt, params);
            return stmt.executeUpdate();
        }
    }

    // =========================================================================
    // DataStore — flat-map path not used when TableRegistry entry exists
    // =========================================================================

    @Override
    public void save(String path, Map<String, Object> data) throws Exception {
        throw new StorageException("SQLiteStorage uses the SQL path via TableRegistry");
    }

    @Override
    public Optional<Map<String, Object>> load(String path) throws Exception {
        throw new StorageException("SQLiteStorage uses the SQL path via TableRegistry");
    }

    @Override
    public void delete(String path) throws Exception {
        throw new StorageException("SQLiteStorage uses the SQL path via TableRegistry");
    }

    @Override
    public boolean exists(String path) throws Exception {
        throw new StorageException("SQLiteStorage uses the SQL path via TableRegistry");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void bindParams(PreparedStatement stmt, List<Object> params) throws Exception {
        if (params == null) return;
        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
    }

    private List<Map<String, Object>> mapResultSet(ResultSet rs) throws Exception {
        final ResultSetMetaData meta = rs.getMetaData();
        final int count = meta.getColumnCount();
        final List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            final Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= count; i++) {
                row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
