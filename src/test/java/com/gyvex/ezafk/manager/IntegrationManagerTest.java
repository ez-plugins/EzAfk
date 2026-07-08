package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.integration.Integration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationManagerTest {

    private static class CountingIntegration extends Integration {
        int loadCalls;
        int unloadCalls;

        @Override
        public void load() {
            loadCalls++;
            isSetup = true;
        }

        @Override
        public void unload() {
            unloadCalls++;
            isSetup = false;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        clearManagerState();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearManagerState();
    }

    @Test
    void add_and_get_integration_round_trip() {
        CountingIntegration integration = new CountingIntegration();

        IntegrationManager.addIntegration("test", integration);

        assertSame(integration, IntegrationManager.getIntegration("test"));
    }

    @Test
    void hasIntegration_returns_true_only_when_setup_and_not_blocked() {
        CountingIntegration integration = new CountingIntegration();
        IntegrationManager.addIntegration("economy", integration);

        integration.isSetup = false;
        assertFalse(IntegrationManager.hasIntegration("economy"));

        integration.isSetup = true;
        assertTrue(IntegrationManager.hasIntegration("economy"));

        IntegrationManager.setRegistrationAllowed("economy", false);
        assertFalse(IntegrationManager.hasIntegration("economy"));

        IntegrationManager.clearRegistrationOverride("economy");
        assertTrue(IntegrationManager.hasIntegration("economy"));
    }

    @Test
    void hasIntegration_returns_false_for_unknown_id() {
        assertFalse(IntegrationManager.hasIntegration("missing"));
    }

    @Test
    void load_and_unload_call_each_registered_integration() {
        CountingIntegration one = new CountingIntegration();
        CountingIntegration two = new CountingIntegration();

        IntegrationManager.addIntegration("one", one);
        IntegrationManager.addIntegration("two", two);

        IntegrationManager.load();
        assertEquals(1, one.loadCalls);
        assertEquals(1, two.loadCalls);
        assertTrue(one.isSetup);
        assertTrue(two.isSetup);

        IntegrationManager.unload();
        assertEquals(1, one.unloadCalls);
        assertEquals(1, two.unloadCalls);
        assertFalse(one.isSetup);
        assertFalse(two.isSetup);
    }

    @SuppressWarnings("unchecked")
    private static void clearManagerState() throws Exception {
        Field integrationsField = IntegrationManager.class.getDeclaredField("integrations");
        integrationsField.setAccessible(true);
        ((Map<String, Integration>) integrationsField.get(null)).clear();

        Field overridesField = IntegrationManager.class.getDeclaredField("registrationAllowedOverrides");
        overridesField.setAccessible(true);
        ((Map<String, Boolean>) overridesField.get(null)).clear();
    }
}
