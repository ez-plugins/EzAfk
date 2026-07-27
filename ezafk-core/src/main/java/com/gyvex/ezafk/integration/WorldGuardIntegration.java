package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.gyvex.ezafk.integration.worldguard.flag.AfkBypassFlag;
import com.gyvex.ezafk.integration.worldguard.FlagRegistrar;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;

import java.util.logging.Level;

public class WorldGuardIntegration extends Integration {
    private boolean apiAvailable = false;
    private Listener pluginEnableListener = null;

    // Region checks are handled directly by callers using AfkBypassFlag.get().

    public void setupTags() {
        EzAfk plugin = Registry.get().getPlugin();

        if (!plugin.getConfig().getBoolean("integration.worldguard")) {
            return;
        }

        plugin.getLogger().info("WorldGuard integration is enabled");

            try {
                boolean ok = com.gyvex.ezafk.integration.worldguard.WorldGuardSupport.registerFlagsIfPossible(plugin, plugin.getLogger());
                if (ok) {
                    plugin.getLogger().info("AFK BYPASS flag ready in WorldGuard");
                    this.isSetup = true;
                }
        } catch (NoClassDefFoundError ex) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "WorldGuard API not available. Skipping WorldGuard integration setup.",
                    ex
            );
            // If WorldGuard isn't present yet, listen for it being enabled so we can try again.
            tryRegisterOnEnableFallback();
        }
    }

    @Override
    public void load() {
        // Detect WorldGuard API presence at load time so callers can decide whether to proceed.
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
            Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            this.apiAvailable = true;
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            this.apiAvailable = false;
            this.isSetup = false;
            // If the WorldGuard API classes are not present at load time, we still
            // register a listener so that if WorldGuard is later enabled we can
            // attempt registration then.
            tryRegisterOnEnableFallback();
            return;
        }

        // If API classes are present, attempt immediate setup.
        if (this.apiAvailable) {
            setupTags();
        }
    }

    public boolean isApiAvailable() {
        return apiAvailable;
    }

    @Override
    public void unload() {
        // Unregister any listener we created.
        if (this.pluginEnableListener != null) {
            try {
                HandlerList.unregisterAll(this.pluginEnableListener);
            } catch (Throwable ignored) {
            }
            this.pluginEnableListener = null;
        }
    }

    private void tryRegisterOnEnableFallback() {
        // Avoid creating multiple listeners.
        if (this.pluginEnableListener != null) return;

        EzAfk plugin = Registry.get().getPlugin();

        this.pluginEnableListener = new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                if (event.getPlugin().getName().equalsIgnoreCase("WorldGuard")) {
                    // Attempt to setup now that WorldGuard is present.
                    setupTags();
                    // Once attempted, unregister this listener.
                    try {
                        HandlerList.unregisterAll(this);
                    } catch (Throwable ignored) {
                    }
                }
            }
        };

        try {
            plugin.getServer().getPluginManager().registerEvents(this.pluginEnableListener, plugin);
        } catch (Throwable ex) {
            plugin.getLogger().warning("Failed to register plugin-enable listener for WorldGuard fallback: " + ex.getMessage());
        }
    }
}
