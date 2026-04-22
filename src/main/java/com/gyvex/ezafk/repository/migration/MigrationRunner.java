package com.gyvex.ezafk.repository.migration;

import com.github.ezframework.jaloquent.store.sql.JdbcStore;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies pending schema migrations to a {@link JdbcStore} in ascending version order.
 * Tracks applied migrations in a {@code schema_migrations} table so each version
 * runs exactly once.
 */
public final class MigrationRunner {

    private static final String CREATE_MIGRATIONS_TABLE =
            "CREATE TABLE IF NOT EXISTS schema_migrations " +
            "(version INTEGER PRIMARY KEY, description VARCHAR(255) NOT NULL, " +
            "applied_at VARCHAR(64) NOT NULL)";

    private final JdbcStore store;
    private final List<Migration> migrations;

    public MigrationRunner(JdbcStore store, Migration... migrations) {
        this.store = store;
        this.migrations = Arrays.stream(migrations)
                .sorted(Comparator.comparingInt(Migration::version))
                .toList();
    }

    /**
     * Ensures the migrations tracking table exists, then runs any pending
     * migrations in ascending version order.
     *
     * @throws Exception if any migration or tracking operation fails
     */
    public void run() throws Exception {
        store.executeUpdate(CREATE_MIGRATIONS_TABLE, List.of());
        final Set<Integer> applied = loadAppliedVersions();
        for (final Migration migration : migrations) {
            if (!applied.contains(migration.version())) {
                migration.up(store);
                store.executeUpdate(
                        "INSERT INTO schema_migrations (version, description, applied_at) VALUES (?, ?, ?)",
                        List.of(migration.version(), migration.description(), Instant.now().toString()));
            }
        }
    }

    private Set<Integer> loadAppliedVersions() throws Exception {
        final List<Map<String, Object>> rows = store.query(
                "SELECT version FROM schema_migrations", List.of());
        final Set<Integer> versions = new HashSet<>();
        for (final Map<String, Object> row : rows) {
            final Object v = row.get("version");
            if (v instanceof Number n) {
                versions.add(n.intValue());
            }
        }
        return versions;
    }
}
