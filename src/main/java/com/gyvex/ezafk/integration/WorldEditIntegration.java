package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldEditIntegration extends Integration {
    // Cached helper class name kept in-memory for faster detection
    private static String cachedHelperClass = null;

    @Override
    public void load() {
        // Prefer our integration helpers if they are available in the jar,
        // otherwise fall back to checking for WorldEdit classes on the classpath.
        String[] detectionOrder = new String[]{
                "com.gyvex.ezafk.integration.worldedit.WorldEditHelper",
                "com.gyvex.ezafk.integration.worldedit.LegacyWorldEditHelper",
                "com.sk89q.worldedit.bukkit.WorldEditPlugin"
        };

        for (String cand : detectionOrder) {
            try {
                Class.forName(cand);
                cachedHelperClass = cand;
                return;
            } catch (ClassNotFoundException ignored) {
            }
        }

        this.isSetup = false;
    }

    @Override
    public void unload() {
        this.isSetup = false;
    }

    /**
     * Attempts to read a selection for the player. First tries to delegate to helper classes
     * under com.gyvex.ezafk.integration.worldedit.* if available, otherwise falls back to
     * the built-in reflection logic which supports older WorldEdit variants.
     * Returns [min, max] Locations or null.
     */
    public static Location[] getSelectionLocations(EzAfk plugin, Player p) {
        // If a helper class was previously detected and cached, try it first.
        if (cachedHelperClass != null) {
            try {
                Class<?> helper = Class.forName(cachedHelperClass);
                java.lang.reflect.Method m = helper.getMethod("getSelectionLocations", EzAfk.class, Player.class);
                Object out = m.invoke(null, plugin, p);
                if (out instanceof Location[]) return (Location[]) out;
            } catch (Exception ex) {
                try {
                    plugin.getLogger().fine("Cached worldedit helper failed: " + ex.getMessage());
                } catch (Exception ignored) {
                }
                // Invalidate in-memory cache and fall back to detection
                cachedHelperClass = null;
            }
        }

        // Delegate to helper implementations under integration.worldedit in order of preference
        String[] candidates = new String[]{
                "com.gyvex.ezafk.integration.worldedit.WorldEditHelper",
                "com.gyvex.ezafk.integration.worldedit.LegacyWorldEditHelper"
        };

        for (String cand : candidates) {
            try {
                Class<?> helper = Class.forName(cand);
                java.lang.reflect.Method m = helper.getMethod("getSelectionLocations", EzAfk.class, Player.class);
                Object out = m.invoke(null, plugin, p);
                if (out instanceof Location[]) {
                    // cache in-memory for faster future lookups
                    cachedHelperClass = cand;
                    return (Location[]) out;
                }
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                // try next candidate
            } catch (Exception ex) {
                try {
                    plugin.getLogger().fine("integration helper " + cand + " threw: " + ex.getMessage());
                } catch (Exception ignored) {
                }
            }
        }

        // No helper provided a selection
        return null;
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
