package com.gyvex.ezafk.repository.yaml;

import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.repository.StorageRepository;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YamlStorage implements StorageRepository {
    private File dataFile;
    private FileConfiguration config;
    private final Map<UUID, Long> cache = new HashMap<>();

    @Override
    public void init() throws Exception {
        File dataFolder = Registry.get().getPlugin().getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        dataFile = new File(dataFolder, "afk_times.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                throw new IOException("Unable to create afk_times.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
        loadIntoCache();
    }

    @Override
    public Map<UUID, Long> loadAll() {
        return new java.util.HashMap<>(cache);
    }

    private void loadIntoCache() {
        cache.clear();
        if (config == null) return;
        for (String key : config.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                long v = config.getLong(key, 0L);
                cache.put(id, v);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void savePlayerAfkTime(UUID player, long seconds) {
        if (player == null) return;
        cache.put(player, seconds);
        if (config != null) config.set(player.toString(), seconds);
    }

    @Override
    public void deletePlayer(UUID player) {
        if (player == null) return;
        cache.remove(player);
        if (config != null) {
            config.set(player.toString(), null);
            try {
                config.save(dataFile);
            } catch (IOException e) {
                Registry.get().getLogger().warning("Failed to delete player from YamlStorage: " + e.getMessage());
            }
        }
    }

    @Override
    public long getPlayerAfkTime(UUID player) {
        if (player == null) return 0L;
        Long v = cache.get(player);
        return v != null ? v : 0L;
    }

    @Override
    public void saveAll() throws Exception {
        if (config == null || dataFile == null) return;
        for (Map.Entry<UUID, Long> e : cache.entrySet()) {
            config.set(e.getKey().toString(), e.getValue());
        }
        try {
            config.save(dataFile);
        } catch (IOException ex) {
            throw new IOException("Failed to save afk_times.yml", ex);
        }
    }

    @Override
    public void shutdown() {
        try {
            saveAll();
        } catch (Exception e) {
            Registry.get().getLogger().warning("Failed to save YamlStorage on shutdown: " + e.getMessage());
        }
        cache.clear();
    }
}
