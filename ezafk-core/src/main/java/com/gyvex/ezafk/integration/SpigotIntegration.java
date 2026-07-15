package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.integration.spigot.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class SpigotIntegration extends Integration {
    @Override
    public void load() {
        if (Registry.get().getPlugin().getConfig().getBoolean("integration.spigot.check-for-update")) {
            // Capture synchronously — the consumer runs async, after Registry may be torn down
            final JavaPlugin plugin = Registry.get().getPlugin();
            final Logger logger = Registry.get().getLogger();
            final String currentVersion = plugin.getDescription().getVersion();

            logger.info("Initiating update check...");

            new UpdateChecker(plugin, 117430).getVersion(version -> {
                if (currentVersion.equals(version)) {
                    logger.info("You are running on the latest version.");
                } else {
                    logger.info("A new version (" + version + ") of EzAfk is available.");
                    logger.info("Please visit the following link to download the latest update:");
                    logger.info("https://modrinth.com/plugin/ezafk");
                }
            });
        }
    }

    @Override
    public void unload() {

    }
}
