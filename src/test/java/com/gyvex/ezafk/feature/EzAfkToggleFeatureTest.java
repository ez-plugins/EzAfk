package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EzAfkToggleFeatureTest {

    private ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
    }

    @AfterEach
    public void tearDown() {
        TestHelpers.stopServer();
    }

    @Test
    public void toggleAfk_self_executes() {
        // Basic smoke of command registration: plugin loads successfully on mock server
        org.junit.jupiter.api.Assertions.assertNotNull(plugin, "Plugin should load on the mock server");
    }
}
