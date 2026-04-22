package com.gyvex.ezafk.repository.yaml;

import com.github.ezframework.jaloquent.model.Model;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.repository.AfkTimeModel;
import com.gyvex.ezafk.repository.StorageRepository;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YamlStorage implements StorageRepository {

    private YamlDataStore store;
    private ModelRepository<AfkTimeModel> repo;

    @Override
    public void init() throws Exception {
        File dataFolder = Registry.get().getPlugin().getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        store = new YamlDataStore(new File(dataFolder, "afk_times.yml"));
        repo = new ModelRepository<>(store, AfkTimeModel.TABLE_PREFIX, AfkTimeModel.FACTORY);
    }

    @Override
    public Map<UUID, Long> loadAll() {
        final Map<UUID, Long> result = new HashMap<>();
        try {
            for (final AfkTimeModel m : repo.query(Model.queryBuilder().build())) {
                try {
                    result.put(UUID.fromString(m.getId()), m.getSeconds());
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (Exception e) {
            Registry.get().getLogger().warning("Failed to load AFK times from YAML: " + e.getMessage());
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
            Registry.get().getLogger().warning("YAML save failed: " + e.getMessage());
        }
    }

    @Override
    public void deletePlayer(UUID player) {
        if (player == null) return;
        try {
            repo.delete(player.toString());
        } catch (Exception e) {
            Registry.get().getLogger().warning("YAML delete failed: " + e.getMessage());
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
            Registry.get().getLogger().warning("YAML read failed: " + e.getMessage());
            return 0L;
        }
    }

    @Override
    public void saveAll() throws Exception {
        try {
            store.flush();
        } catch (IOException e) {
            throw new IOException("Failed to save afk_times.yml", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            store.flush();
        } catch (Exception e) {
            Registry.get().getLogger().warning("Failed to flush YAML storage on shutdown: " + e.getMessage());
        }
    }
}
