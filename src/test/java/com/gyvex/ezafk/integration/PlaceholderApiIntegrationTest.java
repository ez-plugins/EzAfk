package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderApiIntegrationTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        IntegrationTestFixtures.clearIntegrationManagerState();
        server = TestHelpers.startServer();
        TestHelpers.loadPlugin(EzAfk.class);
    }

    @AfterEach
    void tearDown() {
        TestHelpers.stopServer();
        IntegrationTestFixtures.clearIntegrationManagerState();
    }

    @Test
    void load_skips_setup_when_placeholderapi_plugin_is_absent() {
        assertNull(server.getPluginManager().getPlugin("PlaceholderAPI"));

        PlaceholderApiIntegration integration = new PlaceholderApiIntegration();
        integration.load();

        assertFalse(integration.isSetup);
    }

    @Test
    void unload_is_safe_when_nothing_was_registered() {
        PlaceholderApiIntegration integration = new PlaceholderApiIntegration();

        integration.load();

        assertDoesNotThrow(integration::unload);
        assertFalse(integration.isSetup);
    }
}
