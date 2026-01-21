package com.gyvex.ezafk;

import com.gyvex.ezafk.bootstrap.Bootstrap;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import com.gyvex.ezafk.config.ConfigManager;

public class EzAfk extends JavaPlugin {
    private static EzAfk instance;
    private Bootstrap bootstrap;
    private ConfigManager configManager;

    @Override
    public void onLoad() {
        instance = this;
        configManager = new ConfigManager(this);
        bootstrap = new Bootstrap(this);
        bootstrap.onLoad();
    }

    @Override
    public void onEnable() {
        bootstrap.onEnable();
    }

    @Override
    public void onDisable() {
        bootstrap.onDisable();
    }

    public void loadConfig() {
        configManager.loadConfig();
    }
    public boolean isAfkSoundEnabled() {
        return configManager.isAfkSoundEnabled();
    }

    public String getAfkSoundFile() {
        return configManager.getAfkSoundFile();
    }

    public static EzAfk getInstance() {
        return instance;
    }

    public FileConfiguration getMessages() {
        return configManager.getMessages();
    }

    public FileConfiguration getGuiConfig() {
        return configManager.getGuiConfig();
    }

    public FileConfiguration getMysqlConfig() {
        return configManager.getMysqlConfig();
    }

    public void reloadMessages() {
        configManager.reloadMessages();
    }

    public void reloadGuiConfig() {
        configManager.reloadGuiConfig();
    }

    public void reloadMysqlConfig() {
        configManager.reloadMysqlConfig();
    }
}
