package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.integration.Integration;

import java.util.HashMap;
import java.util.Map;

public class IntegrationManager {
    private static HashMap<String, Integration> integrations = new HashMap<>();
    // Test / runtime overrides for registration behavior. If an entry exists
    // the value controls whether registration is allowed for that integration id.
    private static Map<String, Boolean> registrationAllowedOverrides = new HashMap<>();

    public static void addIntegration(String id, Integration integration) {
        integrations.put(id, integration);
    }

    public static Integration getIntegration(String id) {
        return integrations.get(id);
    }

    public static boolean hasIntegration(String id) {
        Boolean override = registrationAllowedOverrides.get(id);
        if (override != null && !override) {
            return false;
        }
        return integrations.containsKey(id) && integrations.get(id).isSetup;
    }

    /**
     * Test/runtime hook to control whether registration of a specific integration
     * id is allowed. Pass `true` to allow (default), `false` to block registration.
     * This is useful for tests that need to simulate absence of optional integrations.
     */
    public static void setRegistrationAllowed(String id, boolean allowed) {
        registrationAllowedOverrides.put(id, allowed);
    }

    public static void clearRegistrationOverride(String id) {
        registrationAllowedOverrides.remove(id);
    }

    public static boolean isRegistrationAllowed(String id) {
        Boolean override = registrationAllowedOverrides.get(id);
        return override == null ? true : override;
    }

    public static void load() {
        for (Integration integration : integrations.values()) {
            integration.load();
        }
    }

    public static void unload() {
        for (Integration integration : integrations.values()) {
            integration.unload();
        }
    }
}
