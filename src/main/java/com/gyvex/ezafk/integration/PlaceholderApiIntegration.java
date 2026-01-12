package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.integration.placeholder.EzAfkPlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class PlaceholderApiIntegration extends Integration {

    private EzAfkPlaceholderExpansion expansion;

    @Override
    public void load() {
        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");

        if (placeholderApi == null || !placeholderApi.isEnabled()) {
            EzAfk.getInstance().getLogger().log(Level.FINE, "PlaceholderAPI not detected. Skipping expansion registration.");
            return;
        }

        expansion = new EzAfkPlaceholderExpansion();

        if (expansion.register()) {
            isSetup = true;
            EzAfk.getInstance().getLogger().log(Level.INFO, "Registered EzAfk placeholders with PlaceholderAPI.");
        } else {
            EzAfk.getInstance().getLogger().log(Level.WARNING, "Failed to register EzAfk placeholders with PlaceholderAPI.");
        }
    }

    @Override
    public void unload() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }

        isSetup = false;
    }
}
