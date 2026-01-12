package com.gyvex.ezafk.compatibility;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompatibilityUtil {
    private static final String META_PREFIX = ChatColor.DARK_GRAY + "ezafk:";
    private static final boolean PERSISTENT_DATA_AVAILABLE = isClassAvailable("org.bukkit.persistence.PersistentDataContainer")
            && hasMethod(ItemMeta.class, "getPersistentDataContainer");

    /**
     * Kicks a player using the most compatible method for the server version.
     * Uses Player.kick(String, Cause) if available, otherwise falls back to Player.kickPlayer(String).
     * @param player The player to kick.
     * @param message The kick message to display to the player.
     */
    public static void kickPlayer(Player player, String message) {
        try {
            player.getClass().getMethod("kick", String.class, PlayerKickEvent.Cause.class)
                .invoke(player, message, PlayerKickEvent.Cause.PLUGIN);
        } catch (NoSuchMethodException e) {
            player.kickPlayer(message);
        } catch (Exception e) {
            player.kickPlayer(message);
        }
    }

    /**
     * Sets legacy skull data on an ItemStack for compatibility with old Bukkit versions.
     * @param item The ItemStack to modify.
     */
    private static void setLegacySkullData(ItemStack item) {
        try {
            Class<?> materialDataClass = Class.forName("org.bukkit.material.MaterialData");
            Object skullData = materialDataClass.getConstructor(Material.class, byte.class).newInstance(item.getType(), (byte) 3);
            Method setData = ItemStack.class.getMethod("setData", materialDataClass);
            setData.invoke(item, skullData);
        } catch (Exception e) {
            // As a last resort, use reflection to call setDurability if present
            try {
                Method setDurability = ItemStack.class.getMethod("setDurability", short.class);
                setDurability.invoke(item, (short) 3);
            } catch (Exception ignored) {
                // If all else fails, do nothing
            }
        }
    }

    /**
     * Checks if an ItemStack is a legacy player head (SKULL_ITEM with data 3).
     * @param item The ItemStack to check.
     * @return True if legacy player head, false otherwise.
     */
    private static boolean isLegacyPlayerHead(ItemStack item) {
        try {
            Method getData = ItemStack.class.getMethod("getData");
            Object data = getData.invoke(item);
            if (data != null && data.getClass().getMethod("getData").invoke(data).equals((byte) 3)) {
                return true;
            }
        } catch (Exception e) {
            // As a last resort, use reflection to call getDurability if present
            try {
                Method getDurability = ItemStack.class.getMethod("getDurability");
                Object durability = getDurability.invoke(item);
                return Short.valueOf((short) 3).equals(durability);
            } catch (Exception ignored) {
                // If all else fails, not a legacy player head
            }
        }
        return false;
    }
    
    /**
     * Creates an inventory with a title, compatible with all Bukkit/Spigot/Paper versions.
     * Uses Adventure Component if available, otherwise falls back to String.
     *
     * @param holder InventoryHolder (can be null)
     * @param size Inventory size
     * @param title Inventory title (String)
     * @return Inventory instance
     */
    public static Inventory createInventory(InventoryHolder holder, int size, String title) {
        // Try Adventure Component API (Paper 1.18+)
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Object component = componentClass.getMethod("text", String.class).invoke(null, org.bukkit.ChatColor.stripColor(title));
            return (Inventory) org.bukkit.Bukkit.class.getMethod("createInventory", org.bukkit.inventory.InventoryHolder.class, int.class, componentClass)
                    .invoke(null, holder, size, component);
        } catch (Exception ignored) {
            // Fallback to legacy String title
            return org.bukkit.Bukkit.createInventory(holder, size, title);
        }
    }

    /**
     * Gets the inventory title from an InventoryEvent, using reflection for compatibility across Bukkit versions.
     * In API versions 1.20.6 and earlier, InventoryView is a class. In 1.21+, it is an interface.
     * @param event The InventoryEvent to inspect.
     * @return The top inventory title from the event's InventoryView.
     * @throws RuntimeException if reflection fails.
     */
    public static String getInventoryTitle(InventoryEvent event) {
        try {
            Object view = event.getView();
            Method getTitle = view.getClass().getMethod("getTitle");
            getTitle.setAccessible(true);
            return (String) getTitle.invoke(view);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a player head ItemStack, compatible with both modern and legacy Bukkit versions.
     * @return A new ItemStack representing a player head.
     */
    public static ItemStack createPlayerHead() {
        Material material = resolveMaterial("PLAYER_HEAD", "SKULL_ITEM");
        ItemStack head = new ItemStack(material);
        if ("SKULL_ITEM".equals(material.name())) {
            setLegacySkullData(head);
        }
        return head;
    }

    /**
     * Checks if an ItemStack is a player head, compatible with both modern and legacy Bukkit versions.
     * @param item The ItemStack to check.
     * @return True if the item is a player head, false otherwise.
     */
    public static boolean isPlayerHead(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        if ("PLAYER_HEAD".equals(type.name())) {
            return true;
        }
        if ("SKULL_ITEM".equals(type.name())) {
            return isLegacyPlayerHead(item);
        }
        return false;
    }

    /**
     * Sets the owner of a SkullMeta to the specified OfflinePlayer, compatible with all Bukkit versions.
     * @param meta The SkullMeta to modify.
     * @param player The OfflinePlayer to set as owner.
     */
    public static void setSkullOwner(SkullMeta meta, OfflinePlayer player) {
        if (meta == null || player == null) {
            return;
        }
        try {
            Method setOwningPlayer = meta.getClass().getMethod("setOwningPlayer", OfflinePlayer.class);
            setOwningPlayer.invoke(meta, player);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method setOwner = meta.getClass().getMethod("setOwner", String.class);
            if (player.getName() != null) {
                setOwner.invoke(meta, player.getName());
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    /**
     * Sets custom metadata on an ItemMeta, using persistent data if available, otherwise lore.
     * @param meta The ItemMeta to modify.
     * @param key The metadata key.
     * @param value The metadata value.
     */
    public static void setItemMetadata(ItemMeta meta, String key, String value) {
        if (meta == null) {
            return;
        }
        if (PERSISTENT_DATA_AVAILABLE) {
            if (setPersistentData(meta, key, value)) {
                return;
            }
        }
        setLoreMetadata(meta, key, value);
    }

    /**
     * Gets custom metadata from an ItemMeta, using persistent data if available, otherwise lore.
     * @param meta The ItemMeta to inspect.
     * @param key The metadata key.
     * @return The metadata value, or null if not found.
     */
    public static String getItemMetadata(ItemMeta meta, String key) {
        if (meta == null) {
            return null;
        }
        if (PERSISTENT_DATA_AVAILABLE) {
            String value = getPersistentData(meta, key);
            if (value != null) {
                return value;
            }
        }
        return getLoreMetadata(meta, key);
    }

    /**
     * Sends a title and subtitle to a player, using reflection for compatibility.
     * @param player The player to send the title to.
     * @param title The main title text.
     * @param subtitle The subtitle text.
     * @param fadeIn Fade-in time in ticks.
     * @param stay Stay time in ticks.
     * @param fadeOut Fade-out time in ticks.
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        try {
            Method sendTitle = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            sendTitle.invoke(player, title, subtitle, fadeIn, stay, fadeOut);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    /**
     * Plays a sound for a player, compatible with both modern and legacy Bukkit versions.
     * Tries all provided sound names until one succeeds.
     * @param player The player to play the sound for.
     * @param volume The sound volume.
     * @param pitch The sound pitch.
     * @param soundNames One or more sound names to try.
     */
    public static void playSound(Player player, float volume, float pitch, String... soundNames) {
        if (player == null || soundNames == null) {
            return;
        }
        for (String soundName : soundNames) {
            if (soundName == null || soundName.isEmpty()) {
                continue;
            }
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
                return;
            } catch (IllegalArgumentException ignored) {
            }
        }
        try {
            Method playSound = player.getClass().getMethod("playSound", player.getLocation().getClass(), String.class, float.class, float.class);
            playSound.invoke(player, player.getLocation(), soundNames[0], volume, pitch);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    /**
     * Resolves a Material by modern or legacy name, compatible with all Bukkit versions.
     * @param modernName The modern material name.
     * @param legacyName The legacy material name.
     * @return The resolved Material.
     * @throws IllegalArgumentException if neither name is valid.
     */
    public static Material resolveMaterial(String modernName, String legacyName) {
        Material material = matchMaterial(modernName);
        if (material == null && legacyName != null) {
            material = matchMaterial(legacyName);
        }
        if (material == null) {
            throw new IllegalArgumentException("Unable to resolve material for " + modernName + " or " + legacyName);
        }
        return material;
    }

    private static Material matchMaterial(String name) {
        if (name == null) {
            return null;
        }
        try {
            Method matchMaterial = Material.class.getMethod("matchMaterial", String.class);
            return (Material) matchMaterial.invoke(null, name);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean setPersistentData(ItemMeta meta, String key, String value) {
        try {
            Object namespacedKey = createNamespacedKey(key);
            if (namespacedKey == null) {
                return false;
            }
            Object container = meta.getClass().getMethod("getPersistentDataContainer").invoke(meta);
            Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Field stringType = persistentDataTypeClass.getField("STRING");
            Object stringTypeInstance = stringType.get(null);
            Method setMethod = container.getClass().getMethod("set", namespacedKey.getClass(), persistentDataTypeClass, Object.class);
            setMethod.invoke(container, namespacedKey, stringTypeInstance, value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String getPersistentData(ItemMeta meta, String key) {
        try {
            Object namespacedKey = createNamespacedKey(key);
            if (namespacedKey == null) {
                return null;
            }
            Object container = meta.getClass().getMethod("getPersistentDataContainer").invoke(meta);
            Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Field stringType = persistentDataTypeClass.getField("STRING");
            Object stringTypeInstance = stringType.get(null);
            Method getMethod = container.getClass().getMethod("get", namespacedKey.getClass(), persistentDataTypeClass);
            Object value = getMethod.invoke(container, namespacedKey, stringTypeInstance);
            return value instanceof String ? (String) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object createNamespacedKey(String key) {
        try {
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
            Constructor<?> constructor = namespacedKeyClass.getConstructor(org.bukkit.plugin.Plugin.class, String.class);
            return constructor.newInstance(EzAfk.getInstance(), key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void setLoreMetadata(ItemMeta meta, String key, String value) {
        String prefix = META_PREFIX + key + ":";
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        for (Iterator<String> iterator = lore.iterator(); iterator.hasNext(); ) {
            String line = iterator.next();
            if (line != null && line.startsWith(prefix)) {
                iterator.remove();
            }
        }
        lore.add(prefix + value);
        meta.setLore(lore);
    }

    private static String getLoreMetadata(ItemMeta meta, String key) {
        if (!meta.hasLore()) {
            return null;
        }
        String prefix = META_PREFIX + key + ":";
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(prefix)) {
                return line.substring(prefix.length());
            }
        }
        return null;
    }

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
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
