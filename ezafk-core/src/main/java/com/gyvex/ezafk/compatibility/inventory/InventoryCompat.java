package com.gyvex.ezafk.compatibility.inventory;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class InventoryCompat {

    private InventoryCompat() {}

    /**
     * Creates an inventory with a title using the universal legacy string API.
     * Works on all Bukkit/Spigot/Paper versions; the title may contain § color codes.
     */
    public static Inventory createInventory(InventoryHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, title);
    }

    /**
     * Gets the inventory title from an {@link InventoryEvent} via reflection, compatible
     * across all API versions (InventoryView was a class until 1.20.6; it became an
     * interface in 1.21).
     *
     * @throws RuntimeException if reflection fails unexpectedly.
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
}
