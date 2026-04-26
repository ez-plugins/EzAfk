package com.gyvex.ezafk.repository.sqlite;

import com.github.ezframework.jaloquent.config.DatabaseSettings;
import com.github.ezframework.jaloquent.config.JaloquentConfig;
import com.github.ezframework.jaloquent.migration.Migration;
import com.github.ezframework.jaloquent.migration.MigrationRunner;
import com.github.ezframework.jaloquent.migration.Schema;
import com.github.ezframework.jaloquent.model.Model;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.sql.SqlDialect;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.repository.AfkTimeModel;
import com.gyvex.ezafk.repository.StorageRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SQLite storage backend using Jaloquent's ModelRepository with a
 * DataSourceJdbcStore backed by a DriverManager JDBC connection.
 */
public class SQLiteStorage implements StorageRepository {

    private static final Map<String, String> TABLE_COLUMNS = Map.of(
            "id",      "TEXT PRIMARY KEY",
            "seconds", "INTEGER NOT NULL DEFAULT 0"
    );

    private DataSourceJdbcStore store;
    private ModelRepository<AfkTimeModel> repo;

    @Override
    public void init() throws Exception {
        java.io.File dataFolder = Registry.get().getPlugin().getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        java.io.File dbFile = new java.io.File(dataFolder, "ezafk.db");

        DatabaseSettings settings = DatabaseSettings.builder()
                .url("jdbc:sqlite:" + dbFile.getAbsolutePath())
                .build();
        JaloquentConfig.setDatabaseSettings(settings);
        store = JaloquentConfig.buildStore();

        new MigrationRunner(store, SqlDialect.SQLITE, List.of(new CreateAfkTimesTable()))
                .run();

        TableRegistry.register(AfkTimeModel.TABLE_PREFIX, "afk_times", TABLE_COLUMNS);
        repo = new ModelRepository<>(store, AfkTimeModel.TABLE_PREFIX, AfkTimeModel.FACTORY,
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
        if (player == null || store == null) return;
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
        // DataSourceJdbcStore closes connections on its own; no explicit shutdown needed.
    }

    // =========================================================================
    // Inner migration class
    // =========================================================================

    private static final class CreateAfkTimesTable implements Migration {
        @Override
        public String getId() { return "2026_04_23_001_create_afk_times"; }

        @Override
        public void up(Schema schema) throws com.github.ezframework.jaloquent.exception.MigrationException {
            schema.create("afk_times", t -> t
                    .column("id", "TEXT NOT NULL")
                    .primaryKey("id")
                    .column("seconds", "INTEGER NOT NULL DEFAULT 0")
                    .ifNotExists()
            );
        }

        @Override
        public void down(Schema schema) throws com.github.ezframework.jaloquent.exception.MigrationException {
            schema.dropIfExists("afk_times");
        }
    }
}
