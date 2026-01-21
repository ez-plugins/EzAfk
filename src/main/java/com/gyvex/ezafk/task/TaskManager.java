package com.gyvex.ezafk.task;

import com.gyvex.ezafk.task.AfkCheckTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class TaskManager {
    private BukkitTask afkCheckTask;

    public void startAfkCheckTask(JavaPlugin plugin) {
        afkCheckTask = new AfkCheckTask().runTaskTimer(plugin, 20, 20);
    }

    public void cancelTasks() {
        if (afkCheckTask != null) {
            afkCheckTask.cancel();
            afkCheckTask = null;
        }
    }
}