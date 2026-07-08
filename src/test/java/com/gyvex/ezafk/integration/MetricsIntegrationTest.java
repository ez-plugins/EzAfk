package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsIntegrationTest {

    @BeforeEach
    void setUp() {
        IntegrationTestFixtures.clearIntegrationManagerState();
        TestHelpers.startServer();
        TestHelpers.loadPlugin(EzAfk.class);
    }

    @AfterEach
    void tearDown() {
        TestHelpers.stopServer();
        IntegrationTestFixtures.clearIntegrationManagerState();
    }

    @Test
    void load_is_guarded_in_test_environment() {
        MetricsIntegration integration = new MetricsIntegration();

        assertDoesNotThrow(integration::load);

        // In this CI/test environment bStats setup is expected to be unavailable.
        assertFalse(integration.isSetup);
        assertNull(integration.getMetrics());
    }

    @Test
    void unload_is_safe_after_guarded_load() {
        MetricsIntegration integration = new MetricsIntegration();
        integration.load();

        assertDoesNotThrow(integration::unload);
    }
}
