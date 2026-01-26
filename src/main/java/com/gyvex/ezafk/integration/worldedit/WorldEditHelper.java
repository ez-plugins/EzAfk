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
            // WorldEdit main class
            Class<?> weClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            java.lang.reflect.Method getInstance = weClass.getMethod("getInstance");
            Object weInstance = getInstance.invoke(null);

            // Session manager
            java.lang.reflect.Method getSessionManager = weClass.getMethod("getSessionManager");
            Object sessionManager = getSessionManager.invoke(weInstance);

            // sessionManager.get(player) -> LocalSession
            java.lang.reflect.Method getSession = sessionManager.getClass().getMethod("get", org.bukkit.entity.Player.class);
            Object localSession = getSession.invoke(sessionManager, p);
            if (localSession == null) return null;

            // Adapt Bukkit world to WorldEdit world using BukkitAdapter.adapt(World)
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            java.lang.reflect.Method adaptWorld = bukkitAdapter.getMethod("adapt", org.bukkit.World.class);
            Object weWorld = adaptWorld.invoke(null, p.getWorld());

            // LocalSession.getSelection(World) -> Region
            java.lang.reflect.Method getSelection = localSession.getClass().getMethod("getSelection", weWorld.getClass());
            Object region = getSelection.invoke(localSession, weWorld);
            if (region == null) return null;

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

            if (minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) return null;

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
