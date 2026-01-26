package com.gyvex.ezafk.repository.mysql;

import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.repository.StorageRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

/**
 * Minimal MySQL storage skeleton. Reads connection info from mysql.yml and attempts
 * to maintain per-player AFK seconds. This implementation is lightweight and tolerant
 * to missing configuration (will log and fall back to no-op).
 */
public class MySQLStorage implements StorageRepository {
    private Connection connection;

    @Override
    public void init() throws Exception {
        try {
            org.bukkit.configuration.file.FileConfiguration cfg = Registry.get().getConfigManager().getMysqlConfig();
            String host = cfg.getString("host", "localhost");
            int port = cfg.getInt("port", 3306);
            String db = cfg.getString("database", "ezafk");
            String user = cfg.getString("username", "root");
            String pass = cfg.getString("password", "");
            String url = String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true&useSSL=false", host, port, db);
            connection = DriverManager.getConnection(url, user, pass);
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS afk_times (uuid VARCHAR(36) PRIMARY KEY, seconds BIGINT)");
            }
        } catch (Exception e) {
            Registry.get().getLogger().warning("MySQLStorage init failed: " + e.getMessage());
            throw e;
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
            Registry.get().getLogger().warning("MySQL loadAll failed: " + e.getMessage());
        }
        return map;
    }

    @Override
    public void savePlayerAfkTime(UUID player, long seconds) {
        if (player == null || connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO afk_times(uuid, seconds) VALUES(?, ?) ON DUPLICATE KEY UPDATE seconds = ?")) {
            ps.setString(1, player.toString());
            ps.setLong(2, seconds);
            ps.setLong(3, seconds);
            ps.executeUpdate();
        } catch (Exception e) {
            Registry.get().getLogger().warning("MySQL save failed: " + e.getMessage());
        }
    }

    @Override
    public long getPlayerAfkTime(UUID player) {
        if (player == null || connection == null) return 0L;
        try (PreparedStatement ps = connection.prepareStatement("SELECT seconds FROM afk_times WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            Registry.get().getLogger().warning("MySQL read failed: " + e.getMessage());
        }
        return 0L;
    }

    @Override
    public void deletePlayer(UUID player) {
        if (player == null || connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM afk_times WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            Registry.get().getLogger().warning("MySQL delete failed: " + e.getMessage());
        }
    }

    @Override
    public void saveAll() throws Exception {
        // Connections are immediate; nothing to flush by default
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception ignored) {}
    }
}
