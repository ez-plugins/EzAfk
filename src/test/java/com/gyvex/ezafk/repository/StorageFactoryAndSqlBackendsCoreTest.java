package com.gyvex.ezafk.repository;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.repository.mysql.MySQLStorage;
import com.gyvex.ezafk.repository.sqlite.SQLiteStorage;
import com.gyvex.ezafk.repository.yaml.YamlStorage;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageFactoryAndSqlBackendsCoreTest {

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
    void storage_factory_returns_expected_backends_by_config_type() {
        plugin.getConfig().set("storage.type", "yaml");
        assertInstanceOf(YamlStorage.class, StorageFactory.create());

        plugin.getConfig().set("storage.type", "sqlite");
        assertInstanceOf(SQLiteStorage.class, StorageFactory.create());

        plugin.getConfig().set("storage.type", "mysql");
        assertInstanceOf(MySQLStorage.class, StorageFactory.create());

        plugin.getConfig().set("storage.type", "unsupported-type");
        assertInstanceOf(YamlStorage.class, StorageFactory.create());

        plugin.getConfig().set("storage.type", null);
        assertInstanceOf(YamlStorage.class, StorageFactory.create());
    }

    @Test
    void sql_backends_handle_core_guard_paths_without_init() {
        SQLiteStorage sqlite = new SQLiteStorage();
        MySQLStorage mysql = new MySQLStorage();

        UUID id = UUID.randomUUID();

        Map<UUID, Long> sqliteLoaded = sqlite.loadAll();
        assertTrue(sqliteLoaded.isEmpty());
        assertDoesNotThrow(() -> sqlite.savePlayerAfkTime(null, 1L));
        assertDoesNotThrow(() -> sqlite.deletePlayer(null));
        assertEquals(0L, sqlite.getPlayerAfkTime(null));
        assertDoesNotThrow(sqlite::saveAll);
        assertDoesNotThrow(sqlite::shutdown);

        Map<UUID, Long> mysqlLoaded = mysql.loadAll();
        assertTrue(mysqlLoaded.isEmpty());
        assertDoesNotThrow(() -> mysql.savePlayerAfkTime(null, 1L));
        assertDoesNotThrow(() -> mysql.savePlayerAfkTime(id, 1L));
        assertDoesNotThrow(() -> mysql.deletePlayer(null));
        assertEquals(0L, mysql.getPlayerAfkTime(null));
        assertEquals(0L, mysql.getPlayerAfkTime(id));
        assertDoesNotThrow(mysql::saveAll);
        assertDoesNotThrow(mysql::shutdown);
    }

    @Test
    void sqlite_storage_init_failure_path_is_handled() {
        SQLiteStorage sqlite = new SQLiteStorage();
        assertThrows(Exception.class, sqlite::init);

        UUID playerId = UUID.randomUUID();
        assertTrue(sqlite.loadAll().isEmpty());
        sqlite.savePlayerAfkTime(playerId, 123L);
        sqlite.deletePlayer(playerId);
        assertEquals(0L, sqlite.getPlayerAfkTime(playerId));

        sqlite.saveAll();
        sqlite.shutdown();
    }

    @Test
    void mysql_storage_init_failure_path_is_handled() {
        // Force a fast connection failure to cover init error handling path.
        Registry.get().getConfigManager().getMysqlConfig().set("host", "127.0.0.1");
        Registry.get().getConfigManager().getMysqlConfig().set("port", 1);
        Registry.get().getConfigManager().getMysqlConfig().set("database", "ezafk_test");
        Registry.get().getConfigManager().getMysqlConfig().set("username", "nouser");
        Registry.get().getConfigManager().getMysqlConfig().set("password", "nopass");

        MySQLStorage mysql = new MySQLStorage();
        assertThrows(Exception.class, mysql::init);

        assertTrue(mysql.loadAll().isEmpty());
        assertFalse(mysql.loadAll().containsKey(UUID.randomUUID()));
    }
}
