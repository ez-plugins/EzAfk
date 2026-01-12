package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.integration.Integration;

import java.util.HashMap;

public class IntegrationManager {
    private static HashMap<String, Integration> integrations = new HashMap<>();

    public static void addIntegration(String id, Integration integration) {
        integrations.put(id, integration);
    }

    public static Integration getIntegration(String id) {
        return integrations.get(id);
    }

    public static boolean hasIntegration(String id) {
        return integrations.containsKey(id) && integrations.get(id).isSetup;
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
