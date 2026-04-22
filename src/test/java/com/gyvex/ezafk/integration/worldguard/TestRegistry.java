package com.gyvex.ezafk.integration.worldguard;

import com.sk89q.worldguard.protection.flags.StateFlag;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TestRegistry {
    private final Map<String, StateFlag> flags = new ConcurrentHashMap<>();
    private volatile boolean throwOnRegister = false;

    public void setThrowOnRegister(boolean v) {
        this.throwOnRegister = v;
    }

    public void register(StateFlag flag) {
        if (throwOnRegister) throw new IllegalStateException("registry locked");
        flags.put(flag.getName(), flag);
    }

    public Object get(String name) {
        return flags.get(name);
    }
}
