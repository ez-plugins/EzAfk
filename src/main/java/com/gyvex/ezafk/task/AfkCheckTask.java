package com.gyvex.ezafk.task;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.compatibility.player.PlayerDisplayCompat;
import com.gyvex.ezafk.compatibility.player.PlayerKickCompat;
import com.gyvex.ezafk.integration.Integration;
import com.gyvex.ezafk.integration.WorldGuardIntegration;
import com.gyvex.ezafk.integration.ezcountdown.EzCountdownIntegration;
import com.gyvex.ezafk.integration.worldguard.flag.AfkBypassFlag;
import com.gyvex.ezafk.manager.AfkZoneManager;
import com.gyvex.ezafk.zone.Zone;
import com.gyvex.ezafk.manager.EconomyManager;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.manager.MessageManager;
import com.gyvex.ezafk.manager.AfkZoneRewardManager;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.state.LastActiveState;
import com.gyvex.ezafk.util.DurationFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.Locale;

public class AfkCheckTask extends BukkitRunnable {
    // Tracks which warnings have been sent to each player: Map<UUID, Set<Integer>>
    private static final Map<UUID, Set<Integer>> warnedPlayers = new HashMap<>();

    @Override
    public void run() {
        EzAfk plugin = Registry.get().getPlugin();
        long kickTimeoutMs = plugin.getConfig().getLong("kick.timeout") * 1000;
        long afkTimeoutMs = plugin.getConfig().getLong("afk.timeout") * 1000;
        boolean kickEnabled = plugin.getConfig().getBoolean("kick.enabled");
        boolean kickEnabledWhenFull = plugin.getConfig().getBoolean("kick.enabledWhenFull");
        long currentTime = System.currentTimeMillis();

        // Warning configuration
        boolean warningsEnabled = plugin.getConfig().getBoolean("kick.warnings.enabled", true);
        List<Integer> warningIntervals = plugin.getConfig().getIntegerList("kick.warnings.intervals");
        List<String> warningDisplays = resolveDisplays(plugin);
        boolean warningDebug = plugin.getConfig().getBoolean("kick.warnings.debug", false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            long lastActive = LastActiveState.getLastActive(player);

            if (shouldBypassAfkCheck(player, playerId)) {
                continue;
            }

            // Config-defined AFK zones: only bypass AFK checks if zone exists and rewards are disabled
            Zone zone = AfkZoneManager.getZoneForPlayer(player);
            if (zone != null && !zone.rewardEnabled) {
                continue;
            }

            if (shouldBypassWorldGuard(player)) {
                continue;
            }

                handleWarnings(player, playerId, lastActive, currentTime, afkTimeoutMs, kickTimeoutMs,
                    warningsEnabled, warningIntervals, warningDisplays, warningDebug);

            if (shouldKick(player, playerId, lastActive, currentTime, afkTimeoutMs, kickTimeoutMs, kickEnabled, kickEnabledWhenFull)) {
                kickPlayer(player, playerId);
            } else if (shouldMarkAfk(player, playerId, lastActive, currentTime, afkTimeoutMs)) {
                markPlayerAfk(player, playerId, lastActive, afkTimeoutMs, currentTime);
            }
        }

        EconomyManager.processRecurringCharges();
        AfkZoneRewardManager.processRewards();
    }

    private boolean shouldBypassAfkCheck(Player player, UUID playerId) {
        // Blacklist always wins — explicitly prevents bypass even for ops
        if (com.gyvex.ezafk.manager.BypassListManager.isBlacklisted(playerId)) return false;
        // Whitelist always grants bypass regardless of config/permission
        if (com.gyvex.ezafk.manager.BypassListManager.isWhitelisted(playerId)) return true;
        // Standard config-based bypass
        EzAfk plugin = Registry.get().getPlugin();
        return plugin.getConfig().getBoolean("afk.bypass.enabled")
                && (player.hasPermission("ezafk.bypass") || AfkState.isBypassed(playerId));
    }

    private boolean shouldBypassWorldGuard(Player player) {
        Object flag = AfkBypassFlag.get();
        if (flag == null) return false;

        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = wgInstance.getClass().getMethod("getPlatform").invoke(wgInstance);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object adaptedWorld = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null, player.getWorld());

            Object regions = container.getClass().getMethod("get", adaptedWorld.getClass()).invoke(container, adaptedWorld);
            if (regions == null) return false;

            Object blockVector = bukkitAdapterClass.getMethod("asBlockVector", org.bukkit.Location.class).invoke(null, player.getLocation());
            Object set = regions.getClass().getMethod("getApplicableRegions", blockVector.getClass()).invoke(regions, blockVector);

