package com.gyvex.ezafk.integration;

public abstract class Integration {
    public boolean isSetup;
    public abstract void load();
    public abstract void unload();
}
