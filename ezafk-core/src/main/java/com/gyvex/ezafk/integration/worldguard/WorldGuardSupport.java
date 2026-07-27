package com.gyvex.ezafk.integration.worldguard;

import com.gyvex.ezafk.integration.worldguard.flag.AfkBypassFlag;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;

public final class WorldGuardSupport {
    private WorldGuardSupport() {}

    // Track whether we've already successfully registered flags to avoid duplicate logs
    private static final AtomicBoolean FLAGS_REGISTERED = new AtomicBoolean(false);

    /**
     * Register all EzAfk WorldGuard flags into the provided registry (when possible).
     * Returns true if at least one flag became available.
     * This method is static and safe to call from `onLoad` or `onEnable` or external code.
     */
    public static boolean registerFlagsIfPossible(Plugin plugin, Logger logger) {
        if (plugin == null || logger == null) return false;
        // If we've already registered flags successfully, skip doing it again.
        if (FLAGS_REGISTERED.get()) return true;

        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = worldGuardClass.getMethod("getInstance").invoke(null);
            Object registry = wgInstance.getClass().getMethod("getFlagRegistry").invoke(wgInstance);

            String mode = plugin.getConfig().getString("integration.flag-registration", "auto").trim().toLowerCase();

            // Try to adopt an existing flag if present
            try {
                Object existing = registry.getClass().getMethod("get", String.class).invoke(registry, "afk-bypass");
                if (existing != null) {
                    AfkBypassFlag.set(existing);
                    FLAGS_REGISTERED.set(true);
                    return true;
                }
            } catch (Throwable ignored) {
                // ignore and continue to attempt registration
            }

            Object active = AfkBypassFlag.ensureRegistered(registry, logger, mode);
            if (active != null) {
                FLAGS_REGISTERED.set(true);
                return true;
            }
            return false;
        } catch (Throwable ex) {
            logger.log(Level.FINE, "WorldGuard not available or registration failed: " + ex.getMessage());
            return false;
        }
    }
}
