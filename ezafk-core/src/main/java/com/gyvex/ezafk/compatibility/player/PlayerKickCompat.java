package com.gyvex.ezafk.compatibility.player;

import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public final class PlayerKickCompat {

    private PlayerKickCompat() {}

    /**
     * Kicks a player using the most compatible method for the server version.
     * Uses {@code Player.kick(String, PlayerKickEvent.Cause)} when available,
     * otherwise falls back to the legacy {@code Player.kickPlayer(String)}.
     *
     * @param player  The player to kick.
     * @param message The kick message.
     */
    public static void kickPlayer(Player player, String message) {
        try {
            Class<?> causeClass = null;
            try {
                causeClass = Class.forName("org.bukkit.event.player.PlayerKickEvent$Cause");
            } catch (ClassNotFoundException ignored) {}

            if (causeClass != null) {
                Object pluginCause = null;
                try {
                    //noinspection unchecked
                    pluginCause = Enum.valueOf((Class<Enum>) causeClass, "PLUGIN");
                } catch (Exception ignored) {}
                if (pluginCause != null) {
                    player.getClass().getMethod("kick", String.class, causeClass)
                            .invoke(player, message, pluginCause);
                    return;
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}

        player.kickPlayer(message);
    }
}
