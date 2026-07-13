package com.gyvex.ezafk.compatibility;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import com.gyvex.ezafk.util.PlaceholderUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for handling item lore and display name formatting with MiniMessage support.
 *
 * <p>Adventure/MiniMessage is bundled (shaded) into the plugin JAR, so it is always available
 * regardless of whether the server is Paper or plain Spigot. All formatting is resolved to
 * legacy §-prefixed color strings, which are then applied via the universal
 * {@link ItemMeta#setLore}/{@link ItemMeta#setDisplayName} API. This avoids any class-type
 * mismatch between the bundled Adventure classes and the server's own Adventure API.</p>
 */
public final class LoreUtil {

    private LoreUtil() {
        // Utility class
    }

    /**
     * Sets the lore on the given ItemMeta, resolving placeholders and applying MiniMessage
     * (or legacy {@code &} color-code) formatting.
     */
    public static void setLore(ItemMeta meta, List<String> rawLore, Player player, Logger logger) {
        if (rawLore == null || rawLore.isEmpty()) {
            return;
        }

        List<String> resolvedLore = new ArrayList<>();
        for (String line : rawLore) {
            String resolvedLine = PlaceholderUtil.resolvePlaceholders(player, line, logger);
            resolvedLore.add(toDisplayString(resolvedLine, logger));
        }
        meta.setLore(resolvedLore);
    }

    /**
     * Sets the display name on the given ItemMeta, resolving placeholders and applying
     * MiniMessage (or legacy {@code &} color-code) formatting.
     */
    public static void setDisplayName(ItemMeta meta, String rawDisplayName, Player player, Logger logger) {
        if (rawDisplayName == null || rawDisplayName.isBlank()) return;

        String resolvedName = PlaceholderUtil.resolvePlaceholders(player, rawDisplayName, logger);
        meta.setDisplayName(toDisplayString(resolvedName, logger));
    }

    /**
     * Validates a MiniMessage string by attempting to deserialize it.
     */
    public static boolean validateMiniMessage(String miniMessageString, String context, Logger logger) {
        if (miniMessageString == null || miniMessageString.isBlank()) return true;
        try {
            Object comp = deserializeToComponent(miniMessageString);
            return comp != null;
        } catch (Exception e) {
            if (logger != null) logger.warning("Invalid MiniMessage in " + context + ": '" + miniMessageString + "' - " + e.getMessage());
            return false;
        }
    }

    /**
     * Converts a raw message string to a legacy §-color string suitable for
     * {@link ItemMeta#setDisplayName} / {@link ItemMeta#setLore} and chat messages.
     *
     * <p>When the bundled MiniMessage library is available (always, since it is shaded into the
     * JAR), MiniMessage tags such as {@code <red>} are parsed and the result is serialized back
     * to a legacy color string. Otherwise, {@code &}-color codes are translated directly.</p>
     */
    public static String toDisplayString(String text, Logger logger) {
        try {
            Object component = deserializeToComponent(text);
            if (component != null) {
                // Serialize MiniMessage component to §-prefixed legacy string, then
                // also translate any &-codes that MiniMessage left as plain text
                // (e.g. "&cRed" is not MiniMessage syntax, so it survives as-is).
                return PlaceholderUtil.colorize(serializeComponentToLegacy(component));
            }
        } catch (Exception e) {
            if (logger != null) logger.fine("MiniMessage parse failed, falling back to legacy colors: " + e.getMessage());
        }
        return PlaceholderUtil.colorize(text);
    }

    private static Object deserializeToComponent(String miniMessage) {
        try {
            Class<?> miniCls = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Method miniFactory = miniCls.getMethod("miniMessage");
            Object mini = miniFactory.invoke(null);
            // Adventure 4.26+ removed the single-arg deserialize(String); use the
            // raw ComponentSerializer.deserialize(Object) bridge method instead.
            Method deserialize = miniCls.getMethod("deserialize", Object.class);
            return deserialize.invoke(mini, miniMessage);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String serializeComponentToLegacy(Object component) throws Exception {
        if (component == null) return "";
        Class<?> legacyCls = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
        Method legacySection = legacyCls.getMethod("legacySection");
        Object serializer = legacySection.invoke(null);
        Method serialize = legacyCls.getMethod("serialize", Class.forName("net.kyori.adventure.text.Component"));
        return (String) serialize.invoke(serializer, component);
    }
}