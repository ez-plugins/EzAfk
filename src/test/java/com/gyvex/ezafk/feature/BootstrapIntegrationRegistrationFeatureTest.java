package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.integration.Integration;
import com.gyvex.ezafk.integration.VoiceChatIntegration;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.integration.IntegrationTestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

class BootstrapIntegrationRegistrationFeatureTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        IntegrationTestFixtures.clearIntegrationManagerState();
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
        IntegrationTestFixtures.clearIntegrationManagerState();
    }

    @Test
    void bootstrap_registers_core_integrations() {
        server = MockBukkit.mock();
        MockBukkit.load(EzAfk.class);

        assertNotNull(IntegrationManager.getIntegration("metrics"));
        assertNotNull(IntegrationManager.getIntegration("tab"));
        assertNotNull(IntegrationManager.getIntegration("spigot"));
        assertNotNull(IntegrationManager.getIntegration("economy"));
        assertNotNull(IntegrationManager.getIntegration("placeholderapi"));
    }

    @Test
    void voicechat_auto_does_not_register_when_plugin_is_absent() {
        server = MockBukkit.mock();
        assertNull(server.getPluginManager().getPlugin("voicechat"));

        MockBukkit.load(EzAfk.class);

        assertNull(IntegrationManager.getIntegration("voicechat"));
    }

    @Test
    void voicechat_auto_registers_when_plugin_is_present() {
        server = MockBukkit.mock();
        MockBukkit.createMockPlugin("voicechat");

        MockBukkit.load(EzAfk.class);

        Integration integration = IntegrationManager.getIntegration("voicechat");
        assertNotNull(integration);
        assertInstanceOf(VoiceChatIntegration.class, integration);
    }

    @Test
    void worldguard_registration_override_blocks_registration() {
        IntegrationManager.setRegistrationAllowed("worldguard", false);

        server = MockBukkit.mock();
        MockBukkit.load(EzAfk.class);

        assertNull(IntegrationManager.getIntegration("worldguard"));
    }
}
