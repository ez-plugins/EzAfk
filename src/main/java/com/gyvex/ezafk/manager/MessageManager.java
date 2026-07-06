package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.util.PlaceholderUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class MessageManager {

    private MessageManager() {
    }

    public static void sendMessage(CommandSender sender, String path, String fallback) {
        sendMessage(sender, path, fallback, null);
    }

    public static void sendMessage(CommandSender sender, String path, String fallback, Map<String, String> placeholders) {
        sendMessage(sender, path, fallback, placeholders, sender instanceof Player ? (Player) sender : null);
    }

    public static void sendMessage(CommandSender sender, String path, String fallback,
                                   Map<String, String> placeholders, Player playerContext) {
        if (sender == null) {
            return;
        }

        String message = getMessage(path, fallback, placeholders, playerContext);

        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public static String getMessage(String path, String fallback) {
        return getMessage(path, fallback, null);
    }

    public static String getMessage(String path, String fallback, Map<String, String> placeholders) {
        return getMessage(path, fallback, placeholders, null);
    }

    public static String getMessage(String path, String fallback, Map<String, String> placeholders, Player playerContext) {
        String message = null;
        if (Registry.get().getConfigManager() != null && Registry.get().getConfigManager().getMessages() != null) {
            message = Registry.get().getConfigManager().getMessages().getString(path);
        }

        if (message == null) {
            message = fallback;
        }

        if (message == null) {
            return null;
        }

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue() : "";
                message = message.replace("%" + entry.getKey() + "%", value);
            }
        }

        message = applyGlobalPlaceholders(message);

        if (playerContext != null) {
            message = PlaceholderUtil.resolvePlaceholderApiPlaceholders(playerContext, message, Registry.get().getLogger());
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String applyGlobalPlaceholders(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        int afkCount = AfkState.getAfkPlayerCount();
        int activeCount = AfkState.getActivePlayerCount();

        return message
                .replace("%afk_count%", String.valueOf(afkCount))
                .replace("%afk_players%", String.valueOf(afkCount))
                .replace("%active_count%", String.valueOf(activeCount))
                .replace("%active_players%", String.valueOf(activeCount));
    }
}
