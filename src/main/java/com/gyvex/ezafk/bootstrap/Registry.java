package com.gyvex.ezafk.bootstrap;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.config.ConfigManager;
import com.gyvex.ezafk.task.TaskManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Registry {
    private static Registry instance;

    private final EzAfk plugin;
    private final Logger logger;
    private ConfigManager configManager;
    private FileConfiguration zonesConfig;
    private TaskManager taskManager;
    private Bootstrap bootstrap;
    private final ArrayList<Listener> registeredListeners = new ArrayList<>();

    private Registry(EzAfk plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configManager = new ConfigManager(plugin);
        this.taskManager = new TaskManager();
        this.bootstrap = new Bootstrap(plugin);
    }

    public static void init(EzAfk plugin) {
        if (instance != null) return;
        instance = new Registry(plugin);
    }

    public static Registry get() {
        if (instance == null) throw new IllegalStateException("Registry not initialized");
        return instance;
    }

    public EzAfk getPlugin() {
        return plugin;
    }

    public Logger getLogger() {
        return logger;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        registeredListeners.add(listener);
    }

    public void unregisterAllListeners() {
        for (Listener l : new ArrayList<>(registeredListeners)) {
            HandlerList.unregisterAll(l);
        }
        registeredListeners.clear();
    }

    public void saveDefaultZonesConfig() {
        if (this.zonesConfig == null) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
        }
        plugin.saveResource("zones.yml", false);
    }

    public void reloadZonesConfig() {
        File zonesFile = new File(plugin.getDataFolder(), "zones.yml");
        if (!zonesFile.exists()) {
            saveDefaultZonesConfig();
        }

        this.zonesConfig = YamlConfiguration.loadConfiguration(zonesFile);
    }

    public FileConfiguration getZonesConfig() {
        if (this.zonesConfig == null) {
            reloadZonesConfig();
        }
        return this.zonesConfig;
    }

    public void saveZonesConfig() {
        File zonesFile = new File(plugin.getDataFolder(), "zones.yml");
        try {
            if (this.zonesConfig != null) {
                this.zonesConfig.save(zonesFile);
            }
        } catch (IOException e) {
            logger.warning("Unable to save zones.yml: " + e.getMessage());
        }
    }
}
