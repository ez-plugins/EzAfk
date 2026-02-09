package com.gyvex.ezafk.bootstrap;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.command.EzAfkCommand;
import com.gyvex.ezafk.command.EzAfkTabCompleter;
import com.gyvex.ezafk.listener.MoveListener;
import com.gyvex.ezafk.listener.PlayerActivityListener;
import com.gyvex.ezafk.listener.PlayerQuitListener;
import com.gyvex.ezafk.listener.AfkPlayerActionsGUI;
import com.gyvex.ezafk.listener.AfkPlayerOverviewGUI;
import com.gyvex.ezafk.integration.EconomyIntegration;
import com.gyvex.ezafk.integration.MetricsIntegration;
import com.gyvex.ezafk.integration.PlaceholderApiIntegration;
import com.gyvex.ezafk.listener.SimpleVoiceChatAfkListener;
import com.gyvex.ezafk.integration.SpigotIntegration;
import com.gyvex.ezafk.integration.TabIntegration;
import com.gyvex.ezafk.integration.VoiceChatIntegration;
import com.gyvex.ezafk.integration.WorldEditIntegration;
import com.gyvex.ezafk.integration.WorldGuardIntegration;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.manager.EconomyManager;
import com.gyvex.ezafk.listener.EconomyServiceListener;
import com.gyvex.ezafk.manager.AfkTimeManager;
import com.gyvex.ezafk.task.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

public class Bootstrap {
    private final EzAfk plugin;
    private final ArrayList<Listener> registeredListeners = new ArrayList<>();
    private TaskManager taskManager = null;
    private com.gyvex.ezafk.listener.EconomyServiceListener economyServiceListener;

    public Bootstrap(EzAfk plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        plugin.saveDefaultConfig();
        Registry.get().getConfigManager().loadConfig();
        maybeRegisterWorldGuardIntegration();
        maybeRegisterWorldEditIntegration();
    }

