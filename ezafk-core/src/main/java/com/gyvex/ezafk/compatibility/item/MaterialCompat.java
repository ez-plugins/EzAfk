package com.gyvex.ezafk.compatibility.item;

import org.bukkit.Material;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class MaterialCompat {

    private MaterialCompat() {}

    /**
     * Resolves a {@link Material} by modern name first, then legacy name.
     * Uses {@code Material.matchMaterial} when available, otherwise {@code Material.valueOf}.
     *
     * @param modernName The modern material name (e.g. {@code "PLAYER_HEAD"}).
     * @param legacyName The legacy material name fallback (e.g. {@code "SKULL_ITEM"}), may be null.
     * @return The resolved {@link Material}.
     * @throws IllegalArgumentException if neither name resolves to a valid material.
     */
    public static Material resolveMaterial(String modernName, String legacyName) {
        Material material = matchMaterial(modernName);
        if (material == null && legacyName != null) {
            material = matchMaterial(legacyName);
        }
        if (material == null) {
            throw new IllegalArgumentException(
                    "Unable to resolve material for '" + modernName + "' or '" + legacyName + "'");
        }
        return material;
    }

    private static Material matchMaterial(String name) {
        if (name == null) return null;
        try {
            Method matchMaterial = Material.class.getMethod("matchMaterial", String.class);
            return (Material) matchMaterial.invoke(null, name);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
