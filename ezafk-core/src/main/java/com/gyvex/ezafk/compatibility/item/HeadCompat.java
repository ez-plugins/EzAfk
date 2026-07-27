package com.gyvex.ezafk.compatibility.item;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class HeadCompat {

    private HeadCompat() {}

    /**
     * Creates a player-head {@link ItemStack} compatible with modern ({@code PLAYER_HEAD})
     * and legacy ({@code SKULL_ITEM} with data 3) Bukkit versions.
     */
    public static ItemStack createPlayerHead() {
        Material material = MaterialCompat.resolveMaterial("PLAYER_HEAD", "SKULL_ITEM");
        ItemStack head = new ItemStack(material);
        if ("SKULL_ITEM".equals(material.name())) {
            setLegacySkullData(head);
        }
        return head;
    }

    /**
     * Returns {@code true} if the given {@link ItemStack} is a player head in any
     * supported API version.
     */
    public static boolean isPlayerHead(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        if ("PLAYER_HEAD".equals(type.name())) return true;
        if ("SKULL_ITEM".equals(type.name())) return isLegacyPlayerHead(item);
        return false;
    }

    /**
     * Sets the owning player on a {@link SkullMeta}, trying the modern
     * {@code setOwningPlayer} API first and falling back to {@code setOwner} by name.
     */
    public static void setSkullOwner(SkullMeta meta, OfflinePlayer player) {
        if (meta == null || player == null) return;
        try {
            Method setOwningPlayer = meta.getClass().getMethod("setOwningPlayer", OfflinePlayer.class);
            setOwningPlayer.invoke(meta, player);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        try {
            Method setOwner = meta.getClass().getMethod("setOwner", String.class);
            if (player.getName() != null) {
                setOwner.invoke(meta, player.getName());
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
    }

    /** Sets legacy skull data (data byte 3) on a SKULL_ITEM stack via reflection. */
    private static void setLegacySkullData(ItemStack item) {
        try {
            Class<?> materialDataClass = Class.forName("org.bukkit.material.MaterialData");
            Object skullData = materialDataClass
                    .getConstructor(Material.class, byte.class)
                    .newInstance(item.getType(), (byte) 3);
            ItemStack.class.getMethod("setData", materialDataClass).invoke(item, skullData);
        } catch (Exception e) {
            try {
                ItemStack.class.getMethod("setDurability", short.class).invoke(item, (short) 3);
            } catch (Exception ignored) {}
        }
    }

    /** Returns {@code true} if the SKULL_ITEM stack has the player-head data byte (3). */
    private static boolean isLegacyPlayerHead(ItemStack item) {
        try {
            Object data = ItemStack.class.getMethod("getData").invoke(item);
            if (data != null && data.getClass().getMethod("getData").invoke(data).equals((byte) 3)) {
                return true;
            }
        } catch (Exception e) {
            try {
                Object durability = ItemStack.class.getMethod("getDurability").invoke(item);
                return Short.valueOf((short) 3).equals(durability);
            } catch (Exception ignored) {}
        }
        return false;
    }
}
