package com.gyvex.ezafk.repository.mysql;

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * MySQL storage backend using Jaloquent's ModelRepository with a JdbcStore
 * backed by a DriverManager JDBC connection.
 */
public class MySQLStorage implements StorageRepository, DataStore, JdbcStore {

    private static final Map<String, String> TABLE_COLUMNS = Map.of(
            "id",      "VARCHAR(36) PRIMARY KEY",
            "seconds", "BIGINT NOT NULL DEFAULT 0"
    );

    private Connection connection;
    private ModelRepository<AfkTimeModel> repo;

    @Override
    public void init() throws Exception {
        try {
            org.bukkit.configuration.file.FileConfiguration cfg =
                    Registry.get().getConfigManager().getMysqlConfig();
            String host = cfg.getString("host", "localhost");
            int port = cfg.getInt("port", 3306);
            String db = cfg.getString("database", "ezafk");
            String user = cfg.getString("username", "root");
            String pass = cfg.getString("password", "");
            String url = String.format(
                    "jdbc:mysql://%s:%d/%s?autoReconnect=true&useSSL=false", host, port, db);
            connection = DriverManager.getConnection(url, user, pass);

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS afk_times " +
                        "(id VARCHAR(36) PRIMARY KEY, seconds BIGINT NOT NULL DEFAULT 0)");
            }

            TableRegistry.register(AfkTimeModel.TABLE_PREFIX, "afk_times", TABLE_COLUMNS);
            repo = new ModelRepository<>(this, AfkTimeModel.TABLE_PREFIX, AfkTimeModel.FACTORY,
                    SqlDialect.MYSQL);
        } catch (Exception e) {
            Registry.get().getLogger().warning("MySQLStorage init failed: " + e.getMessage());
            throw e;
        }
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
            Registry.get().getLogger().warning("MySQL loadAll failed: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void savePlayerAfkTime(UUID player, long seconds) {
        if (player == null || connection == null) return;
        final AfkTimeModel model = new AfkTimeModel(player.toString());
        model.setSeconds(seconds);
        try {
            repo.save(model);
        } catch (Exception e) {
            Registry.get().getLogger().warning("MySQL save failed: " + e.getMessage());
        }
    }

    @Override
    public long getPlayerAfkTime(UUID player) {
        if (player == null || connection == null) return 0L;
        try {
            return repo.find(player.toString())
                    .map(AfkTimeModel::getSeconds)
                    .orElse(0L);
        } catch (Exception e) {
            Registry.get().getLogger().warning("MySQL read failed: " + e.getMessage());
            return 0L;
        }
    }

    @Override
    public void deletePlayer(UUID player) {
        if (player == null || connection == null) return;
        try {
            repo.delete(player.toString());
        } catch (Exception e) {
            Registry.get().getLogger().warning("MySQL delete failed: " + e.getMessage());
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
        throw new StorageException("MySQLStorage uses the SQL path via TableRegistry");
    }

    @Override
    public Optional<Map<String, Object>> load(String path) throws Exception {
        throw new StorageException("MySQLStorage uses the SQL path via TableRegistry");
    }

    @Override
    public void delete(String path) throws Exception {
        throw new StorageException("MySQLStorage uses the SQL path via TableRegistry");
    }

    @Override
    public boolean exists(String path) throws Exception {
        throw new StorageException("MySQLStorage uses the SQL path via TableRegistry");
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
