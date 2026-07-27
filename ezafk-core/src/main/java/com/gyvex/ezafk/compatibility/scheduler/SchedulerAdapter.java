package com.gyvex.ezafk.compatibility.scheduler;

/**
 * Platform-agnostic scheduling facade used by EzAfk so that task scheduling
 * works on both Bukkit/Paper and Folia without scattering platform-specific
 * branching throughout the codebase.
 */
public interface SchedulerAdapter {

    /** Schedules a synchronous repeating task. */
    TaskHandle runTaskTimer(Runnable task, long delayTicks, long periodTicks);

    /** Schedules an asynchronous repeating task. */
    TaskHandle runTaskTimerAsync(Runnable task, long delayTicks, long periodTicks);

    /** Runs a one-shot task asynchronously (fire-and-forget). */
    void runTaskAsync(Runnable task);

    /** Runs a one-shot task synchronously after {@code delayTicks} ticks. */
    void runTaskLater(Runnable task, long delayTicks);
}
