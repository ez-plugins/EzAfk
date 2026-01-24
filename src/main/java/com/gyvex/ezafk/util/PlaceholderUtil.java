package com.gyvex.ezafk.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PlaceholderUtil {
    private static boolean placeholderAPIAvailable = false;
    private static boolean checkedAvailability = false;
    private static final Map<String, String> placeholderCache = new ConcurrentHashMap<>();

    public static String replacePlaceholders(Player executor, Player target, String message) {
        if (message == null) {
            return null;
        }
        String result = message;
        result = result.replace("%executor%", executor != null ? executor.getName() : "");
        result = result.replace("%player%", target != null ? target.getName() : "");
        return result;
    }

    public static String colorize(String message) {
        if (message == null) {
            return null;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Checks if PlaceholderAPI is available on the server.
     * @param logger Logger for info/warning messages (may be null)
     * @return true if PlaceholderAPI is present and enabled
     */
    public static boolean isPlaceholderAPIAvailable(Logger logger) {
        if (!checkedAvailability) {
            try {
                Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
                placeholderAPIAvailable = papi != null && papi.isEnabled();

                if (logger != null) {
                    if (placeholderAPIAvailable) {
                        logger.info("PlaceholderAPI found and enabled. Placeholder support is available.");
                    } else {
                        logger.info("PlaceholderAPI not found. Placeholder resolution will use fallback behavior.");
                    }
                }
            } catch (Exception e) {
                placeholderAPIAvailable = false;
                if (logger != null) {
                    logger.info("PlaceholderAPI not available. Placeholder resolution will use fallback behavior.");
                }
            }
            checkedAvailability = true;
        }
        return placeholderAPIAvailable;
    }

    /**
     * Resolves placeholders in the given text using PlaceholderAPI if available.
     * Falls back to local replacements when PAPI is not present.
     *
     * @param player The player context for placeholder resolution
     * @param text   The text containing placeholders to resolve
     * @param logger Logger for debug messages
     * @return The text with placeholders resolved, or original text if unavailable
     */
    public static String resolvePlaceholders(Player player, String text, Logger logger) {
        if (text == null) {
            return null;
        }

        // First apply local replacements (%executor%, %player%)
        String replaced = replacePlaceholders(player, null, text);

        if (!isPlaceholderAPIAvailable(logger)) {
            return replaced;
        }

        String cacheKey = (player != null ? player.getUniqueId().toString() : "null") + ":" + replaced;
        String cached = placeholderCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String resolved = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, replaced);
            placeholderCache.put(cacheKey, resolved);
            return resolved;
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Failed to resolve placeholders via PlaceholderAPI: " + e.getMessage());
            }
            return replaced;
        }
    }

    /**
     * Resolves placeholders without player context (server-wide/offline placeholders).
     */
    public static String resolvePlaceholders(String text, Logger logger) {
        if (text == null) {
            return null;
        }
        if (!isPlaceholderAPIAvailable(logger)) {
            return text;
        }
        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(null, text);
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Failed to resolve placeholders via PlaceholderAPI: " + e.getMessage());
            }
            return text;
        }
    }

    /**
     * Resets the availability check (useful for testing).
     */
    public static void resetAvailabilityCheck() {
        checkedAvailability = false;
        placeholderAPIAvailable = false;
        placeholderCache.clear();
    }
}
