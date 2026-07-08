package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.manager.IntegrationManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Field;
import java.util.Map;

public final class IntegrationTestFixtures {

    private IntegrationTestFixtures() {
    }

    public static Plugin registerVaultProvider(Economy economyProvider) {
        Plugin vault = MockBukkit.createMockPlugin("Vault");
        Bukkit.getServicesManager().register(Economy.class, economyProvider, vault, ServicePriority.Normal);
        return vault;
    }

    @SuppressWarnings("unchecked")
    public static void clearIntegrationManagerState() {
        try {
            Field integrationsField = IntegrationManager.class.getDeclaredField("integrations");
            integrationsField.setAccessible(true);
            ((Map<String, com.gyvex.ezafk.integration.Integration>) integrationsField.get(null)).clear();

            Field overridesField = IntegrationManager.class.getDeclaredField("registrationAllowedOverrides");
            overridesField.setAccessible(true);
            ((Map<String, Boolean>) overridesField.get(null)).clear();
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to reset IntegrationManager test state", ex);
        }
    }
}
