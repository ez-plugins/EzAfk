package com.gyvex.ezafk.state;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.compatibility.CompatibilityUtil;
import com.gyvex.ezafk.integration.TabIntegration;
import com.gyvex.ezafk.manager.AfkTimeManager;
import com.gyvex.ezafk.manager.EconomyManager;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.manager.MessageManager;
import com.gyvex.ezafk.manager.MySQLManager;
import com.gyvex.ezafk.event.PlayerAfkStatusChangeEvent;
import com.gyvex.ezafk.state.AfkReason;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AfkState {
    public static Set<UUID> afkPlayers = new HashSet<>();
    private static final Map<UUID, String> originalDisplayNames = new HashMap<>();
    private static final Map<UUID, Long> afkStartTimes = new HashMap<>();
    private static final Map<UUID, AfkStatusDetails> afkStatusDetails = new HashMap<>();
    private static final Map<UUID, AfkActivationMode> activationModes = new HashMap<>();
    private static final Set<UUID> bypassedPlayers = new HashSet<>();

    public static ToggleResult toggle(EzAfk plugin, Player player) {
        return toggle(plugin, player, true, AfkReason.MANUAL, null);
    }

    public static ToggleResult toggle(EzAfk plugin, Player player, boolean initiatedByPlayer) {
        AfkReason reason = initiatedByPlayer ? AfkReason.MANUAL : AfkReason.INACTIVITY;
        return toggle(plugin, player, initiatedByPlayer, reason, null);
    }

    public static ToggleResult toggle(EzAfk plugin, Player player, boolean initiatedByPlayer,
                                      AfkReason reason, String detail) {
        if (isAfk(player.getUniqueId())) {
            disable(plugin, player);
            return ToggleResult.NO_LONGER_AFK;
        }

        if (!EconomyManager.handleEnter(player, initiatedByPlayer)) {
            return ToggleResult.FAILED;
        }

        enable(plugin, player, reason, detail, AfkActivationMode.STANDARD);
        return ToggleResult.NOW_AFK;
    }

    public static void markAfk(EzAfk plugin, Player player, AfkReason reason, String detail,
                               AfkActivationMode mode) {
        UUID playerId = player.getUniqueId();
        boolean alreadyAfk = isAfk(playerId);

        if (!alreadyAfk && !EconomyManager.handleEnter(player, false)) {
            return;
        }

        enable(plugin, player, reason, detail, mode);
    }

    private static void enable(EzAfk plugin, Player player, AfkReason reason, String detail,
                               AfkActivationMode mode) {
        UUID playerId = player.getUniqueId();

        // Fire custom event for AFK status change (going AFK) BEFORE any state changes or messages
        PlayerAfkStatusChangeEvent event =
            new PlayerAfkStatusChangeEvent(player, true, reason, detail);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        afkStatusDetails.put(playerId, new AfkStatusDetails(reason, detail));

        boolean newlyAfk = afkPlayers.add(playerId);
        if (!newlyAfk) {
            return;
        }

        activationModes.put(playerId, mode);
        afkStartTimes.put(playerId, System.currentTimeMillis());

        if (mode == AfkActivationMode.STANDARD) {
            applyAfkDisplayName(plugin, player);

            String message = getStatusMessage(
                    "afk.now",
                    "&eYou are now marked as AFK. Move around to stay active!",
                    player,
                    true
            );
            if (message != null && !message.isEmpty()) {
                player.sendMessage(message);
            }

            boolean broadcastEnabled = plugin.getConfig().getBoolean("afk.broadcast.enabled");
            String broadcastMessage = getStatusMessage(
                    "afk.broadcast",
                    "&cPlayer&a %player% &cis now AFK",
                    player,
                    true
            );

            if (broadcastEnabled && broadcastMessage != null && !broadcastMessage.isEmpty()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(broadcastMessage);
                }
            }

            boolean titleEnabled = plugin.getConfig().getBoolean("afk.title.enabled");

            if (titleEnabled) {
                String title = MessageManager.getMessage("titles.afk.title", "&eAFK");
                String subtitle = MessageManager.getMessage("titles.afk.subtitle", "&7You are now AFK");

                if (title == null) {
                    title = "";
                }

                if (subtitle == null) {
                    subtitle = "";
                }

                CompatibilityUtil.sendTitle(player, title, subtitle, 10, 70, 20);
            }

            if (Registry.get().getPlugin().getConfig().getBoolean("afk.animation.enabled")) {
                StateAnimator.playAfkEnableAnimation(player);
            }

            if (plugin.getConfig().getBoolean("afk.hide-screen.enabled")) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false,
                        false
                ));
            }
        }

        if (MySQLManager.isEnabled()) {
            MySQLManager.addAfkPlayerAsync(playerId, LastActiveState.getLastActive(player));
        }

        if (mode == AfkActivationMode.STANDARD && IntegrationManager.hasIntegration("tab")) {
            TabIntegration integration = (TabIntegration) IntegrationManager.getIntegration("tab");
            integration.update();
        }
    }

    public static void disable(EzAfk plugin, Player player) {
        UUID playerId = player.getUniqueId();

        // Fire custom event for AFK status change (return from AFK) BEFORE any state changes or messages
        PlayerAfkStatusChangeEvent event =
            new PlayerAfkStatusChangeEvent(player, false,
                afkStatusDetails.get(playerId) != null ? afkStatusDetails.get(playerId).reason() : null,
                afkStatusDetails.get(playerId) != null ? afkStatusDetails.get(playerId).detail() : null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // Prevent un-AFK if cancelled
            return;
        }

        if (!afkPlayers.remove(playerId)) {
            return;
        }

        EconomyManager.onDisable(playerId);

        AfkActivationMode mode = activationModes.getOrDefault(playerId, AfkActivationMode.STANDARD);
        clearAfkStart(playerId);

        restoreDisplayName(playerId);

        if (mode == AfkActivationMode.STANDARD && plugin.getConfig().getBoolean("afk.hide-screen.enabled")) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }

        if (IntegrationManager.hasIntegration("tab")) {
            TabIntegration integration = (TabIntegration) IntegrationManager.getIntegration("tab");
            integration.update();
        }

        if (MySQLManager.isEnabled()) {
            MySQLManager.removeAfkPlayerAsync(playerId);
        }

        if (mode == AfkActivationMode.STANDARD && Registry.get().getPlugin().getConfig().getBoolean("unafk.animation.enabled")) {
            StateAnimator.playAfkDisableAnimation(player);
        }

        if (mode == AfkActivationMode.STANDARD) {
            MessageManager.sendMessage(player, "afk.no-longer", "&aYou are no longer AFK!");
        }

        if (mode == AfkActivationMode.STANDARD && plugin.getConfig().getBoolean("unafk.broadcast.enabled")) {
            String message = getStatusMessage(
                    "unafk.broadcast",
                    "&aPlayer&7 %player% &ais no longer AFK",
                    player,
                    false
            );

            if (message != null && !message.isEmpty()) {
                for (Player messagePlayer : Bukkit.getOnlinePlayers()) {
                    messagePlayer.sendMessage(message);
                }
            }
        }

        boolean titleEnabled = mode == AfkActivationMode.STANDARD && plugin.getConfig().getBoolean("unafk.title.enabled");
        if (titleEnabled) {
            String title = MessageManager.getMessage("titles.unafk.title", "&aWelcome back!");
            String subtitle = MessageManager.getMessage("titles.unafk.subtitle", "&7You are no longer AFK");

            if (title == null) {
                title = "";
            }

            if (subtitle == null) {
                subtitle = "";
            }

            CompatibilityUtil.sendTitle(player, title, subtitle, 10, 70, 20);
        }
    }

    public static boolean isAfk(UUID playerId) {
        return afkPlayers.contains(playerId);
    }

    public static int getAfkPlayerCount() {
        return afkPlayers.size();
    }

    public static int getActivePlayerCount() {
        int onlineCount = Bukkit.getOnlinePlayers().size();
        int activeCount = onlineCount - getAfkPlayerCount();
        return Math.max(0, activeCount);
    }

    public static long getAfkStartTime(UUID playerId) {
        return afkStartTimes.getOrDefault(playerId, 0L);
    }

    public static long getSecondsSinceAfk(UUID playerId) {
        Long start = afkStartTimes.get(playerId);
        if (start == null || start <= 0L) {
            return -1L;
        }

        long diff = System.currentTimeMillis() - start;
        if (diff < 0L) {
            return 0L;
        }

        return diff / 1000L;
    }

    public static long getTotalAfkSeconds(UUID playerId) {
        return AfkTimeManager.getTotalAfkSeconds(playerId);
    }

    public static AfkStatusDetails getAfkStatusDetails(UUID playerId) {
        return afkStatusDetails.get(playerId);
    }

    public static void clearAfkStart(UUID playerId) {
        Long startTime = afkStartTimes.remove(playerId);
        if (startTime != null) {
            AfkTimeManager.recordAfkSession(playerId, startTime, System.currentTimeMillis());
        }
        afkStatusDetails.remove(playerId);
        activationModes.remove(playerId);
    }

    public static boolean toggleBypass(UUID playerId) {
        if (bypassedPlayers.contains(playerId)) {
            bypassedPlayers.remove(playerId);
            return false;
        }

        bypassedPlayers.add(playerId);
        return true;
    }

    public static boolean isBypassed(UUID playerId) {
        return bypassedPlayers.contains(playerId);
    }

    public static void clearBypass() {
        bypassedPlayers.clear();
    }

    public static void restoreAllDisplayNames() {
        Map<UUID, String> snapshot = new HashMap<>(originalDisplayNames);
        for (Map.Entry<UUID, String> entry : snapshot.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setDisplayName(entry.getValue());
            }
        }
        originalDisplayNames.clear();
        afkStartTimes.clear();
        afkStatusDetails.clear();
        activationModes.clear();
    }

    public static void forgetDisplayName(UUID playerId) {
        originalDisplayNames.remove(playerId);
        clearAfkStart(playerId);
    }

    public static Map<UUID, Long> getActiveAfkSessions() {
        return new HashMap<>(afkStartTimes);
    }

    private static String getStatusMessage(String path, String fallback, Player player, boolean useDisplayNameForPlayerPlaceholder) {
        Map<String, String> placeholders = new HashMap<>();

        if (player != null) {
            String replacement = useDisplayNameForPlayerPlaceholder ? player.getDisplayName() : player.getName();

            if (replacement == null || replacement.isEmpty()) {
                replacement = player.getName();
            }

            if (replacement == null) {
                replacement = "";
            }

            placeholders.put("player", replacement);
        }

        return MessageManager.getMessage(path, fallback, placeholders);
    }

    private static void applyAfkDisplayName(EzAfk plugin, Player player) {
        if (!plugin.getConfig().getBoolean("afk.display-name.enabled")) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String baseName = originalDisplayNames.computeIfAbsent(uuid, id -> {
            String currentDisplayName = player.getDisplayName();
            if (currentDisplayName == null || currentDisplayName.isEmpty()) {
                return player.getName();
            }
            return currentDisplayName;
        });

        if (baseName == null || baseName.isEmpty()) {
            baseName = player.getName();
        }

        String prefix = plugin.getConfig().getString("afk.display-name.prefix", "&7[AFK] ");
        String suffix = plugin.getConfig().getString("afk.display-name.suffix", "");
        String format = plugin.getConfig().getString("afk.display-name.format", "%prefix%%player%%suffix%");

        if (prefix == null) {
            prefix = "";
        }
        if (suffix == null) {
            suffix = "";
        }
        if (format == null || format.isEmpty()) {
            format = "%prefix%%player%%suffix%";
        }

        String combined = format
                .replace("%prefix%", prefix)
                .replace("%player%", baseName)
                .replace("%suffix%", suffix);
        combined = ChatColor.translateAlternateColorCodes('&', combined);

        player.setDisplayName(combined);
    }

    private static void restoreDisplayName(UUID playerId) {
        String original = originalDisplayNames.remove(playerId);
        if (original == null) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.setDisplayName(original);
        }
    }
}
