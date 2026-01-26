package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.integration.spigot.UpdateChecker;

public class SpigotIntegration extends Integration {
    @Override
    public void load() {
        if (Registry.get().getPlugin().getConfig().getBoolean("integration.spigot.check-for-update")) {
            Registry.get().getLogger().info("Initiating update check...");

            new UpdateChecker(Registry.get().getPlugin(), 117430).getVersion(version -> {
                if (Registry.get().getPlugin().getDescription().getVersion().equals(version)) {
                    Registry.get().getLogger().info("You are running on the latest version.");
                } else {
                    Registry.get().getLogger().info("A new version (" + version + ") of EzAfk is available.");
                    Registry.get().getLogger().info("Please visit the following link to download the latest update:");
                    Registry.get().getLogger().info("https://www.spigotmc.org/resources/ezafk.117430/");
                }
            });
        }
    }

    @Override
    public void unload() {

    }
}
