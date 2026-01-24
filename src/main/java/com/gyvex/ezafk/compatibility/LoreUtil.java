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
 * Provides compatibility between Paper (Component-based) and Spigot/Bukkit (legacy string-based) APIs.
 */
public final class LoreUtil {

    private LoreUtil() {
        // Utility class
    }

    /**
     * Sets the lore on the given ItemMeta, resolving placeholders and handling MiniMessage formatting.
     * Uses reflection for Adventure classes so this code can run on servers without Adventure.
     */
    public static void setLore(ItemMeta meta, List<String> rawLore, Player player, Logger logger) {
        if (rawLore == null || rawLore.isEmpty()) {
            return;
        }

        boolean hasComponentLoreMethod = hasMethodByName(meta.getClass(), "lore", 1);

        if (hasComponentLoreMethod && isAdventureAvailable()) {
            // Server supports Component-based lore; construct Components reflectively
            try {
                List<Object> lore = new ArrayList<>();
                for (String line : rawLore) {
                    String resolvedLine = PlaceholderUtil.resolvePlaceholders(player, line, logger);
                    Object component = deserializeToComponent(resolvedLine);
                    if (component != null) lore.add(component);
                    else lore.add(deserializeToLegacyString(resolvedLine));
                }
                Method loreMethod = meta.getClass().getMethod("lore", List.class);
                loreMethod.invoke(meta, lore);
                return;
            } catch (Exception e) {
                if (logger != null) logger.warning("Failed to set component lore reflectively: " + e.getMessage());
                // fall through to legacy path
            }
        }

        // Fallback / Spigot/Bukkit: convert to legacy strings
        List<String> resolvedLore = new ArrayList<>();
        for (String line : rawLore) {
            String resolvedLine = PlaceholderUtil.resolvePlaceholders(player, line, logger);
            if (isAdventureAvailable()) {
                try {
                    Object component = deserializeToComponent(resolvedLine);
                    String legacy = serializeComponentToLegacy(component);
                    resolvedLore.add(legacy);
                    continue;
                } catch (Exception ignored) {
                }
            }
            resolvedLore.add(deserializeToLegacyString(resolvedLine));
        }
        meta.setLore(resolvedLore);
    }

    /**
     * Sets the display name on the given ItemMeta, resolving placeholders and handling MiniMessage formatting.
     * Compatible with both Paper and Spigot APIs.
     *
     * @param meta The ItemMeta to modify
     * @param rawDisplayName The raw display name with MiniMessage formatting and placeholders
     * @param player The player for placeholder resolution
     * @param logger Logger for warnings
     */
    public static void setDisplayName(ItemMeta meta, String rawDisplayName, Player player, Logger logger) {
        if (rawDisplayName == null || rawDisplayName.isBlank()) return;

        String resolvedName = PlaceholderUtil.resolvePlaceholders(player, rawDisplayName, logger);

        boolean hasComponentDisplayMethod = hasMethodByName(meta.getClass(), "displayName", 1);
        if (hasComponentDisplayMethod && isAdventureAvailable()) {
            try {
                Object component = deserializeToComponent(resolvedName);
                Method displayMethod = meta.getClass().getMethod("displayName", component.getClass());
                displayMethod.invoke(meta, component);
                return;
            } catch (Exception e) {
                if (logger != null) logger.warning("Failed to set displayName reflectively: " + e.getMessage());
            }
        }

        // Fallback: legacy string
        String legacy = deserializeToLegacyString(resolvedName);
        meta.setDisplayName(legacy);
    }

    /**
     * Validates a MiniMessage string by attempting to deserialize it.
     * Logs a warning if invalid.
     *
     * @param miniMessageString The string to validate
     * @param context Description of the context for logging
     * @param logger Logger for warnings
     * @return true if valid, false if invalid
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

    private static boolean hasMethodByName(Class<?> clazz, String methodName, int paramCount) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == paramCount) return true;
        }
        return false;
    }

    private static boolean isAdventureAvailable() {
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object deserializeToComponent(String miniMessage) {
        try {
            Class<?> miniCls = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Method miniFactory = miniCls.getMethod("miniMessage");
            Object mini = miniFactory.invoke(null);
            Method deserialize = miniCls.getMethod("deserialize", String.class);
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

    private static String deserializeToLegacyString(String text) {
        // Simple fallback: translate '&' color codes
        return PlaceholderUtil.colorize(text);
    }
}