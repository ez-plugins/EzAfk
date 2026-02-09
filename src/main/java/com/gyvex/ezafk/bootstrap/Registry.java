package com.gyvex.ezafk.bootstrap;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.config.ConfigManager;
import com.gyvex.ezafk.repository.StorageFactory;
import com.gyvex.ezafk.repository.StorageRepository;
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

    private EzAfk plugin = null;
    private Logger logger;
    private ConfigManager configManager;
    private FileConfiguration zonesConfig = null;
    private TaskManager taskManager = null;
    private Bootstrap bootstrap = null;
    private StorageRepository storageRepository;
    private final ArrayList<Listener> registeredListeners = new ArrayList<>();

    private Registry() {
        // plugin and logger are initialized in setPlugin(EzAfk) before init() is called.
    }

    public static void init(EzAfk plugin) {
        if (instance != null) return;

        instance = new Registry();
        instance.setPlugin(plugin);
        instance.init();
    }

    public void init() {
        if (plugin == null) {
            throw new IllegalStateException("Registry plugin not set");
        }

        this.configManager = new ConfigManager(plugin);
        this.bootstrap = new Bootstrap(plugin);
        
        // initialize storage after Registry is available
        try {
            storageRepository = StorageFactory.create();
            if (storageRepository != null) storageRepository.init();
        } catch (Exception e) {
            logger.warning("Failed to initialize storage repository: " + e.getMessage());
        }
    }

    public static Registry get() {
        if (instance == null) throw new IllegalStateException("Registry not initialized");
        return instance;
    }

    public void setPlugin(EzAfk plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
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
        if (this.taskManager == null) this.taskManager = new TaskManager();
        return taskManager;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public com.gyvex.ezafk.repository.StorageRepository getStorageRepository() {
        return storageRepository;
    }

    /**
     * Reload the storage repository based on current configuration.
     * This will create a new repository via StorageFactory, init it,
     * replace the existing repository and shutdown the old one.
     */
    public synchronized void reloadStorageRepository() {
        com.gyvex.ezafk.repository.StorageRepository previous = this.storageRepository;
        com.gyvex.ezafk.repository.StorageRepository next = null;
        try {
            next = com.gyvex.ezafk.repository.StorageFactory.create();
            if (next != null) {
                try {
                    next.init();
                } catch (Exception e) {
                    logger.warning("Failed to initialize new storage repository: " + e.getMessage());
                }
            }
            this.storageRepository = next;
        } catch (Exception e) {
            logger.warning("Failed to reload storage repository: " + e.getMessage());
        } finally {
            if (previous != null && previous != this.storageRepository) {
                try {
                    previous.shutdown();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void registerListener(Listener listener) {
        try {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            registeredListeners.add(listener);
        } catch (org.bukkit.plugin.IllegalPluginAccessException ex) {
            logger.warning("Failed to register listener " + listener.getClass().getSimpleName() + " during registry init: " + ex.getMessage());
        }
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
