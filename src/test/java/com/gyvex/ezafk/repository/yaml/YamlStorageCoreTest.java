package com.gyvex.ezafk.repository.yaml;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.bootstrap.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlStorageCoreTest {

    private org.mockbukkit.mockbukkit.ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
    }

    @AfterEach
    void tearDown() {
        TestHelpers.stopServer();
    }

    @Test
    void yaml_storage_crud_loadAll_and_persistence_paths() throws Exception {
        YamlStorage storage = new YamlStorage();
        storage.init();

        UUID playerId = UUID.randomUUID();

        storage.savePlayerAfkTime(playerId, 99L);
        assertEquals(99L, storage.getPlayerAfkTime(playerId));

        Map<UUID, Long> loaded = storage.loadAll();
        assertTrue(loaded.containsKey(playerId));
        assertEquals(99L, loaded.get(playerId));

        storage.deletePlayer(playerId);
        assertEquals(0L, storage.getPlayerAfkTime(playerId));

        // Null guards should be no-op paths.
        assertDoesNotThrow(() -> storage.savePlayerAfkTime(null, 10L));
        assertDoesNotThrow(() -> storage.deletePlayer(null));
        assertEquals(0L, storage.getPlayerAfkTime(null));

        // saveAll and shutdown both flush datastore.
        UUID persistentId = UUID.randomUUID();
        storage.savePlayerAfkTime(persistentId, 15L);
        storage.saveAll();
        storage.shutdown();

        File file = new File(Registry.get().getPlugin().getDataFolder(), "afk_times.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        Object persistedSeconds = cfg.get("afk_times." + persistentId + ".seconds");
        assertEquals(15L, ((Number) persistedSeconds).longValue());
    }

    @Test
    void yaml_storage_loadAll_ignores_invalid_uuid_keys() throws Exception {
        File file = new File(Registry.get().getPlugin().getDataFolder(), "afk_times.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("afk_times.not-a-uuid.seconds", 77L);
        cfg.save(file);

        YamlStorage storage = new YamlStorage();
        storage.init();

        Map<UUID, Long> loaded = storage.loadAll();
        assertFalse(loaded.containsValue(77L));
    }
}
