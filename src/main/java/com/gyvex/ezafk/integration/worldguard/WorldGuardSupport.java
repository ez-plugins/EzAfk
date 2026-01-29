package com.gyvex.ezafk.integration.worldguard;

import com.gyvex.ezafk.integration.worldguard.flag.AfkBypassFlag;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

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
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            String mode = plugin.getConfig().getString("integration.flag-registration", "auto").trim().toLowerCase();

            // If the registry already contains the flag, adopt it without extra logging.
            com.sk89q.worldguard.protection.flags.Flag<?> existing = registry.get("afk-bypass");
            if (existing instanceof StateFlag) {
                AfkBypassFlag.set((StateFlag) existing);
                FLAGS_REGISTERED.set(true);
                return true;
            }

            StateFlag active = AfkBypassFlag.ensureRegistered(registry, logger, mode);
            if (active != null) {
                FLAGS_REGISTERED.set(true);
                return true;
            }
            return false;
        } catch (NoClassDefFoundError | Exception ex) {
            logger.log(Level.FINE, "WorldGuard not available or registration failed: " + ex.getMessage());
            return false;
        }
    }
}
