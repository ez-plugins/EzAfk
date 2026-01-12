package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.integration.spigot.UpdateChecker;

public class SpigotIntegration extends Integration {
    @Override
    public void load() {
        if (EzAfk.getInstance().config.getBoolean("integration.spigot.check-for-update")) {
            EzAfk.getInstance().getLogger().info("Initiating update check...");

            new UpdateChecker(EzAfk.getInstance(), 117430).getVersion(version -> {
                if (EzAfk.getInstance().getDescription().getVersion().equals(version)) {
                    EzAfk.getInstance().getLogger().info("You are running on the latest version.");
                } else {
                    EzAfk.getInstance().getLogger().info("A new version (" + version + ") of EzAfk is available.");
                    EzAfk.getInstance().getLogger().info("Please visit the following link to download the latest update:");
                    EzAfk.getInstance().getLogger().info("https://www.spigotmc.org/resources/ezafk.117430/");
                }
            });
        }
    }

    @Override
    public void unload() {

    }
}
