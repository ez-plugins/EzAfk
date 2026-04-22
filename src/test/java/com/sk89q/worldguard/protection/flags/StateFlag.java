package com.sk89q.worldguard.protection.flags;

public class StateFlag {
    private final String name;
    private final boolean defaultValue;

    public StateFlag(String name, boolean defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public StateFlag(String name, Boolean defaultValue) {
        this(name, defaultValue == null ? false : defaultValue.booleanValue());
    }

    public String getName() {
        return this.name;
    }

    public boolean getDefault() {
        return this.defaultValue;
    }
}
