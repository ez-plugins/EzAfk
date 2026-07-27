package com.gyvex.ezafk.compatibility.player;

import com.gyvex.ezafk.bootstrap.Registry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerDisplayCompat {

    private PlayerDisplayCompat() {}

    /**
     * Active BossBar instances used for kick-warning displays, keyed by player UUID.
     * Stored as {@link Object} so this class compiles on pre-1.9 servers that lack
     * the BossBar API. All access goes through reflection.
     */
    private static final Map<UUID, Object> activeBossBars = new HashMap<>();

    /**
     * Sends a title and subtitle to a player via reflection for cross-version compatibility.
     */
    public static void sendTitle(Player player, String title, String subtitle,
                                 int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        try {
            Method sendTitle = player.getClass().getMethod(
                    "sendTitle", String.class, String.class, int.class, int.class, int.class);
            sendTitle.invoke(player, title, subtitle, fadeIn, stay, fadeOut);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
    }

    /**
     * Sends an action-bar message to a player via reflection (Paper/Spigot 1.13+).
     * Silently skipped on older server versions.
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        try {
            Method sendActionBar = player.getClass().getMethod("sendActionBar", String.class);
            sendActionBar.invoke(player, message);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
    }

    /**
     * Shows a temporary BossBar kick-warning to a player for {@code seconds} duration,
     * then removes it automatically. All BossBar API access uses reflection so the
     * method is safe on pre-1.9 servers; the call is silently skipped if unavailable.
     */
    public static void showBossBarWarning(Player player, String message, int seconds) {
        if (player == null || message == null) return;
        UUID playerId = player.getUniqueId();
        removeWarningBossBar(playerId);
        try {
            Class<?> barColorClass = Class.forName("org.bukkit.boss.BarColor");
            Class<?> barStyleClass = Class.forName("org.bukkit.boss.BarStyle");
            //noinspection unchecked
            Object barColor = Enum.valueOf((Class<Enum>) barColorClass, "RED");
            //noinspection unchecked
            Object barStyle = Enum.valueOf((Class<Enum>) barStyleClass, "SOLID");

            String title = ChatColor.translateAlternateColorCodes('&', message);
            Method createBossBar = Bukkit.class.getMethod(
                    "createBossBar", String.class, barColorClass, barStyleClass);
            Object bar = createBossBar.invoke(null, title, barColor, barStyle);

            bar.getClass().getMethod("addPlayer", Player.class).invoke(bar, player);
            activeBossBars.put(playerId, bar);

            long ticksToRemove = Math.max(1L, (long) seconds * 20L);
            Registry.get().getScheduler().runTaskLater(
                    () -> removeWarningBossBar(playerId),
                    ticksToRemove);
        } catch (ClassNotFoundException | NoSuchMethodException
                 | IllegalAccessException | InvocationTargetException ignored) {}
    }

    /**
     * Removes and hides the active BossBar kick-warning for a player, if any.
     *
     * @param playerId UUID of the player whose BossBar should be removed.
     */
    public static void removeWarningBossBar(UUID playerId) {
        Object bar = activeBossBars.remove(playerId);
        if (bar == null) return;
        try {
            bar.getClass().getMethod("removeAll").invoke(bar);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
    }
}
