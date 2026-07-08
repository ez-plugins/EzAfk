package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

class WorldEditIntegrationTest {

    private ServerMock server;
    private EzAfk plugin;

    @BeforeEach
    void setUp() {
        server = TestHelpers.startServer();
        plugin = (EzAfk) TestHelpers.loadPlugin();
    }

    @AfterEach
    void tearDown() {
        TestHelpers.stopServer();
    }

    @Test
    void load_can_mark_setup_when_worldedit_classes_are_on_classpath() {
        assertNull(server.getPluginManager().getPlugin("WorldEdit"));

        WorldEditIntegration integration = new WorldEditIntegration();
        integration.load();

        assertTrue(integration.isSetup);
    }

    @Test
    void unload_resets_setup_state() {
        WorldEditIntegration integration = new WorldEditIntegration();
        integration.isSetup = true;

        integration.unload();

        assertFalse(integration.isSetup);
    }

    @Test
    void getSelectionLocations_returns_null_when_no_helper_can_resolve_selection() {
        Player player = server.addPlayer("NoSelectionPlayer");

        assertNull(WorldEditIntegration.getSelectionLocations(plugin, player));
    }
}
