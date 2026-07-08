package com.gyvex.ezafk.repository.mysql;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.bootstrap.Registry;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySQLStorageTestcontainersTest {

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
    void mysql_storage_init_and_crud_with_testcontainers() throws Exception {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Testcontainers MySQL test"
        );

        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0")
                .withDatabaseName("ezafk_test")
                .withUsername("ezafk")
                .withPassword("ezafk")
                .withStartupTimeout(Duration.ofMinutes(2))) {
            mysql.start();

            Registry.get().getConfigManager().getMysqlConfig().set("host", mysql.getHost());
            Registry.get().getConfigManager().getMysqlConfig().set("port", mysql.getFirstMappedPort());
            Registry.get().getConfigManager().getMysqlConfig().set("database", mysql.getDatabaseName());
            Registry.get().getConfigManager().getMysqlConfig().set("username", mysql.getUsername());
            Registry.get().getConfigManager().getMysqlConfig().set("password", mysql.getPassword());

            MySQLStorage storage = new MySQLStorage();
            storage.init();

            UUID playerId = UUID.randomUUID();
            storage.savePlayerAfkTime(playerId, 321L);
            assertEquals(321L, storage.getPlayerAfkTime(playerId));

            Map<UUID, Long> loaded = storage.loadAll();
            assertTrue(loaded.containsKey(playerId));
            assertEquals(321L, loaded.get(playerId));

            storage.deletePlayer(playerId);
            assertEquals(0L, storage.getPlayerAfkTime(playerId));

            storage.saveAll();
            storage.shutdown();
        }
    }
}
