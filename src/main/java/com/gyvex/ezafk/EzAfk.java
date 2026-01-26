package com.gyvex.ezafk;

import org.bukkit.plugin.java.JavaPlugin;

import com.gyvex.ezafk.bootstrap.Registry;

public class EzAfk extends JavaPlugin {
    @Override
    public void onEnable() {
        Registry.init(this);
        Registry.get().getBootstrap().onLoad();
        Registry.get().getBootstrap().onEnable();
    }

    @Override
    public void onDisable() {
        try {
            if (Registry.get().getBootstrap() != null) Registry.get().getBootstrap().onDisable();
        } catch (IllegalStateException ignored) {
            // Registry not initialized
        }
    }
}
