package com.gyvex.ezafk.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PlaceholderUtil {
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
}
