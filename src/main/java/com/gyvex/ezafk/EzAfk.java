package com.gyvex.ezafk;

import com.gyvex.ezafk.command.EzAfkCommand;
import com.gyvex.ezafk.event.MoveListener;
import com.gyvex.ezafk.event.PlayerActivityListener;
import com.gyvex.ezafk.event.PlayerQuitListener;
import com.gyvex.ezafk.gui.AfkPlayerActionsGUI;
import com.gyvex.ezafk.gui.AfkPlayerOverviewGUI;
import com.gyvex.ezafk.integration.EconomyIntegration;
import com.gyvex.ezafk.integration.MetricsIntegration;
import com.gyvex.ezafk.integration.PlaceholderApiIntegration;
import com.gyvex.ezafk.integration.SpigotIntegration;
import com.gyvex.ezafk.integration.TabIntegration;
import com.gyvex.ezafk.integration.WorldGuardIntegration;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.manager.MySQLManager;
import com.gyvex.ezafk.runnable.AfkCheckTask;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.manager.EconomyManager;
import com.gyvex.ezafk.integration.EconomyServiceListener;
import com.gyvex.ezafk.manager.AfkTimeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public class EzAfk extends JavaPlugin {
    public FileConfiguration config;
    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration mysqlConfig;
    private File messagesFile;
    private File guiFile;
    private File mysqlFile;
    private static EzAfk instance;
    private EconomyServiceListener economyServiceListener;

    @Override
    public void onLoad() {
        instance = this;

        saveDefaultConfig();
        loadConfig();

        maybeRegisterWorldGuardIntegration();
    }

    @Override
    public void onEnable() {
        logStartupBanner();

        IntegrationManager.addIntegration("metrics", new MetricsIntegration());
        IntegrationManager.addIntegration("tab", new TabIntegration());
        IntegrationManager.addIntegration("spigot", new SpigotIntegration());
        IntegrationManager.addIntegration("economy", new EconomyIntegration());
        IntegrationManager.addIntegration("placeholderapi", new PlaceholderApiIntegration());
        IntegrationManager.load();

        MySQLManager.setup();
        AfkTimeManager.load(this);

        economyServiceListener = new EconomyServiceListener();
        getServer().getPluginManager().registerEvents(economyServiceListener, this);

        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerActivityListener(), this);
        getServer().getPluginManager().registerEvents(new AfkPlayerOverviewGUI(), this);
        getServer().getPluginManager().registerEvents(new AfkPlayerActionsGUI(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);

        getCommand("ezafk").setExecutor(new EzAfkCommand(this));

        startAFKCheckTask();
    }

    private void logStartupBanner() {
        String[] banner = new String[]{
                "███████╗███████╗░█████╗░███████╗██╗░░██╗",
                "██╔════╝╚════██║██╔══██╗██╔════╝██║░██╔╝",
                "█████╗░░░░███╔═╝███████║█████╗░░█████═╝░",
                "██╔══╝░░██╔══╝░░██╔══██║██╔══╝░░██╔═██╗░",
                "███████╗███████╗██║░░██║██║░░░░░██║░╚██╗",
                "╚══════╝╚══════╝╚═╝░░╚═╝╚═╝░░░░░╚═╝░░╚═╝"
        };

        for (String line : banner) {
            getLogger().info(line);
        }
    }

    @Override
    public void onDisable() {
        AfkTimeManager.flushActiveSessions(AfkState.getActiveAfkSessions());
        if (config.getBoolean("afk.hide-screen.enabled")) {
            for (UUID uuid : new ArrayList<>(AfkState.afkPlayers)) {
                Player player = Bukkit.getPlayer(uuid);

                if (player != null) {
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                }
            }
        }

        AfkState.restoreAllDisplayNames();

        AfkState.afkPlayers.clear();
        AfkState.clearBypass();
        if (economyServiceListener != null) {
            HandlerList.unregisterAll(economyServiceListener);
            economyServiceListener = null;
        }
        IntegrationManager.unload();
        EconomyManager.reset();
        AfkTimeManager.shutdown();
    }

    private void startAFKCheckTask() {
        new AfkCheckTask().runTaskTimer(this, 20, 20);
    }

    public void loadConfig() {
        this.reloadConfig();
        this.config = getConfig();
        saveDefaultGuiConfig();
        reloadGuiConfig();
        saveDefaultMysqlConfig();
        reloadMysqlConfig();
        saveDefaultMessages();
        reloadMessages();
        EconomyManager.reset();
    }

    public static EzAfk getInstance() {
        return instance;
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

        InputStream defaultStream = getResource(fileName);

        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                messagesConfig.setDefaults(defaultConfig);
                messagesConfig.options().copyDefaults(true);
            } catch (IOException exception) {
                getLogger().log(Level.WARNING, "Failed to load default " + fileName, exception);
            }
        }
    }

    private void saveDefaultMessages() {
        saveDefaultMessages(getMessagesFileName());
    }

    private void saveDefaultMessages(String fileName) {
        File dataFolder = getDataFolder();
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
            saveResource(fileName, false);
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
                getLogger().log(Level.WARNING, "Unknown messages.language '" + language + "', defaulting to en");
                return resolveEnglishMessagesFileName();
        }
    }

    private String resolveEnglishMessagesFileName() {
        File dataFolder = getDataFolder();
        return resolveMessagesFileName("en", "messages.yml", "messages_en.yml");
    }

    private String resolveMessagesFileName(String language, String... legacyFileNames) {
        File dataFolder = getDataFolder();
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
            guiFile = new File(getDataFolder(), "gui.yml");
        }

        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        InputStream defaultStream = getResource("gui.yml");

        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                guiConfig.setDefaults(defaultConfig);
                guiConfig.options().copyDefaults(true);
            } catch (IOException exception) {
                getLogger().log(Level.WARNING, "Failed to load default gui.yml", exception);
            }
        }
    }

    private void saveDefaultGuiConfig() {
        if (guiFile == null) {
            guiFile = new File(getDataFolder(), "gui.yml");
        }

        if (!guiFile.exists()) {
            saveResource("gui.yml", false);
        }
    }

    public void reloadMysqlConfig() {
        saveDefaultMysqlConfig();

        if (mysqlFile == null) {
            mysqlFile = new File(getDataFolder(), "mysql.yml");
        }

        mysqlConfig = YamlConfiguration.loadConfiguration(mysqlFile);

        InputStream defaultStream = getResource("mysql.yml");

        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                mysqlConfig.setDefaults(defaultConfig);
                mysqlConfig.options().copyDefaults(true);
            } catch (IOException exception) {
                getLogger().log(Level.WARNING, "Failed to load default mysql.yml", exception);
            }
        }
    }

    private void saveDefaultMysqlConfig() {
        if (mysqlFile == null) {
            mysqlFile = new File(getDataFolder(), "mysql.yml");
        }

        if (!mysqlFile.exists()) {
            saveResource("mysql.yml", false);
        }
    }

    private void maybeRegisterWorldGuardIntegration() {
        if (!config.getBoolean("integration.worldguard")) {
            getLogger().fine("WorldGuard integration disabled via config");
            return;
        }

        Plugin worldGuardPlugin = getServer().getPluginManager().getPlugin("WorldGuard");

        if (worldGuardPlugin == null || !worldGuardPlugin.isEnabled()) {
            getLogger().info("WorldGuard plugin not found. Skipping integration setup.");
            return;
        }

        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
            Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            getLogger().log(Level.WARNING, "WorldGuard classes not present. Skipping integration setup.", ex);
            return;
        }

        try {
            WorldGuardIntegration integration = new WorldGuardIntegration();
            integration.setupTags();

            if (integration.isSetup) {
                IntegrationManager.addIntegration("worldguard", integration);
                getLogger().info("WorldGuard integration registered.");
            } else {
                getLogger().info("WorldGuard integration setup skipped after tag registration failure.");
            }
        } catch (NoClassDefFoundError ex) {
            getLogger().log(Level.WARNING, "Failed to initialize WorldGuard integration.", ex);
        }
    }
}
