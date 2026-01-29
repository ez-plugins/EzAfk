package com.gyvex.ezafk.repository.sqlite;

import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.repository.StorageRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

/**
 * Minimal SQLite storage stub. Creates a simple table and stores per-player seconds.
 * This is intentionally small but functional for local file-based storage.
 */
public class SQLiteStorage implements StorageRepository {
    private Connection connection;

    @Override
    public void init() throws Exception {
        java.io.File dataFolder = Registry.get().getPlugin().getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        java.io.File dbFile = new java.io.File(dataFolder, "ezafk.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS afk_times (uuid TEXT PRIMARY KEY, seconds INTEGER)");
        }
    }

    @Override
    public java.util.Map<UUID, Long> loadAll() {
        java.util.Map<UUID, Long> map = new java.util.HashMap<>();
        if (connection == null) return map;
        try (PreparedStatement ps = connection.prepareStatement("SELECT uuid, seconds FROM afk_times")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID id = UUID.fromString(rs.getString(1));
                        long s = rs.getLong(2);
                        map.put(id, s);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Registry.get().getLogger().warning("SQLite loadAll failed: " + e.getMessage());
        }
        return map;
    }

    @Override
    public void savePlayerAfkTime(UUID player, long seconds) {
        if (player == null) return;
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO afk_times(uuid, seconds) VALUES(?, ?) ON CONFLICT(uuid) DO UPDATE SET seconds=excluded.seconds")) {
            ps.setString(1, player.toString());
            ps.setLong(2, seconds);
            ps.executeUpdate();
        } catch (Exception e) {
            Registry.get().getLogger().warning("SQLite save failed: " + e.getMessage());
        }
    }

    @Override
    public void deletePlayer(UUID player) {
        if (player == null || connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM afk_times WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            Registry.get().getLogger().warning("SQLite delete failed: " + e.getMessage());
        }
    }

    @Override
    public long getPlayerAfkTime(UUID player) {
        if (player == null) return 0L;
        try (PreparedStatement ps = connection.prepareStatement("SELECT seconds FROM afk_times WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            Registry.get().getLogger().warning("SQLite read failed: " + e.getMessage());
        }
        return 0L;
    }

    @Override
    public void saveAll() throws Exception {
        // no-op; writes are immediate
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception ignored) {}
    }
}
