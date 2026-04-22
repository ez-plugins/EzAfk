package com.gyvex.ezafk.repository.migration;

import com.github.ezframework.jaloquent.store.sql.JdbcStore;

/**
 * A single versioned schema change that can be applied to a {@link JdbcStore}.
 */
public interface Migration {
    int version();
    String description();
    void up(JdbcStore store) throws Exception;
}
