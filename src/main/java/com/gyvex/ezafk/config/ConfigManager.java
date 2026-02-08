package com.gyvex.ezafk.config;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.gyvex.ezafk.manager.EconomyManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Level;

public class ConfigManager {
    private final EzAfk plugin;
    public FileConfiguration config;
    private boolean afkSoundEnabled;
    private String afkSoundFile;
    private boolean unafkSoundEnabled;
    private String unafkSoundFile;
    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration mysqlConfig;
    private File messagesFile;
    private File guiFile;
    private File mysqlFile;

    public ConfigManager(EzAfk plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration loadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        saveDefaultGuiConfig();
        reloadGuiConfig();
        saveDefaultMysqlConfig();
        reloadMysqlConfig();
        saveDefaultMessages();
        reloadMessages();
        EconomyManager.reset();

        // Load AFK sound config
        afkSoundEnabled = config.getBoolean("afk.sound.enabled", true);
        afkSoundFile = config.getString("afk.sound.file", "plugins/EzAfk/afk-sound.mp3");
        // Load return-from-AFK sound config
        unafkSoundEnabled = config.getBoolean("unafk.sound.enabled", true);
        unafkSoundFile = config.getString("unafk.sound.file", "mp3/ezafk-sound.mp3");
        return this.config;
    }

    public boolean isAfkSoundEnabled() {
        return afkSoundEnabled;
    }

    public String getAfkSoundFile() {
        return afkSoundFile;
    }

    public boolean isUnafkSoundEnabled() {
        return unafkSoundEnabled;
    }

    public String getUnafkSoundFile() {
        return unafkSoundFile;
    }

    public FileConfiguration getMessages() {
        if (this.messagesConfig == null) {
            reloadMessages();
        }

        return messagesConfig;
    }

    public FileConfiguration getGuiConfig() {
        if (this.guiConfig == null) {
            reloadGuiConfig();
        }

        return guiConfig;
    }

    public FileConfiguration getMysqlConfig() {
        if (this.mysqlConfig == null) {
            reloadMysqlConfig();
        }

        return mysqlConfig;
    }

    public void reloadMessages() {
        String fileName = getMessagesFileName();

        saveDefaultMessages(fileName);

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = plugin.getResource(fileName);

        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                messagesConfig.setDefaults(defaultConfig);
                messagesConfig.options().copyDefaults(true);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to load default " + fileName, exception);
            }
        }
    }

    private void saveDefaultMessages() {
        saveDefaultMessages(getMessagesFileName());
    }

    private void saveDefaultMessages(String fileName) {
        File dataFolder = plugin.getDataFolder();
        File targetFile = new File(dataFolder, fileName);

        if (messagesFile == null || !messagesFile.equals(targetFile)) {
            messagesFile = targetFile;
        }

        File parent = messagesFile.getParentFile();
        if (parent != null && !parent.exists()) {
            // Ensure the messages directory exists before attempting to save resources inside it.
            parent.mkdirs();
        }

        if (!messagesFile.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private String getMessagesFileName() {
        String language = "en";

        if (config != null) {
            String configured = config.getString("messages.language", "en");

            if (configured != null) {
                language = configured.trim().toLowerCase(Locale.ROOT);
            }
        }

        switch (language) {
            case "es":
                return resolveMessagesFileName("es", "messages_es.yml");
            case "nl":
                return resolveMessagesFileName("nl", "messages_nl.yml");
            case "ru":
                return resolveMessagesFileName("ru", "messages_ru.yml");
            case "zh":
                return resolveMessagesFileName("zh", "messages_zh.yml");
            case "en":
                return resolveEnglishMessagesFileName();
            default:
                plugin.getLogger().log(Level.WARNING, "Unknown messages.language '" + language + "', defaulting to en");
                return resolveEnglishMessagesFileName();
        }
    }

    private String resolveEnglishMessagesFileName() {
        File dataFolder = plugin.getDataFolder();
        return resolveMessagesFileName("en", "messages.yml", "messages_en.yml");
    }

    private String resolveMessagesFileName(String language, String... legacyFileNames) {
        File dataFolder = plugin.getDataFolder();
        String newFileName = "messages/" + (language.equals("en") ? "messages" : "messages_" + language) + ".yml";

        File newFile = new File(dataFolder, newFileName);
        if (newFile.exists()) {
            return newFileName;
        }

        for (String legacyFileName : legacyFileNames) {
            File legacyFile = new File(dataFolder, legacyFileName);
            if (legacyFile.exists()) {
                return legacyFileName;
            }
        }

        return newFileName;
    }

    public void reloadGuiConfig() {
        saveDefaultGuiConfig();

        if (guiFile == null) {
            guiFile = new File(plugin.getDataFolder(), "gui.yml");
        }

        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        InputStream defaultStream = plugin.getResource("gui.yml");

        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                guiConfig.setDefaults(defaultConfig);
                guiConfig.options().copyDefaults(true);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to load default gui.yml", exception);
            }
        }
    }

    private void saveDefaultGuiConfig() {
        if (guiFile == null) {
            guiFile = new File(plugin.getDataFolder(), "gui.yml");
        }

        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
    }

    public void reloadMysqlConfig() {
        saveDefaultMysqlConfig();

        if (mysqlFile == null) {
            mysqlFile = new File(plugin.getDataFolder(), "mysql.yml");
        }

        mysqlConfig = YamlConfiguration.loadConfiguration(mysqlFile);

        InputStream defaultStream = plugin.getResource("mysql.yml");

        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                mysqlConfig.setDefaults(defaultConfig);
                mysqlConfig.options().copyDefaults(true);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to load default mysql.yml", exception);
            }
        }
    }

    private void saveDefaultMysqlConfig() {
        if (mysqlFile == null) {
            mysqlFile = new File(plugin.getDataFolder(), "mysql.yml");
        }

        if (!mysqlFile.exists()) {
            plugin.saveResource("mysql.yml", false);
        }
    }
}