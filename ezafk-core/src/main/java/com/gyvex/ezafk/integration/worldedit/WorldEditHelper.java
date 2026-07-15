package com.gyvex.ezafk.integration.worldedit;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Helper for modern WorldEdit (7+) using reflection so we avoid a hard compile dependency.
 */
public class WorldEditHelper {

    /**
     * Attempts to read a WorldEdit 7+ selection for the player via reflection.
     * Returns [min, max] Locations or null.
     */
    public static Location[] getSelectionLocations(EzAfk plugin, Player p) {
        try {
            // no-op: diagnostic removed
            // WorldEdit main class
            Class<?> weClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            java.lang.reflect.Method getInstance = weClass.getMethod("getInstance");
            Object weInstance = getInstance.invoke(null);

            // Session manager
            java.lang.reflect.Method getSessionManager = weClass.getMethod("getSessionManager");
            Object sessionManager = getSessionManager.invoke(weInstance);

            // sessionManager.get(player) -> LocalSession
            Object localSession = null;
            
            for (java.lang.reflect.Method m : sessionManager.getClass().getMethods()) {
                if (!m.getName().equals("get")) continue;
                if (m.getParameterCount() != 1) continue;
                Class<?> param = m.getParameterTypes()[0];
                Object arg = null;
                // If the method accepts org.bukkit.entity.Player directly
                if (param.isInstance(p)) {
                    arg = p;
                } else {
                    // Try adapting via BukkitAdapter.adapt(Player) if available and returns correct type
                    try {
                        Class<?> ba = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                        for (java.lang.reflect.Method am : ba.getMethods()) {
                            if (!am.getName().equals("adapt")) continue;
                            if (am.getParameterCount() != 1) continue;
                            if (!am.getParameterTypes()[0].isAssignableFrom(org.bukkit.entity.Player.class)) continue;
                            if (!param.isAssignableFrom(am.getReturnType())) continue;
                            try {
                                arg = am.invoke(null, p);
                                break;
                            } catch (Exception ignored) {
                            }
                        }
                    } catch (ClassNotFoundException ignored) {
                    }
                    // Try passing name or UUID if method accepts those
                    if (arg == null) {
                        if (param.isAssignableFrom(String.class)) arg = p.getName();
                        else if (param.isAssignableFrom(java.util.UUID.class)) arg = p.getUniqueId();
                    }
                }
                if (arg == null) continue;
                try {
                    localSession = m.invoke(sessionManager, arg);
                    if (localSession != null) break;
                } catch (IllegalArgumentException ignored) {
                    // try next overload
                }
            }
            if (localSession == null) {
                return null;
            }

            // Adapt Bukkit world to WorldEdit world using BukkitAdapter.adapt(World)
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            java.lang.reflect.Method adaptWorld = bukkitAdapter.getMethod("adapt", org.bukkit.World.class);
            Object weWorld = adaptWorld.invoke(null, p.getWorld());

            // LocalSession.getSelection(World) -> Region
            Object region = null;
            for (java.lang.reflect.Method m : localSession.getClass().getMethods()) {
                if (!m.getName().equals("getSelection")) continue;
                if (m.getParameterCount() != 1) continue;
                try {
                    region = m.invoke(localSession, weWorld);
                    break;
                } catch (IllegalArgumentException ignored) {
                    // try next overload
                }
            }
            if (region == null) {
                return null;
            }

            // Region.getMinimumPoint()/getMaximumPoint() -> BlockVector3-like
            java.lang.reflect.Method getMin = region.getClass().getMethod("getMinimumPoint");
            java.lang.reflect.Method getMax = region.getClass().getMethod("getMaximumPoint");
            Object minObj = getMin.invoke(region);
            Object maxObj = getMax.invoke(region);
            if (minObj == null || maxObj == null) return null;

            Double minX = reflectGetNumber(minObj, "getX");
            Double minY = reflectGetNumber(minObj, "getY");
            Double minZ = reflectGetNumber(minObj, "getZ");
            Double maxX = reflectGetNumber(maxObj, "getX");
            Double maxY = reflectGetNumber(maxObj, "getY");
            Double maxZ = reflectGetNumber(maxObj, "getZ");

            if (minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) {
                return null;
            }

            Location minLoc = new Location(p.getWorld(), Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ));
            Location maxLoc = new Location(p.getWorld(), Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ));

            return new Location[]{minLoc, maxLoc};
        } catch (ClassNotFoundException ex) {
            // WorldEdit 7 classes not present
            return null;
        } catch (Exception ex) {
            try {
                plugin.getLogger().fine("WorldEditHelper failed: " + ex.getMessage());
            } catch (Exception ignored) {}
            return null;
        }
    }

    private static Double reflectGetNumber(Object obj, String methodName) {
        if (obj == null) return null;
        try {
            java.lang.reflect.Method m = obj.getClass().getMethod(methodName);
            Object v = m.invoke(obj);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v instanceof String) return Double.parseDouble((String) v);
        } catch (Exception ignored) {}
        return null;
    }
}
