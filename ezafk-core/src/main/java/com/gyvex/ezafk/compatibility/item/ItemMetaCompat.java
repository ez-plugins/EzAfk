package com.gyvex.ezafk.compatibility.item;

import com.gyvex.ezafk.bootstrap.Registry;
import org.bukkit.ChatColor;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ItemMetaCompat {

    private ItemMetaCompat() {}

    private static final String META_PREFIX = ChatColor.DARK_GRAY + "ezafk:";

    private static final boolean PERSISTENT_DATA_AVAILABLE =
            isClassAvailable("org.bukkit.persistence.PersistentDataContainer")
                    && hasMethod(ItemMeta.class, "getPersistentDataContainer");

    /**
     * Stores a custom key/value pair on an {@link ItemMeta}.
     * Uses the {@code PersistentDataContainer} API when available (1.14+);
     * falls back to hidden lore on older versions.
     */
    public static void setItemMetadata(ItemMeta meta, String key, String value) {
        if (meta == null) return;
        if (PERSISTENT_DATA_AVAILABLE && setPersistentData(meta, key, value)) return;
        setLoreMetadata(meta, key, value);
    }

    /**
     * Reads a custom key/value pair from an {@link ItemMeta}.
     * Checks the {@code PersistentDataContainer} first, then falls back to lore.
     *
     * @return The stored value, or {@code null} if not found.
     */
    public static String getItemMetadata(ItemMeta meta, String key) {
        if (meta == null) return null;
        if (PERSISTENT_DATA_AVAILABLE) {
            String value = getPersistentData(meta, key);
            if (value != null) return value;
        }
        return getLoreMetadata(meta, key);
    }

    // -------------------------------------------------------------------------
    // PersistentDataContainer (1.14+)
    // -------------------------------------------------------------------------

    private static boolean setPersistentData(ItemMeta meta, String key, String value) {
        try {
            Object namespacedKey = createNamespacedKey(key);
            if (namespacedKey == null) return false;
            Object container = meta.getClass().getMethod("getPersistentDataContainer").invoke(meta);
            Class<?> pdt = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = pdt.getField("STRING").get(null);
            container.getClass()
                    .getMethod("set", namespacedKey.getClass(), pdt, Object.class)
                    .invoke(container, namespacedKey, stringType, value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String getPersistentData(ItemMeta meta, String key) {
        try {
            Object namespacedKey = createNamespacedKey(key);
            if (namespacedKey == null) return null;
            Object container = meta.getClass().getMethod("getPersistentDataContainer").invoke(meta);
            Class<?> pdt = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = pdt.getField("STRING").get(null);
            Object value = container.getClass()
                    .getMethod("get", namespacedKey.getClass(), pdt)
                    .invoke(container, namespacedKey, stringType);
            return value instanceof String ? (String) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object createNamespacedKey(String key) {
        try {
            Class<?> nkClass = Class.forName("org.bukkit.NamespacedKey");
            Constructor<?> ctor = nkClass.getConstructor(org.bukkit.plugin.Plugin.class, String.class);
            return ctor.newInstance(Registry.get().getPlugin(), key);
        } catch (Exception ignored) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Lore fallback (pre-1.14)
    // -------------------------------------------------------------------------

    private static void setLoreMetadata(ItemMeta meta, String key, String value) {
        String prefix = META_PREFIX + key + ":";
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        for (Iterator<String> it = lore.iterator(); it.hasNext(); ) {
            String line = it.next();
            if (line != null && line.startsWith(prefix)) it.remove();
        }
        lore.add(prefix + value);
        meta.setLore(lore);
    }

    private static String getLoreMetadata(ItemMeta meta, String key) {
        if (!meta.hasLore()) return null;
        String prefix = META_PREFIX + key + ":";
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(prefix)) return line.substring(prefix.length());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Reflection helpers (local to this class)
    // -------------------------------------------------------------------------

    private static boolean isClassAvailable(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)) return true;
        }
        return false;
    }
}
