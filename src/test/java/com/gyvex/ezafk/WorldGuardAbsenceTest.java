package com.gyvex.ezafk;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.gyvex.ezafk.manager.IntegrationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldGuardAbsenceTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        // Force integration registration off so the test environment simulates
        // WorldGuard absence even if MockBukkit provides WG classes.
        com.gyvex.ezafk.manager.IntegrationManager.setRegistrationAllowed("worldguard", false);
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void loadsWithoutWorldGuard() {
        // Do not register a WorldGuard plugin on the mock server.
        EzAfk plugin = MockBukkit.load(EzAfk.class);
        assertNotNull(plugin);

        // Integration manager should not report WorldGuard as available.
        assertFalse(IntegrationManager.hasIntegration("worldguard"), "WorldGuard integration should not be active when WorldGuard is absent");
    }
}
