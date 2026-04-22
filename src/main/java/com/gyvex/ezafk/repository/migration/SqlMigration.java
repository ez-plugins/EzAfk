package com.gyvex.ezafk.repository.migration;

import com.github.ezframework.jaloquent.store.sql.JdbcStore;

import java.util.List;

/**
 * A migration that executes a single SQL statement.
 */
public record SqlMigration(int version, String description, String sql) implements Migration {
    @Override
    public void up(JdbcStore store) throws Exception {
        store.executeUpdate(sql, List.of());
    }
}
