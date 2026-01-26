package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.state.AfkState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.placeholder.PlaceholderManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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

    public TabIntegration() {
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        this.tabPrefixEnabled = Registry.get().getPlugin().getConfig().getBoolean("afk.tab-prefix.enabled");

        this.prefixTemplate = Registry.get().getPlugin().getConfig().getString("afk.tab-prefix.prefix", "");
        this.suffixTemplate = Registry.get().getPlugin().getConfig().getString("afk.tab-prefix.suffix", "");
        this.formatTemplate = Registry.get().getPlugin().getConfig().getString("afk.tab-prefix.format", "%prefix%%player%%suffix%");
        this.integrationMode = resolveIntegrationMode();
        this.tabApiUnavailableLogged = false;
        this.tabPlaceholderUnavailableLogged = false;
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
            }
            return;
        }

        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        boolean tabPluginEnabled = tabPlugin != null && tabPlugin.isEnabled();

        if (!tabPluginEnabled) {
            if (tabApiAdapter != null) {
                tabApiAdapter.restoreAll();
                tabApiAdapter = null;
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
            } else if (!tabApiUnavailableLogged) {
                Registry.get().getLogger().log(Level.WARNING, "TAB detected but API is unavailable. Falling back to Bukkit player list.");
                tabApiUnavailableLogged = true;
            }
        }
    }

    private PlayerListNameAdapter createTabApiAdapter() {
        try {
            Class<?> adapterClass = Class.forName("com.gyvex.ezafk.integration.TabApiPlayerListNameAdapter");
            if (!PlayerListNameAdapter.class.isAssignableFrom(adapterClass)) {
                return null;
            }
            return (PlayerListNameAdapter) adapterClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            if (!tabApiUnavailableLogged) {
                Registry.get().getLogger().log(Level.FINE, "Failed to initialize TAB API adapter", ex);
            }
            return null;
        } catch (LinkageError ex) {
            if (!tabApiUnavailableLogged) {
                Registry.get().getLogger().log(Level.FINE, "Failed to initialize TAB API adapter", ex);
            }
            return null;
        }
    }

    @Override
    public void load() {
        refreshTabApiAdapter();
        this.update();
        this.isSetup = true;
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
        String rawValue = Registry.get().getPlugin().getConfig().getString("afk.tab-prefix.mode", "auto");
        TabIntegrationMode resolved = TabIntegrationMode.fromConfig(rawValue);
        if (resolved == null) {
            Registry.get().getLogger().log(Level.WARNING, "Unknown afk.tab-prefix.mode value '{0}'. Falling back to AUTO.", rawValue);
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
