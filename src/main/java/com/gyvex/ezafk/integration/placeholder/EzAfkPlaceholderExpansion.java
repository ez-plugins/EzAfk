package com.gyvex.ezafk.integration.placeholder;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.state.LastActiveState;
import com.gyvex.ezafk.util.DurationFormatter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EzAfkPlaceholderExpansion extends PlaceholderExpansion {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("(-?\\d+)");

    private final boolean playtimeIntegrationEnabled;
    private final String playtimePlaceholder;

    public EzAfkPlaceholderExpansion() {
        var config = Registry.get().getPlugin().getConfig();
        this.playtimeIntegrationEnabled = config.getBoolean("integration.playtime.enabled", false);
        this.playtimePlaceholder = config.getString("integration.playtime.placeholder", "%playtime_time_total_seconds%");
    }

    @Override
    public String getIdentifier() {
        return "ezafk";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", Registry.get().getPlugin().getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return Registry.get().getPlugin().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "";
        }

        String params = identifier.toLowerCase(Locale.ROOT);

        switch (params) {
            case "afk_count":
            case "afk_players":
                return String.valueOf(AfkState.getAfkPlayerCount());
            case "active_count":
            case "active_players":
                return String.valueOf(AfkState.getActivePlayerCount());
            default:
                return handlePlayerSpecificPlaceholder(offlinePlayer, params);
        }
    }

    private String handlePlayerSpecificPlaceholder(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null) {
            return "";
        }

        UUID playerId = offlinePlayer.getUniqueId();

        return switch (params) {
            case "status" -> AfkState.isAfk(playerId) ? "AFK" : "ACTIVE";
            case "status_colored" -> AfkState.isAfk(playerId) ? "&cAFK" : "&aACTIVE";
            case "since" -> formatDurationSeconds(AfkState.getSecondsSinceAfk(playerId));
            case "last_active" -> formatDurationSeconds(LastActiveState.getSecondsSinceLastActive(playerId));
            case "total_seconds" -> formatDurationSeconds(AfkState.getTotalAfkSeconds(playerId));
            case "total" -> formatDurationPretty(AfkState.getTotalAfkSeconds(playerId));
            case "total_formatted" -> formatDurationPretty(AfkState.getTotalAfkSeconds(playerId));
            case "prefix" -> getConfigValue("afk.display-name.prefix");
            case "suffix" -> getConfigValue("afk.display-name.suffix");
            case "playtime_active_seconds" -> formatOptionalSeconds(getActivePlaytimeSeconds(offlinePlayer, playerId), false);
            case "playtime_active" -> formatOptionalSeconds(getActivePlaytimeSeconds(offlinePlayer, playerId), true);
            case "playtime_active_formatted" -> formatOptionalSeconds(getActivePlaytimeSeconds(offlinePlayer, playerId), true);
            default -> "";
        };
    }

    private String getConfigValue(String path) {
        String value = Registry.get().getPlugin().getConfig().getString(path, "");
        return value == null ? "" : value;
    }

    private String formatDurationSeconds(long seconds) {
        if (seconds < 0) {
            return "";
        }

        return String.valueOf(seconds);
    }

    private String formatDurationPretty(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }

        return DurationFormatter.formatDuration(seconds);
    }

    private OptionalLong getActivePlaytimeSeconds(OfflinePlayer offlinePlayer, UUID playerId) {
        OptionalLong playtimeSeconds = resolvePlaytimeSeconds(offlinePlayer);

        if (playtimeSeconds.isEmpty()) {
            return OptionalLong.empty();
        }

        long activeSeconds = playtimeSeconds.getAsLong() - AfkState.getTotalAfkSeconds(playerId);

        if (activeSeconds < 0) {
            activeSeconds = 0;
        }

        return OptionalLong.of(activeSeconds);
    }

    private OptionalLong resolvePlaytimeSeconds(OfflinePlayer offlinePlayer) {
        if (!isPlaytimeIntegrationActive() || offlinePlayer == null) {
            return OptionalLong.empty();
        }

        if (playtimePlaceholder == null || playtimePlaceholder.isBlank()) {
            return OptionalLong.empty();
        }

        String resolved = PlaceholderAPI.setPlaceholders(offlinePlayer, playtimePlaceholder);

        if (resolved == null) {
            return OptionalLong.empty();
        }

        resolved = resolved.trim();

        if (resolved.isEmpty()) {
            return OptionalLong.empty();
        }

        Long parsed = parseLongValue(resolved);

        if (parsed == null) {
            return OptionalLong.empty();
        }

        long seconds = parsed;

        if (seconds < 0) {
            seconds = 0;
        }

        return OptionalLong.of(seconds);
    }

    private boolean isPlaytimeIntegrationActive() {
        if (!playtimeIntegrationEnabled) {
            return false;
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("Playtime");
        return plugin != null && plugin.isEnabled();
    }

    private Long parseLongValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
        }

        Matcher matcher = INTEGER_PATTERN.matcher(value);

        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    private String formatOptionalSeconds(OptionalLong seconds, boolean pretty) {
        if (seconds.isEmpty()) {
            return "";
        }

        long value = seconds.getAsLong();
        return pretty ? formatDurationPretty(value) : formatDurationSeconds(value);
    }
}