    public void onEnable() {
        Registry.get().getConfigManager().loadConfig();
        logStartupBanner();
        // Copy default AFK sound to the EzAfk plugin folder (plugins/EzAfk/mp3/ezafk-sound.mp3)
        String afkSoundPath = Registry.get().getConfigManager().getAfkSoundFile();
        java.io.File afkSoundFile = new java.io.File(Registry.get().getPlugin().getDataFolder(), afkSoundPath); // plugins/EzAfk/mp3/ezafk-sound.mp3
        // Use Bukkit's saveResource to copy the mp3 file safely
        plugin.saveResource("mp3/ezafk-sound.mp3", true);

        IntegrationManager.addIntegration("metrics", new MetricsIntegration());
        IntegrationManager.addIntegration("tab", new TabIntegration());
        IntegrationManager.addIntegration("spigot", new SpigotIntegration());
        IntegrationManager.addIntegration("economy", new EconomyIntegration());
        IntegrationManager.addIntegration("placeholderapi", new PlaceholderApiIntegration());

        String voicechatConfig = plugin.getConfig().getString("integration.voicechat", "auto").trim().toLowerCase();
        boolean voicechatAvailable = plugin.getServer().getPluginManager().getPlugin("voicechat") != null;
        boolean enableVoicechatIntegration = false;
        if ("true".equals(voicechatConfig)) {
            enableVoicechatIntegration = true;
        } else if ("auto".equals(voicechatConfig)) {
            enableVoicechatIntegration = voicechatAvailable;
        } // false disables integration

        if (enableVoicechatIntegration) {
            IntegrationManager.addIntegration("voicechat", new VoiceChatIntegration(plugin));
        }
        IntegrationManager.load();

        String storageType = plugin.getConfig().getString("storage.type", "yaml").trim().toLowerCase();
        plugin.getLogger().fine("Storage type selected: " + storageType);
        // Storage repository is initialized during Registry.init(); on reload the command will refresh it.
        AfkTimeManager.load(plugin);

        economyServiceListener = new EconomyServiceListener();
        try {
            plugin.getServer().getPluginManager().registerEvents(economyServiceListener, plugin);
        } catch (org.bukkit.plugin.IllegalPluginAccessException ex) {
            plugin.getLogger().warning("Failed to register EconomyServiceListener during enable: " + ex.getMessage());
        }

        registerListener(new MoveListener(plugin));
        registerListener(new PlayerActivityListener());
        registerListener(new AfkPlayerOverviewGUI());
        registerListener(new AfkPlayerActionsGUI());
        registerListener(new PlayerQuitListener());

        // Register Simple Voice Chat AFK listener
        if (enableVoicechatIntegration) {
            registerListener(new SimpleVoiceChatAfkListener(plugin));
        }

        if (plugin.getCommand("ezafk") != null) {
            plugin.getCommand("ezafk").setExecutor(new EzAfkCommand(plugin));
            plugin.getCommand("ezafk").setTabCompleter(new EzAfkTabCompleter());
        } else {
            plugin.getLogger().warning("Command 'ezafk' not found in plugin description; skipping command registration.");
        }

        // Initialize and start task manager here to avoid loading task classes during onLoad/init.
        this.taskManager = new TaskManager();
        taskManager.startAfkCheckTask(plugin);
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
            plugin.getLogger().info(line);
        }
    }

    public void onDisable() {
        AfkTimeManager.flushActiveSessions(AfkState.getActiveAfkSessions());
        if (plugin.getConfig().getBoolean("afk.hide-screen.enabled")) {
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

        // Unregister all listeners
        for (Listener listener : registeredListeners) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();

        if (economyServiceListener != null) {
            HandlerList.unregisterAll(economyServiceListener);
            economyServiceListener = null;
        }

        if (taskManager != null) {
            taskManager.cancelTasks();
            taskManager = null;
        }

        IntegrationManager.unload();
        EconomyManager.reset();
        AfkTimeManager.shutdown();
    }

    private void registerListener(Listener listener) {
        try {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            registeredListeners.add(listener);
        } catch (org.bukkit.plugin.IllegalPluginAccessException ex) {
            plugin.getLogger().warning("Failed to register listener " + listener.getClass().getSimpleName() + " during enable: " + ex.getMessage());
        }
    }

    private void maybeRegisterWorldGuardIntegration() {

        if (!plugin.getConfig().getBoolean("integration.worldguard")) {
            plugin.getLogger().fine("WorldGuard integration disabled via config");
            return;
        }

        org.bukkit.plugin.Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

        if (worldGuardPlugin == null || !worldGuardPlugin.isEnabled()) {
            plugin.getLogger().info("WorldGuard plugin not found. Attempting class-based detection and flag registration.");
            // Continue: even if the PluginManager does not report WorldGuard, try
            // class-based detection and flag registration. This covers cases where
            // classes are present on the classpath or WorldGuard is loaded differently.
        }

        // Create and register the integration instance now, but defer calling
        // its `load()` method until IntegrationManager.load() runs in onEnable().
        // This prevents double-invocation of setup logic.
        try {
            // Allow test/runtime overrides to block registration
            // (IntegrationManager.setRegistrationAllowed can be used by tests)
            if (!IntegrationManager.isRegistrationAllowed("worldguard")) {
                plugin.getLogger().info("WorldGuard integration explicitly disabled by test override.");
                return;
            }
            WorldGuardIntegration integration = new WorldGuardIntegration();
            IntegrationManager.addIntegration("worldguard", integration);
            plugin.getLogger().info("WorldGuard integration registered.");
        } catch (NoClassDefFoundError ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize WorldGuard integration.", ex);
        }
    }

    private void maybeRegisterWorldEditIntegration() {
        if (!plugin.getConfig().getBoolean("integration.worldedit", true)) {
            plugin.getLogger().info("WorldEdit integration disabled via config");
            return;
        }

        org.bukkit.plugin.Plugin wePlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");

        if (wePlugin == null || !wePlugin.isEnabled()) {
            plugin.getLogger().info("WorldEdit plugin not found. Skipping integration setup.");
            return;
        }

        try {
            WorldEditIntegration integration = new WorldEditIntegration();
            integration.load();

            if (integration.isSetup) {
                IntegrationManager.addIntegration("worldedit", integration);
                plugin.getLogger().info("WorldEdit integration registered.");
            } else {
                plugin.getLogger().info("WorldEdit integration setup skipped.");
            }
        } catch (NoClassDefFoundError ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to initialize WorldEdit integration.", ex);
        }
    }
}