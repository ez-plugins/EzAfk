package com.gyvex.ezafk.compatibility.scheduler;

import com.gyvex.ezafk.compatibility.platform.PlatformDetector;
import org.bukkit.plugin.Plugin;

/**
 * Creates the best {@link SchedulerAdapter} for the current server platform.
 *
 * <ul>
 *   <li><b>Folia</b>: {@link FoliaSchedulerAdapter} (GlobalRegionScheduler +
 *       AsyncScheduler)</li>
 *   <li><b>Paper / Spigot</b>: {@link BukkitSchedulerAdapter}</li>
 * </ul>
 *
 * <p>If Folia is detected but its API classes are absent at runtime, a warning
 * is logged and the factory falls back to {@link BukkitSchedulerAdapter}.
 */
public final class SchedulerAdapterFactory {

    private SchedulerAdapterFactory() {}

    public static SchedulerAdapter create(Plugin plugin) {
        if (PlatformDetector.isFolia()) {
            try {
                return new FoliaSchedulerAdapter(plugin);
            } catch (NoClassDefFoundError e) {
                plugin.getLogger().warning("[EzAfk] Folia classes not found, falling back to Bukkit scheduler: " + e.getMessage());
            } catch (Throwable e) {
                plugin.getLogger().warning("[EzAfk] Folia detected but Folia scheduler API unavailable — falling back to Bukkit scheduler.");
            }
        }
        return new BukkitSchedulerAdapter(plugin);
    }
}
