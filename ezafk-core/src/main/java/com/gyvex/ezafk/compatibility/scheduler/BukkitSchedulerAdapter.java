package com.gyvex.ezafk.compatibility.scheduler;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/** {@link SchedulerAdapter} backed by the Bukkit scheduler (Spigot / Paper). */
public final class BukkitSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TaskHandle runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask t = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, task, delayTicks, periodTicks);
        return wrap(t);
    }

    @Override
    public TaskHandle runTaskTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        // Use BukkitRunnable so that MockBukkit (and Bukkit's own scheduler) can
        // track the task by identity and honour cancel() correctly for async timers.
        BukkitRunnable br = new BukkitRunnable() {
            @Override public void run() { task.run(); }
        };
        BukkitTask t = br.runTaskTimerAsynchronously(plugin, delayTicks, periodTicks);
        return wrap(t);
    }

    @Override
    public void runTaskAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    private static TaskHandle wrap(BukkitTask task) {
        return new TaskHandle() {
            @Override public void cancel()         { task.cancel(); }
            @Override public boolean isCancelled() { return task.isCancelled(); }
        };
    }
}
