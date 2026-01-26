package com.gyvex.ezafk.listener;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.manager.IntegrationManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

public class EconomyServiceListener implements Listener {

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (!isEconomyService(event.getProvider())) {
            return;
        }

        com.gyvex.ezafk.integration.EconomyIntegration economyIntegration = getEconomyIntegration();

        if (economyIntegration == null) {
            return;
        }

        economyIntegration.load();

        if (economyIntegration.isSetup) {
            log("Economy provider registered; refreshed economy integration.");
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (!isEconomyService(event.getProvider())) {
            return;
        }

        com.gyvex.ezafk.integration.EconomyIntegration economyIntegration = getEconomyIntegration();

        if (economyIntegration == null) {
            return;
        }

        economyIntegration.unload();
        log("Economy provider unregistered; unloaded economy integration.");
    }

    private boolean isEconomyService(RegisteredServiceProvider<?> provider) {
        return provider != null && Economy.class.equals(provider.getService());
    }

    private com.gyvex.ezafk.integration.EconomyIntegration getEconomyIntegration() {
        com.gyvex.ezafk.integration.Integration integration = IntegrationManager.getIntegration("economy");

        if (integration instanceof com.gyvex.ezafk.integration.EconomyIntegration) {
            return (com.gyvex.ezafk.integration.EconomyIntegration) integration;
        }

        return null;
    }

    private void log(String message) {
        EzAfk plugin = Registry.get().getPlugin();

        if (plugin == null) {
            return;
        }

        Logger logger = plugin.getLogger();

        if (logger != null) {
            logger.info(message);
        }
    }
}
