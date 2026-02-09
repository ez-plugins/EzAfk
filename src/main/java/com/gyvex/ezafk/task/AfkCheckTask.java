package com.gyvex.ezafk.task;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.compatibility.CompatibilityUtil;
import com.gyvex.ezafk.integration.WorldGuardIntegration;
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
        String warningMode = plugin.getConfig().getString("kick.warnings.mode", "both");
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
                    warningsEnabled, warningIntervals, warningMode, warningDebug);

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

    private void handleWarnings(Player player, UUID playerId, long lastActive, long currentTime,
                                long afkTimeoutMs, long kickTimeoutMs, boolean warningsEnabled,
                                List<Integer> warningIntervals, String warningMode, boolean warningDebug) {
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
                sendWarning(player, interval, warningMode);
                sent.add(interval);
            }
        }
    }

    private void sendWarning(Player player, int seconds, String warningMode) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("seconds", String.valueOf(seconds));
        if (warningMode.equalsIgnoreCase("chat") || warningMode.equalsIgnoreCase("both")) {
            MessageManager.sendMessage(player, "kick.warning.chat",
                    "&eYou will be kicked for being AFK in &c%seconds% &eseconds!", placeholders);
        }
        if (warningMode.equalsIgnoreCase("title") || warningMode.equalsIgnoreCase("both")) {
            String title = MessageManager.getMessage("kick.warning.title.title", "&cAFK Warning", placeholders);
            String subtitle = MessageManager.getMessage("kick.warning.title.subtitle", "&eKicked in &c%seconds% &esec!", placeholders);
            CompatibilityUtil.sendTitle(player, title, subtitle, 10, 40, 10);
        }
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
        CompatibilityUtil.kickPlayer(player, message);
        LastActiveState.lastActive.remove(playerId);
        warnedPlayers.remove(playerId); // Reset warnings after kick
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
    }
}
