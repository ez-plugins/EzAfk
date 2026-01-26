package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyIntegration extends Integration {
    private Economy economy;

    public Economy getEconomy() {
        return economy;
    }

    @Override
    public void load() {
        this.isSetup = false;

        EzAfk plugin = Registry.get().getPlugin();

        clearEconomy(plugin, "Economy integration cleared before Vault detection.");

        if (plugin == null || plugin.getConfig() == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean("economy.enabled")) {
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            clearEconomy(plugin, "Economy integration cleared because Vault is not enabled.");
            plugin.getLogger().log(Level.WARNING, "Economy support requires Vault, but it was not found. Disabling economy features.");
            return;
        }

        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            clearEconomy(plugin, "Economy integration cleared because no Vault economy provider was detected.");
            plugin.getLogger().log(Level.WARNING, "Vault is installed but no economy provider was detected. Disabling economy features.");
            return;
        }

        Economy detectedEconomy = provider.getProvider();

        if (detectedEconomy == null) {
            clearEconomy(plugin, "Economy integration cleared because the Vault provider returned no economy implementation.");
            plugin.getLogger().log(Level.WARNING, "Failed to access the Vault economy provider. Disabling economy features.");
            return;
        }

        this.economy = detectedEconomy;
        this.isSetup = true;
        String providerName = provider.getProvider().getName();
        plugin.getLogger().log(Level.INFO, "Economy integration enabled via Vault. Found provider: " + providerName);
    }

    @Override
    public void unload() {
        EzAfk plugin = Registry.get().getPlugin();
        clearEconomy(plugin, "Economy integration cleared during unload.");
        this.isSetup = false;
    }

    private void clearEconomy(EzAfk plugin, String reason) {
        this.economy = null;

        Logger logger = plugin != null ? plugin.getLogger() : Bukkit.getLogger();

        if (logger != null && reason != null && !reason.isBlank()) {
            logger.log(Level.INFO, reason);
        }
    }
}
