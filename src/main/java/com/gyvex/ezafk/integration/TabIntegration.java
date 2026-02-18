package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.state.AfkState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import me.neznamy.tab.api.TabAPI;
import java.util.function.Consumer;
import java.lang.reflect.InvocationTargetException;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.placeholder.PlaceholderManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TabIntegration extends Integration {
    private static final int PLAYER_LIST_NAME_LIMIT = 80;
    private static final String TAB_AFK_PLACEHOLDER = "%afk%";
    private static final int TAB_AFK_PLACEHOLDER_REFRESH_INTERVAL = 200;

    private boolean tabPrefixEnabled;
    private String prefixTemplate;
    private String suffixTemplate;
    private String formatTemplate;
    private TabIntegrationMode integrationMode = TabIntegrationMode.AUTO;

    private final BukkitTabNameAdapter bukkitAdapter = new BukkitTabNameAdapter();
    private PlayerListNameAdapter tabApiAdapter;
    private boolean tabApiUnavailableLogged;
    private boolean tabPlaceholderRegistered;
    private boolean tabPlaceholderUnavailableLogged;
    private int tabInitAttempts;
    private static final int MAX_TAB_INIT_ATTEMPTS = 5;
    private enum TabAdapterFailure {
        NONE,
        API_NULL,
        CLASS_NOT_FOUND,
        LINKAGE_ERROR,
        REFLECTION_ERROR,
        OTHER
    }

    private TabAdapterFailure lastTabAdapterFailure = TabAdapterFailure.NONE;
    private Throwable lastTabAdapterThrowable;
    private boolean tabApiAvailableLogged;

    public TabIntegration() {
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        this.tabPrefixEnabled = Registry.get().getPlugin().getConfig().getBoolean("integration.tab-prefix.enabled");

        this.prefixTemplate = Registry.get().getPlugin().getConfig().getString("integration.tab-prefix.prefix", "");
        this.suffixTemplate = Registry.get().getPlugin().getConfig().getString("integration.tab-prefix.suffix", "");
            // The config keys live under 'integration.tab-prefix' in config.yml
            this.formatTemplate = Registry.get().getPlugin().getConfig().getString("integration.tab-prefix.format", "%prefix%%player%%suffix%");
        this.integrationMode = resolveIntegrationMode();
        this.tabApiUnavailableLogged = false;
        this.tabPlaceholderUnavailableLogged = false;
        this.tabApiAvailableLogged = false;
    }

    public void update() {
        refreshTabApiAdapter();
        if (shouldUseTabPlugin()) {
            ensureTabPlaceholderRegistration();
        } else {
            unregisterTabPlaceholder();
        }

        if (!isIntegrationEnabled() || !tabPrefixEnabled) {
            if (tabApiAdapter != null) {
                tabApiAdapter.restoreAll();
            }
            bukkitAdapter.restoreAll();
            unregisterTabPlaceholder();
            return;
        }

        if (tabApiAdapter != null) {
            tabApiAdapter.removeInvalidEntries();
        }
        bukkitAdapter.removeInvalidEntries();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (AfkState.isAfk(uuid)) {
                if (!applyUsingTabApi(player)) {
                    applyUsingBukkit(player);
                }
            } else {
                if (tabApiAdapter != null) {
                    tabApiAdapter.restore(uuid);
                }
                bukkitAdapter.restore(uuid);
            }
        }
    }

    private boolean applyUsingTabApi(Player player) {
        if (tabApiAdapter == null) {
            return false;
        }

        String baseName = tabApiAdapter.getBaseName(player);
        if (baseName == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        bukkitAdapter.restore(uuid);

        String targetName = buildAfkListName(baseName);
        return tabApiAdapter.apply(player, targetName);
    }

    private void applyUsingBukkit(Player player) {
        String baseName = bukkitAdapter.getBaseName(player);
        String targetName = buildAfkListName(baseName);
        bukkitAdapter.apply(player, targetName);
    }

    private String buildAfkListName(String baseName) {
        String format = formatTemplate;
        if (format == null || format.isEmpty()) {
            format = "%prefix%%player%%suffix%";
        }

        String prefix = prefixTemplate == null ? "" : prefixTemplate;
        String suffix = suffixTemplate == null ? "" : suffixTemplate;

        String combined =
                format.replace("%prefix%", prefix).replace("%player%", baseName).replace("%suffix%", suffix);
        combined = ChatColor.translateAlternateColorCodes('&', combined);
        if (combined.length() <= PLAYER_LIST_NAME_LIMIT) {
            return combined;
        }

        String trimmed = combined.substring(0, PLAYER_LIST_NAME_LIMIT);
        if (trimmed.endsWith("§")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private void refreshTabApiAdapter() {
        if (!shouldUseTabPlugin()) {
            if (tabApiAdapter != null) {
                tabApiAdapter.restoreAll();
                tabApiAdapter = null;
                tabApiAvailableLogged = false;
            }
            return;
        }

        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        boolean tabPluginEnabled = tabPlugin != null && tabPlugin.isEnabled();

        if (!tabPluginEnabled) {
            if (tabApiAdapter != null) {
                tabApiAdapter.restoreAll();
                tabApiAdapter = null;
                tabApiAvailableLogged = false;
            }
            if (integrationMode == TabIntegrationMode.TAB && !tabApiUnavailableLogged) {
                Registry.get().getLogger().log(Level.WARNING, "TAB support requested but the plugin is not installed or disabled. Falling back to Bukkit player list.");
                tabApiUnavailableLogged = true;
            }
            return;
        }

        if (tabApiAdapter == null) {
            PlayerListNameAdapter adapter = createTabApiAdapter();
            if (adapter != null) {
                tabApiAdapter = adapter;
                tabApiUnavailableLogged = false;
                tabApiAvailableLogged = false;
                tabInitAttempts = 0;

                // Try to register for TAB reload events reflectively so we
                // re-initialize when TAB reloads without a compile-time
                // dependency on event classes.
                try {
                    TabAPI api = TabAPI.getInstance();
                    if (api != null) {
                        Object eventBus = api.getClass().getMethod("getEventBus").invoke(api);
                        if (eventBus != null) {
                            try {
                                java.lang.reflect.Method register = eventBus.getClass().getMethod(
                                        "register", Class.class, Consumer.class);
                                Consumer<Object> consumer = ev -> refreshTabApiAdapter();
                                // Attempt to register TabLoadEvent and PlayerLoadEvent if present
                                try {
                                    Class<?> tabLoad = Class.forName("me.neznamy.tab.api.event.TabLoadEvent");
                                    register.invoke(eventBus, tabLoad, consumer);
                                } catch (ClassNotFoundException ignored) {
                                }
                                try {
                                    Class<?> playerLoad = Class.forName("me.neznamy.tab.api.event.PlayerLoadEvent");
                                    register.invoke(eventBus, playerLoad, consumer);
                                } catch (ClassNotFoundException ignored) {
                                }
                            } catch (NoSuchMethodException ignored) {
                                // Event bus API differs; ignore.
                            }
                        }
                    }
                } catch (LinkageError | Exception ignored) {
                    // Best-effort: ignore reflective registration failures.
                }
                // Log successful initialization once at INFO so admins see TAB integration
                try {
                    if (!tabApiAvailableLogged) {
                        TabAPI api = null;
                        try {
                            api = TabAPI.getInstance();
                        } catch (LinkageError | Exception ignored) {
                        }
                        String apiInfo = api == null ? "(TabAPI=null)" : api.getClass().getName();
                        Registry.get().getLogger().log(Level.INFO, "TAB integration enabled using adapter {0} {1}",
                                new Object[]{adapter.getClass().getName(), apiInfo});
                        tabApiAvailableLogged = true;
                    }
                } catch (Exception ignored) {
                }
            } else if (!tabApiUnavailableLogged) {
                // If TAB plugin is present but adapter failed for a non-transient
                // reason, log an immediate INFO line so admins can see the cause
                // without enabling FINE logs.
                    try {
                        TabAPI maybeApi = null;
                        try { maybeApi = TabAPI.getInstance(); } catch (Throwable ignored) {}
                        if (maybeApi != null && lastTabAdapterFailure != TabAdapterFailure.API_NULL) {
                            if (lastTabAdapterFailure == TabAdapterFailure.REFLECTION_ERROR && lastTabAdapterThrowable != null) {
                                Registry.get().getLogger().log(Level.INFO,
                                        "EzAfk TAB integration unavailable: {0} (adapter class {1})",
                                        new Object[]{lastTabAdapterFailure, (lastTabAdapterFailure == TabAdapterFailure.CLASS_NOT_FOUND ? "missing" : "present")});
                                Registry.get().getLogger().log(Level.INFO, "Detailed TAB adapter error:", lastTabAdapterThrowable);
                            } else {
                                Registry.get().getLogger().log(Level.INFO,
                                        "EzAfk TAB integration unavailable: {0} (adapter class {1})",
                                        new Object[]{lastTabAdapterFailure, (lastTabAdapterFailure == TabAdapterFailure.CLASS_NOT_FOUND ? "missing" : "present")});
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                // Distinguish between TAB plugin present but API not yet initialized
                try {
                    // Decide logging based on the specific failure reason to avoid
                    // noisy warnings when TAB is merely still initializing.
                    if (lastTabAdapterFailure == TabAdapterFailure.API_NULL || TabAPI.getInstance() == null) {
                        Registry.get().getLogger().log(Level.FINE, "TAB plugin present but API returned null; deferring adapter initialization.");
                        // schedule a bounded retry with exponential backoff
                        if (tabInitAttempts < MAX_TAB_INIT_ATTEMPTS) {
                            long delay = 40L * (1L << tabInitAttempts); // 1s,2s,4s,...
                            tabInitAttempts++;
                            Bukkit.getScheduler().runTaskLater(Registry.get().getPlugin(), this::refreshTabApiAdapter, delay);
                        } else {
                            Registry.get().getLogger().log(Level.FINE, "Exceeded TAB adapter init retries; will not retry further.");
                        }
                    } else if (lastTabAdapterFailure == TabAdapterFailure.CLASS_NOT_FOUND
                            || lastTabAdapterFailure == TabAdapterFailure.LINKAGE_ERROR
                            || lastTabAdapterFailure == TabAdapterFailure.OTHER
                            || lastTabAdapterFailure == TabAdapterFailure.REFLECTION_ERROR) {
                        // Only warn once retries are exhausted; otherwise defer silently
                        if (tabInitAttempts >= MAX_TAB_INIT_ATTEMPTS) {
                            Registry.get().getLogger().log(Level.WARNING, "TAB detected but API is unavailable. Falling back to Bukkit player list.");
                            // also emit a short INFO diagnostic for admins who don't use FINE
                            Registry.get().getLogger().log(Level.INFO, "EzAfk TAB diagnostics: lastFailure={0}, adapterClass={1}",
                                    new Object[]{lastTabAdapterFailure, (lastTabAdapterFailure == TabAdapterFailure.CLASS_NOT_FOUND ? "missing" : "present")});
                        } else {
                            Registry.get().getLogger().log(Level.FINE, "TAB adapter initialization failed ({0}); will retry.", lastTabAdapterFailure);
                        }
                    } else {
                        // Fallback conservative behavior
                        Registry.get().getLogger().log(Level.FINE, "TAB detected but adapter initialization failed; deferring.");
                    }
                } catch (LinkageError | Exception ex) {
                    Registry.get().getLogger().log(Level.WARNING, "TAB detected but API appears unusable. Falling back to Bukkit player list.");
                }
                tabApiUnavailableLogged = true;
            }
        }
    }

    private PlayerListNameAdapter createTabApiAdapter() {
        try {
            Class<?> adapterClass = Class.forName("com.gyvex.ezafk.integration.tab.TabApiPlayerListNameAdapter");
            if (!PlayerListNameAdapter.class.isAssignableFrom(adapterClass)) {
                lastTabAdapterFailure = TabAdapterFailure.OTHER;
                return null;
            }
            Object instance = adapterClass.getDeclaredConstructor().newInstance();
            lastTabAdapterFailure = TabAdapterFailure.NONE;
            lastTabAdapterThrowable = null;
            return (PlayerListNameAdapter) instance;
        } catch (ClassNotFoundException ex) {
            lastTabAdapterFailure = TabAdapterFailure.CLASS_NOT_FOUND;
            lastTabAdapterThrowable = ex;
            if (!tabApiUnavailableLogged) {
                Registry.get().getLogger().log(Level.FINE, "TAB adapter class not found", ex);
            }
            return null;
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IllegalStateException && "TAB API returned null instance".equals(cause.getMessage())) {
                lastTabAdapterFailure = TabAdapterFailure.API_NULL;
                lastTabAdapterThrowable = cause;
            } else {
                lastTabAdapterFailure = TabAdapterFailure.REFLECTION_ERROR;
                lastTabAdapterThrowable = ex;
            }
            if (!tabApiUnavailableLogged) {
                Registry.get().getLogger().log(Level.FINE, "Failed to initialize TAB API adapter", ex);
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            lastTabAdapterFailure = TabAdapterFailure.REFLECTION_ERROR;
            lastTabAdapterThrowable = ex;
            if (!tabApiUnavailableLogged) {
                Registry.get().getLogger().log(Level.FINE, "Failed to initialize TAB API adapter", ex);
            }
            return null;
        } catch (LinkageError ex) {
            lastTabAdapterFailure = TabAdapterFailure.LINKAGE_ERROR;
            lastTabAdapterThrowable = ex;
            if (!tabApiUnavailableLogged) {
                Registry.get().getLogger().log(Level.FINE, "Failed to initialize TAB API adapter", ex);
            }
            return null;
        } catch (Throwable ex) {
            lastTabAdapterFailure = TabAdapterFailure.OTHER;
            lastTabAdapterThrowable = ex;
            if (!tabApiUnavailableLogged) {
                Registry.get().getLogger().log(Level.FINE, "Failed to initialize TAB API adapter", ex);
            }
            return null;
        }
    }

    @Override
    public void load() {
        refreshTabApiAdapter();
        logTabDiagnostics();
        this.update();
        this.isSetup = true;
    }

    private void logTabDiagnostics() {
        try {
            Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
            boolean present = tabPlugin != null;
            boolean enabled = present && tabPlugin.isEnabled();
            TabAPI api = null;
            try {
                api = TabAPI.getInstance();
            } catch (LinkageError | Exception ignored) {
            }
            String apiInstance = api == null ? "null" : api.getClass().getName();
            String adapterClassAvailable;
            try {
                Class.forName("com.gyvex.ezafk.integration.tab.TabApiPlayerListNameAdapter");
                adapterClassAvailable = "present";
            } catch (ClassNotFoundException ex) {
                adapterClassAvailable = "missing";
            }

            Registry.get().getLogger().log(
                    Level.FINE,
                    "TAB diagnostics - pluginPresent={0}, pluginEnabled={1}, apiInstance={2}, adapterClass={3}, lastFailure={4}",
                    new Object[]{present, enabled, apiInstance, adapterClassAvailable, lastTabAdapterFailure});
        } catch (Exception ex) {
            Registry.get().getLogger().log(Level.FINE, "Failed to collect TAB diagnostics", ex);
        }
    }

    @Override
    public void unload() {
        if (tabApiAdapter != null) {
            tabApiAdapter.restoreAll();
        }
        bukkitAdapter.restoreAll();
        unregisterTabPlaceholder();
    }

    private void ensureTabPlaceholderRegistration() {
        if (tabPlaceholderRegistered) {
            return;
        }

        if (!shouldUseTabPlugin()) {
            return;
        }

        try {
            TabAPI tabAPI = TabAPI.getInstance();
            if (tabAPI == null) {
                return;
            }

            PlaceholderManager placeholderManager = tabAPI.getPlaceholderManager();
            if (placeholderManager == null) {
                return;
            }

            placeholderManager.registerPlayerPlaceholder(
                    TAB_AFK_PLACEHOLDER,
                    TAB_AFK_PLACEHOLDER_REFRESH_INTERVAL,
                    tabPlayer -> buildPlaceholderValue(tabPlayer));
            tabPlaceholderRegistered = true;
            tabPlaceholderUnavailableLogged = false;
        } catch (LinkageError ex) {
            handlePlaceholderRegistrationFailure(ex);
        } catch (Exception ex) {
            handlePlaceholderRegistrationFailure(ex);
        }
    }

    private String buildPlaceholderValue(TabPlayer tabPlayer) {
        if (tabPlayer == null || !tabPrefixEnabled) {
            return "";
        }

        UUID uuid = tabPlayer.getUniqueId();
        if (uuid == null || !AfkState.isAfk(uuid)) {
            return "";
        }

        String prefix = prefixTemplate == null ? "" : prefixTemplate;
        if (prefix.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    private void unregisterTabPlaceholder() {
        if (!tabPlaceholderRegistered) {
            return;
        }

        try {
            TabAPI tabAPI = TabAPI.getInstance();
            if (tabAPI != null) {
                PlaceholderManager placeholderManager = tabAPI.getPlaceholderManager();
                if (placeholderManager != null) {
                    placeholderManager.unregisterPlaceholder(TAB_AFK_PLACEHOLDER);
                }
            }
        } catch (LinkageError ignored) {
            // Ignored: best effort cleanup.
        } catch (Exception ignored) {
            // Ignored: best effort cleanup.
        }
        tabPlaceholderRegistered = false;
    }

    private void handlePlaceholderRegistrationFailure(Throwable throwable) {
        if (!tabPlaceholderUnavailableLogged) {
            Registry.get().getLogger().log(Level.FINE, "Failed to register TAB AFK placeholder", throwable);
            tabPlaceholderUnavailableLogged = true;
        }
    }

    private boolean shouldUseTabPlugin() {
        if (!isIntegrationEnabled()) {
            return false;
        }
        return integrationMode != TabIntegrationMode.CUSTOM;
    }

    private boolean isIntegrationEnabled() {
        return Registry.get().getPlugin().getConfig().getBoolean("integration.tab");
    }

    private TabIntegrationMode resolveIntegrationMode() {
        String rawValue = Registry.get().getPlugin().getConfig().getString("integration.tab-prefix.mode", "auto");
        TabIntegrationMode resolved = TabIntegrationMode.fromConfig(rawValue);
        if (resolved == null) {
            Registry.get().getLogger().log(Level.WARNING, "Unknown integration.tab-prefix.mode value '{0}'. Falling back to AUTO.", rawValue);
            return TabIntegrationMode.AUTO;
        }
        return resolved;
    }

    private enum TabIntegrationMode {
        AUTO,
        TAB,
        CUSTOM;

        private static TabIntegrationMode fromConfig(String value) {
            if (value == null) {
                return AUTO;
            }
            try {
                return TabIntegrationMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    private static class BukkitTabNameAdapter implements PlayerListNameAdapter {
        private final Map<UUID, String> originalPlayerListNames = new HashMap<>();

        @Override
        public void removeInvalidEntries() {
            originalPlayerListNames.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        }

        @Override
        public String getBaseName(Player player) {
            UUID uuid = player.getUniqueId();
            return originalPlayerListNames.computeIfAbsent(uuid, id -> {
                String currentListName = player.getPlayerListName();
                if (currentListName == null || currentListName.isEmpty()) {
                    currentListName = player.getName();
                }
                return currentListName;
            });
        }

        @Override
        public boolean apply(Player player, String targetName) {
            if (!targetName.equals(player.getPlayerListName())) {
                player.setPlayerListName(targetName);
            }
            return true;
        }

        @Override
        public boolean restore(UUID uuid) {
            String original = originalPlayerListNames.remove(uuid);
            if (original == null) {
                return false;
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setPlayerListName(original);
            }
            return true;
        }

        @Override
        public void restoreAll() {
            Set<UUID> trackedPlayers = new HashSet<>(originalPlayerListNames.keySet());
            for (UUID uuid : trackedPlayers) {
                restore(uuid);
            }
            originalPlayerListNames.clear();
        }
    }
}