            for (Object region : (Iterable<?>) set) {
                try {
                    Object flagsMap = region.getClass().getMethod("getFlags").invoke(region);
                    boolean contains = (boolean) flagsMap.getClass().getMethod("containsKey", Object.class).invoke(flagsMap, flag);
                    if (contains) {
                        Object flagVal = region.getClass().getMethod("getFlag", flag.getClass()).invoke(region, flag);
                        Class<?> stateEnum = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag$State");
                        Object allow = java.lang.Enum.valueOf((Class<Enum>) stateEnum, "ALLOW");
                        if (allow.equals(flagVal)) {
                            return true;
                        }
                    }
                } catch (NoSuchMethodException nsme) {
                    // API mismatch; ignore this region and continue
                }
            }
        } catch (Throwable ignored) {
            // WorldGuard not available or error while checking regions.
            return false;
        }

        return false;
    }

    /**
     * Resolves the list of display types to use for kick warnings.
     *
     * <p>If {@code kick.warnings.displays} is set in the config it is used
     * directly (upper-cased). Otherwise the legacy {@code kick.warnings.mode}
     * value is translated: {@code chat} → [CHAT], {@code title} → [TITLE],
     * {@code both} → [CHAT, TITLE].
     */
    private static List<String> resolveDisplays(EzAfk plugin) {
        List<String> configured = plugin.getConfig().getStringList("kick.warnings.displays");
        if (!configured.isEmpty()) {
            List<String> result = new ArrayList<>(configured.size());
            for (String d : configured) {
                result.add(d.toUpperCase(Locale.ROOT));
            }
            return result;
        }
        // Backward-compat: translate legacy mode key
        String mode = plugin.getConfig().getString("kick.warnings.mode", "both");
        switch (mode.toLowerCase(Locale.ROOT)) {
            case "chat":  return Collections.singletonList("CHAT");
            case "title": return Collections.singletonList("TITLE");
            default:      return Arrays.asList("CHAT", "TITLE");
        }
    }

    private void handleWarnings(Player player, UUID playerId, long lastActive, long currentTime,
                                long afkTimeoutMs, long kickTimeoutMs, boolean warningsEnabled,
                                List<Integer> warningIntervals, List<String> warningDisplays, boolean warningDebug) {
        EzAfk plugin = Registry.get().getPlugin();
        boolean kickEnabled = plugin.getConfig().getBoolean("kick.enabled");
        if (!kickEnabled) {
            return;
        }
        long timeAfk = currentTime - lastActive - afkTimeoutMs;
        if (!warningsEnabled || !AfkState.isAfk(playerId) || timeAfk < 0 || timeAfk >= kickTimeoutMs) {
            if (warningDebug) {
                Registry.get().getLogger().info("[EzAfk][Debug] Skipping warnings: enabled=" + warningsEnabled + ", isAfk=" + AfkState.isAfk(playerId) + ", timeAfk=" + timeAfk + ", kickTimeoutMs=" + kickTimeoutMs);
            }
            return;
        }

        long secondsUntilKick = (kickTimeoutMs - timeAfk) / 1000L;
        Set<Integer> sent = warnedPlayers.computeIfAbsent(playerId, k -> new HashSet<>());
        if (warningDebug) {
            Registry.get().getLogger().info("[EzAfk][Debug] Player " + player.getName() + " secondsUntilKick=" + secondsUntilKick + ", warningIntervals=" + warningIntervals + ", sent=" + sent);
        }
        for (int interval : warningIntervals) {
            if (secondsUntilKick == interval && !sent.contains(interval)) {
                if (warningDebug) {
                    Registry.get().getLogger().info("[EzAfk][Debug] Sending warning to " + player.getName() + " for interval " + interval + "s");
                }
                sendWarning(player, interval, warningDisplays);
                sent.add(interval);
            }
        }
    }

    private void sendWarning(Player player, int seconds, List<String> displays) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("seconds", String.valueOf(seconds));

        // CHAT is always sent directly through MessageManager regardless of whether
        // EzCountdown is active, because EzCountdown's countdown format is not
        // suitable for a localised one-shot chat sentence.
        if (displays.contains("CHAT")) {
            MessageManager.sendMessage(player, "kick.warning.chat",
                    "&eYou will be kicked for being AFK in &c%seconds% &eseconds!", placeholders);
        }

        // Non-CHAT displays: delegate to EzCountdown when available so the
        // countdown ticks live and SCOREBOARD / DIALOG are supported.
        // If EzCountdown is absent or disabled, fall back to native Bukkit APIs.
        List<String> visualDisplays = new ArrayList<>(displays);
        visualDisplays.remove("CHAT");
        if (visualDisplays.isEmpty()) return;

        EzCountdownIntegration ez = getEzCountdownIntegration();
        if (ez != null) {
            String msg = MessageManager.getMessage(
                    "kick.warning.countdown",
                    "&cAFK kick in &e{seconds}s",
                    placeholders);
            if (msg == null) msg = "&cAFK kick in &e{seconds}s";
            if (!ez.sendKickWarning(player, msg, seconds, visualDisplays)) {
                // EzCountdown rejected the request; fall back to native display.
                sendWarningNative(player, seconds, visualDisplays, placeholders);
            }
        } else {
            sendWarningNative(player, seconds, visualDisplays, placeholders);
        }
    }

    /**
     * Native (non-EzCountdown) fallback for visual kick-warning displays.
     * Handles TITLE, ACTION_BAR, and BOSS_BAR via {@link PlayerDisplayCompat}.
     */
    private void sendWarningNative(Player player, int seconds, List<String> displays,
                                   Map<String, String> placeholders) {
        for (String display : displays) {
            switch (display) {
                case "TITLE": {
                    String title = MessageManager.getMessage("kick.warning.title.title", "&cAFK Warning", placeholders);
                    String subtitle = MessageManager.getMessage("kick.warning.title.subtitle", "&eKicked in &c%seconds% &esec!", placeholders);
                    PlayerDisplayCompat.sendTitle(player, title, subtitle, 10, 40, 10);
                    break;
                }
                case "ACTION_BAR": {
                    String msg = MessageManager.getMessage("kick.warning.action_bar",
                            "&eKicked in &c%seconds%s &efor being AFK!", placeholders);
                    PlayerDisplayCompat.sendActionBar(player, msg);
                    break;
                }
                case "BOSS_BAR": {
                    String msg = MessageManager.getMessage("kick.warning.boss_bar",
                            "&cAFK Warning &e\u2014 kicked in &c%seconds%s", placeholders);
                    PlayerDisplayCompat.showBossBarWarning(player, msg, seconds);
                    break;
                }
                case "SCOREBOARD":
                case "DIALOG":
                    Registry.get().getLogger().warning(
                            "[EzAfk] Display type '" + display + "' requires the EzCountdown plugin. "
                            + "Install EzCountdown or remove this type from kick.warnings.displays.");
                    break;
                default:
                    Registry.get().getLogger().warning(
                            "[EzAfk] Unknown kick warning display type: '" + display + "'. "
                            + "Supported without EzCountdown: CHAT, TITLE, ACTION_BAR, BOSS_BAR.");
                    break;
            }
        }
    }

    /**
     * Removes any active kick-warning display for a player — both the native
     * BossBar (if one was created via {@link PlayerDisplayCompat}) and any
     * EzCountdown countdown that was delegated to this integration.
     */
    public static void removeBossBar(UUID playerId) {
        PlayerDisplayCompat.removeWarningBossBar(playerId);
        EzCountdownIntegration ez = getEzCountdownIntegration();
        if (ez != null) {
            ez.removeKickWarning(playerId);
        }
    }

    /**
     * Returns the active {@link EzCountdownIntegration} if it is set up,
     * or {@code null} if EzCountdown is not available or disabled.
     */
    private static EzCountdownIntegration getEzCountdownIntegration() {
        if (!IntegrationManager.hasIntegration("ezcountdown")) return null;
        Integration integration = IntegrationManager.getIntegration("ezcountdown");
        if (!(integration instanceof EzCountdownIntegration)) return null;
        EzCountdownIntegration ez = (EzCountdownIntegration) integration;
        return ez.isSetup ? ez : null;
    }

    private boolean shouldKick(Player player, UUID playerId, long lastActive, long currentTime,
                               long afkTimeoutMs, long kickTimeoutMs,
                               boolean kickEnabled, boolean kickEnabledWhenFull) {
        boolean timeoutExceeded = currentTime - lastActive > (kickTimeoutMs + afkTimeoutMs);
        boolean canKick = kickEnabled
                || (kickEnabledWhenFull && Bukkit.getOnlinePlayers().size() == Bukkit.getMaxPlayers());
        return timeoutExceeded && canKick && AfkState.isAfk(playerId);
    }

    private void kickPlayer(Player player, UUID playerId) {
        String message = MessageManager.getMessage(
                "kick.message",
                "&cYou have been kicked from this server for being AFK too long!"
        );
        if (message == null) {
            message = "";
        }
        PlayerKickCompat.kickPlayer(player, message);
        LastActiveState.lastActive.remove(playerId);
        warnedPlayers.remove(playerId); // Reset warnings after kick
        removeBossBar(playerId);        // Remove any active boss bar
    }

    private boolean shouldMarkAfk(Player player, UUID playerId, long lastActive, long currentTime, long afkTimeoutMs) {
        return currentTime - lastActive > afkTimeoutMs
                && !AfkState.isAfk(playerId)
                && !EconomyManager.isEconomyBlocked(player);
    }

    private void markPlayerAfk(Player player, UUID playerId, long lastActive, long afkTimeoutMs, long currentTime) {
        long inactivitySeconds = (currentTime - lastActive) / 1000L;
        long timeoutSeconds = afkTimeoutMs / 1000L;
        String detail = "Inactive for " + DurationFormatter.formatDuration(inactivitySeconds)
                + " (threshold " + DurationFormatter.formatDuration(timeoutSeconds) + ")";
        AfkState.toggle(Registry.get().getPlugin(), player, false, AfkReason.INACTIVITY, detail);
        warnedPlayers.remove(playerId); // Reset warnings if player returns from AFK
        removeBossBar(playerId);        // Remove any active boss bar
    }
}
