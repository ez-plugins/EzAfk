package com.gyvex.ezafk;

import org.bukkit.plugin.java.JavaPlugin;

import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.integration.worldguard.WorldGuardSupport;

public class EzAfk extends JavaPlugin {
    @Override
    public void onLoad() {
        // Only attempt early WorldGuard flag registration here. Keep heavy
        // Registry initialization for onEnable so startup is safer.
        try {
            WorldGuardSupport.registerFlagsIfPossible(this, this.getLogger());
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onEnable() {
        // Initialize Registry and run both load-phase and enable-phase bootstrap now.
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
