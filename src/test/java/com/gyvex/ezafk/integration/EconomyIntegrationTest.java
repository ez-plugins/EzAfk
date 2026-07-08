package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import net.milkbowl.vault.economy.Economy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EconomyIntegrationTest {

    private ServerMock server;
    private EzAfk plugin;

    @BeforeEach
    void setUp() {
        IntegrationTestFixtures.clearIntegrationManagerState();
        server = TestHelpers.startServer();
        plugin = (EzAfk) TestHelpers.loadPlugin();
    }

    @AfterEach
    void tearDown() {
        TestHelpers.stopServer();
        IntegrationTestFixtures.clearIntegrationManagerState();
    }

    @Test
    void load_keeps_integration_disabled_when_economy_is_off_in_config() {
        plugin.getConfig().set("economy.enabled", false);

        EconomyIntegration integration = new EconomyIntegration();
        integration.load();

        assertFalse(integration.isSetup);
        assertNull(integration.getEconomy());
    }

    @Test
    void load_keeps_integration_disabled_when_vault_is_not_enabled() {
        plugin.getConfig().set("economy.enabled", true);
        assertFalse(server.getPluginManager().isPluginEnabled("Vault"));

        EconomyIntegration integration = new EconomyIntegration();
        integration.load();

        assertFalse(integration.isSetup);
        assertNull(integration.getEconomy());
    }

    @Test
    void unload_is_safe_after_failed_load() {
        plugin.getConfig().set("economy.enabled", true);

        EconomyIntegration integration = new EconomyIntegration();
        integration.load();

        assertDoesNotThrow(integration::unload);
        assertFalse(integration.isSetup);
        assertNull(integration.getEconomy());
    }

    @Test
    void load_enables_integration_when_vault_provider_is_registered() {
        plugin.getConfig().set("economy.enabled", true);
        Economy economyProvider = mock(Economy.class);
        when(economyProvider.getName()).thenReturn("MockEconomy");
        IntegrationTestFixtures.registerVaultProvider(economyProvider);

        EconomyIntegration integration = new EconomyIntegration();
        integration.load();

        assertTrue(integration.isSetup);
        assertSame(economyProvider, integration.getEconomy());

        integration.unload();
        assertFalse(integration.isSetup);
        assertNull(integration.getEconomy());
    }
}
