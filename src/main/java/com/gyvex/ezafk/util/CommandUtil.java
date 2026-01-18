package com.gyvex.ezafk.util;

import com.gyvex.ezafk.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandUtil {
    /**
     * Checks if the sender has the required permission. Sends a message and returns false if not.
     */
    public static boolean checkPermission(CommandSender sender, String permission, String messageKey, String defaultMsg) {
        if (!sender.hasPermission(permission)) {
            MessageManager.sendMessage(sender, messageKey, defaultMsg);
            return false;
        }
        return true;
    }

    /**
     * Finds an online or offline player by name.
     */
    public static OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayer(name);
        return online != null ? online : Bukkit.getOfflinePlayer(name);
    }
}
