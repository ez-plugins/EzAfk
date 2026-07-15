package com.gyvex.ezafk.compatibility.scheduler;

/** A cancellable handle to a scheduled repeating or delayed task. */
public interface TaskHandle {
    void cancel();
    boolean isCancelled();
}
