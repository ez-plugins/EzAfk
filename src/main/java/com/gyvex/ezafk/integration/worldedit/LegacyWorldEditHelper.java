package com.gyvex.ezafk.integration.worldedit;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Legacy WorldEdit helper which implements the older Bukkit WorldEditPlugin selection reflection
 * logic previously embedded in WorldEditIntegration.
 */
public class LegacyWorldEditHelper {

    public static Location[] getSelectionLocations(EzAfk plugin, Player p) {
        try { plugin.getLogger().info("LegacyWorldEditHelper: attempting selection for " + p.getName()); } catch (Exception ignored) {}
        org.bukkit.plugin.Plugin wePlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        if (wePlugin == null) {
            try { plugin.getLogger().info("LegacyWorldEditHelper: WorldEdit plugin not found via PluginManager"); } catch (Exception ignored) {}
            return null;
        }
        try {
            Class<?> weClass = Class.forName("com.sk89q.worldedit.bukkit.WorldEditPlugin");
            if (!weClass.isInstance(wePlugin)) return null;
            java.lang.reflect.Method getSelection = null;
            Object selection = null;
            for (java.lang.reflect.Method m : weClass.getMethods()) {
                if (!m.getName().equals("getSelection")) continue;
                if (m.getParameterCount() != 1) continue;
                try {
                    selection = m.invoke(wePlugin, p);
                    break;
                } catch (IllegalArgumentException ignored) {
                    // try next overload
                }
            }
            if (selection == null) {
                try { plugin.getLogger().info("LegacyWorldEditHelper: selection is null"); } catch (Exception ignored) {}
                return null;
            }
            if (selection == null) return null;
            Class<?> selClass = selection.getClass();
            java.lang.reflect.Method getMinimumPoint = selClass.getMethod("getMinimumPoint");
            java.lang.reflect.Method getMaximumPoint = selClass.getMethod("getMaximumPoint");
            Object minObj = getMinimumPoint.invoke(selection);
            Object maxObj = getMaximumPoint.invoke(selection);
            if (minObj instanceof Location && maxObj instanceof Location) {
                return new Location[]{(Location) minObj, (Location) maxObj};
            }

            Double minX = reflectGetNumber(minObj, "getX");
            Double minY = reflectGetNumber(minObj, "getY");
            Double minZ = reflectGetNumber(minObj, "getZ");
            Double maxX = reflectGetNumber(maxObj, "getX");
            Double maxY = reflectGetNumber(maxObj, "getY");
            Double maxZ = reflectGetNumber(maxObj, "getZ");
            if (minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) return null;

            String worldName = null;
            try {
                java.lang.reflect.Method getWorld = selClass.getMethod("getWorld");
                Object weWorld = getWorld.invoke(selection);
                if (weWorld != null) {
                    try {
                        java.lang.reflect.Method getName = weWorld.getClass().getMethod("getName");
                        Object nameObj = getName.invoke(weWorld);
                        if (nameObj != null) worldName = nameObj.toString();
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            } catch (NoSuchMethodException ignored) {
            }

            World world = worldName != null ? org.bukkit.Bukkit.getWorld(worldName) : p.getWorld();
            if (world == null) world = p.getWorld();
            Location minLoc = new Location(world, Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ));
            Location maxLoc = new Location(world, Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ));
            return new Location[]{minLoc, maxLoc};
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ex) {
            try { plugin.getLogger().info("LegacyWorldEditHelper threw: " + ex.getMessage()); } catch (Exception ignored) {}
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
        } catch (Exception ignored) {
        }
        return null;
    }
}
