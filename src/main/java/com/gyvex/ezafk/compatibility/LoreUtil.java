package com.gyvex.ezafk.compatibility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import com.gyvex.ezafk.util.PlaceholderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for handling item lore and display name formatting with MiniMessage support.
 * Provides compatibility between Paper (Component-based) and Spigot/Bukkit (legacy string-based) APIs.
 */
public final class LoreUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private LoreUtil() {
        // Utility class
    }

    /**
     * Sets the lore on the given ItemMeta, resolving placeholders and handling MiniMessage formatting.
     * Compatible with both Paper and Spigot APIs.
     *
     * @param meta The ItemMeta to modify
     * @param rawLore The raw lore lines with MiniMessage formatting and placeholders
     * @param player The player for placeholder resolution
     * @param logger Logger for warnings
     */
    public static void setLore(ItemMeta meta, List<String> rawLore, Player player, Logger logger) {
        if (rawLore.isEmpty()) {
            return;
        }

        boolean useLegacy = rawLore.stream().anyMatch(line -> line.contains("&") && !line.contains("<"));

        if (hasMethod(meta.getClass(), "lore", List.class)) {
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                String resolvedLine = PlaceholderUtil.replacePlaceholders(player, null, line);
                if (useLegacy) {
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(resolvedLine));
                } else {
                    lore.add(MINI_MESSAGE.deserialize(resolvedLine));
                }
            }
            meta.lore(lore);
        } else {
            List<String> resolvedLore = new ArrayList<>();
            for (String line : rawLore) {
                String resolvedLine = PlaceholderUtil.replacePlaceholders(player, null, line);
                Component component = useLegacy ? LegacyComponentSerializer.legacyAmpersand().deserialize(resolvedLine) : MINI_MESSAGE.deserialize(resolvedLine);
                String legacy = LegacyComponentSerializer.legacySection().serialize(component);
                resolvedLore.add(legacy);
            }
            meta.setLore(resolvedLore);
        }
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
        if (rawDisplayName == null || rawDisplayName.isBlank()) {
            return;
        }

        String resolvedName = PlaceholderUtil.replacePlaceholders(player, null, rawDisplayName);
        boolean useLegacy = (resolvedName.contains("&") && !resolvedName.contains("<"));
        Component component = useLegacy ? LegacyComponentSerializer.legacyAmpersand().deserialize(resolvedName) : MINI_MESSAGE.deserialize(resolvedName);

        if (hasMethod(meta.getClass(), "displayName", Component.class)) {
            meta.displayName(component);
        } else {
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(component));
        }
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
        if (miniMessageString == null || miniMessageString.isBlank()) {
            return true;
        }
        try {
            MINI_MESSAGE.deserialize(miniMessageString);
            return true;
        } catch (Exception e) {
            logger.warning("Invalid MiniMessage in " + context + ": '" + miniMessageString + "' - " + e.getMessage());
            return false;
        }
    }

    private static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            clazz.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}