package com.gyvex.ezafk.repository;

import java.util.Map;
import java.util.UUID;

/**
 * StorageRepository: abstraction for storing AFK-related persistent data.
 */
public interface StorageRepository {
    void init() throws Exception;
    Map<UUID, Long> loadAll();
    void savePlayerAfkTime(UUID player, long seconds);
    void deletePlayer(UUID player);
    long getPlayerAfkTime(UUID player);
    void saveAll() throws Exception;
    void shutdown();
}
