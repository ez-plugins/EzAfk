package com.gyvex.ezafk.task;

import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.compatibility.scheduler.TaskHandle;
import org.bukkit.plugin.java.JavaPlugin;

public class TaskManager {
    private TaskHandle afkCheckTask;

    public void startAfkCheckTask(JavaPlugin plugin) {
        afkCheckTask = Registry.get().getScheduler().runTaskTimer(new AfkCheckTask(), 20, 20);
    }

    public void cancelTasks() {
        if (afkCheckTask != null) {
            afkCheckTask.cancel();
            afkCheckTask = null;
        }
    }
}