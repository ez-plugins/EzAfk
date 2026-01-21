package com.gyvex.ezafk.bootstrap;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.command.EzAfkCommand;
import com.gyvex.ezafk.command.EzAfkTabCompleter;
import com.gyvex.ezafk.event.MoveListener;
import com.gyvex.ezafk.event.PlayerActivityListener;
import com.gyvex.ezafk.event.PlayerQuitListener;
import com.gyvex.ezafk.gui.AfkPlayerActionsGUI;
import com.gyvex.ezafk.gui.AfkPlayerOverviewGUI;
import com.gyvex.ezafk.integration.EconomyIntegration;
import com.gyvex.ezafk.integration.MetricsIntegration;
import com.gyvex.ezafk.integration.PlaceholderApiIntegration;
import com.gyvex.ezafk.integration.SimpleVoiceChatAfkListener;
import com.gyvex.ezafk.integration.SpigotIntegration;
import com.gyvex.ezafk.integration.TabIntegration;
import com.gyvex.ezafk.integration.VoiceChatIntegration;
import com.gyvex.ezafk.integration.WorldGuardIntegration;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.manager.MySQLManager;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.manager.EconomyManager;
import com.gyvex.ezafk.integration.EconomyServiceListener;
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
    private final TaskManager taskManager = new TaskManager();
    private EconomyServiceListener economyServiceListener;

    public Bootstrap(EzAfk plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        plugin.saveDefaultConfig();
        plugin.loadConfig();
        maybeRegisterWorldGuardIntegration();
    }

    public void onEnable() {
        plugin.loadConfig();
        logStartupBanner();
        // Copy default AFK sound to the EzAfk plugin folder (plugins/EzAfk/mp3/ezafk-sound.mp3)
        String afkSoundPath = plugin.config.getString("afk.sound.file", "mp3/ezafk-sound.mp3");
        java.io.File afkSoundFile = new java.io.File(plugin.getDataFolder(), afkSoundPath); // plugins/EzAfk/mp3/ezafk-sound.mp3
        // Use Bukkit's saveResource to copy the mp3 file safely
        plugin.saveResource("mp3/ezafk-sound.mp3", true);

        IntegrationManager.addIntegration("metrics", new MetricsIntegration());
        IntegrationManager.addIntegration("tab", new TabIntegration());
        IntegrationManager.addIntegration("spigot", new SpigotIntegration());
        IntegrationManager.addIntegration("economy", new EconomyIntegration());
        IntegrationManager.addIntegration("placeholderapi", new PlaceholderApiIntegration());

        String voicechatConfig = plugin.config.getString("integration.voicechat", "auto").trim().toLowerCase();
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

        MySQLManager.setup();
        AfkTimeManager.load(plugin);

        economyServiceListener = new EconomyServiceListener();
        plugin.getServer().getPluginManager().registerEvents(economyServiceListener, plugin);

        registerListener(new MoveListener(plugin));
        registerListener(new PlayerActivityListener());
        registerListener(new AfkPlayerOverviewGUI());
        registerListener(new AfkPlayerActionsGUI());
        registerListener(new PlayerQuitListener());

        // Register Simple Voice Chat AFK listener
        if (enableVoicechatIntegration) {
            registerListener(new SimpleVoiceChatAfkListener(plugin));
        }

        plugin.getCommand("ezafk").setExecutor(new EzAfkCommand(plugin));
        plugin.getCommand("ezafk").setTabCompleter(new EzAfkTabCompleter());

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
        if (plugin.config.getBoolean("afk.hide-screen.enabled")) {
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

        taskManager.cancelTasks();

        IntegrationManager.unload();
        EconomyManager.reset();
        AfkTimeManager.shutdown();
    }

    private void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        registeredListeners.add(listener);
    }

    private void maybeRegisterWorldGuardIntegration() {
        if (!plugin.config.getBoolean("integration.worldguard")) {
            plugin.getLogger().fine("WorldGuard integration disabled via config");
            return;
        }

        org.bukkit.plugin.Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

        if (worldGuardPlugin == null || !worldGuardPlugin.isEnabled()) {
            plugin.getLogger().info("WorldGuard plugin not found. Skipping integration setup.");
            return;
        }

        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
            Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            plugin.getLogger().log(Level.WARNING, "WorldGuard classes not present. Skipping integration setup.", ex);
            return;
        }

        try {
            WorldGuardIntegration integration = new WorldGuardIntegration();
            integration.setupTags();

            if (integration.isSetup) {
                IntegrationManager.addIntegration("worldguard", integration);
                plugin.getLogger().info("WorldGuard integration registered.");
            } else {
                plugin.getLogger().info("WorldGuard integration setup skipped after tag registration failure.");
            }
        } catch (NoClassDefFoundError ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize WorldGuard integration.", ex);
        }
    }
}