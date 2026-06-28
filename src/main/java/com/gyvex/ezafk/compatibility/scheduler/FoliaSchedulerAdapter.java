package com.gyvex.ezafk.compatibility.scheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * {@link SchedulerAdapter} backed by Folia's {@link GlobalRegionScheduler} and
 * {@link AsyncScheduler}.
 *
 * <p>This class directly imports Folia API types — it must <em>never</em> be
 * instantiated on servers that do not provide those types (Spigot, pre-1.19.4
 * Paper). {@link SchedulerAdapterFactory} guards instantiation so that the JVM
 * only class-loads this type when Folia is actually detected at runtime; if the
 * Folia classes are unexpectedly absent the resulting {@link NoClassDefFoundError}
 * is caught and a {@link BukkitSchedulerAdapter} is used as a fallback.
 */
public final class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;
    private final GlobalRegionScheduler global;
    private final AsyncScheduler async;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.global = plugin.getServer().getGlobalRegionScheduler();
        this.async  = plugin.getServer().getAsyncScheduler();
    }

    @Override
    public TaskHandle runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        ScheduledTask t = global.runAtFixedRate(plugin, st -> task.run(),
                Math.max(1L, delayTicks), periodTicks);
        return wrap(t);
    }

    @Override
    public TaskHandle runTaskTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        long delayMs  = Math.max(50L, delayTicks  * 50L);
        long periodMs = Math.max(50L, periodTicks * 50L);
        ScheduledTask t = async.runAtFixedRate(plugin, st -> task.run(),
                delayMs, periodMs, TimeUnit.MILLISECONDS);
        return wrap(t);
    }

    @Override
    public void runTaskAsync(Runnable task) {
        async.runNow(plugin, st -> task.run());
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        if (delayTicks <= 0) {
            global.run(plugin, st -> task.run());
        } else {
            global.runDelayed(plugin, st -> task.run(), delayTicks);
        }
    }

    private static TaskHandle wrap(ScheduledTask task) {
        return new TaskHandle() {
            @Override public void cancel()         { task.cancel(); }
            @Override public boolean isCancelled() { return task.isCancelled(); }
        };
    }
}
