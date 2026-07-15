package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldEditIntegration extends Integration {
    // Cached helper class name kept in-memory for faster detection
    private static String cachedHelperClass = null;

    @Override
    public void load() {
        // Only mark the integration as setup if WorldEdit is actually present
        // (either by plugin manager or by WorldEdit core classes on the classpath).
        EzAfk plugin = Registry.get().getPlugin();
        boolean worldEditPresent = false;
        try {
            // Check for WorldEdit core class
            Class.forName("com.sk89q.worldedit.WorldEdit");
            worldEditPresent = true;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            // Also allow detection via plugin manager if available
            if (plugin != null) {
                org.bukkit.plugin.Plugin wePlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
                if (wePlugin != null && wePlugin.isEnabled()) worldEditPresent = true;
            }
        } catch (Exception ignored) {
        }

        if (!worldEditPresent) {
            this.isSetup = false;
            cachedHelperClass = null;
            return;
        }

        // WorldEdit is present; prefer our helper implementations if they exist.
        String[] helpers = new String[]{
                "com.gyvex.ezafk.integration.worldedit.WorldEditHelper",
                "com.gyvex.ezafk.integration.worldedit.LegacyWorldEditHelper"
        };
        for (String cand : helpers) {
            try {
                Class.forName(cand);
                cachedHelperClass = cand;
                this.isSetup = true;
                return;
            } catch (ClassNotFoundException ignored) {
            }
        }

        // If helpers aren't present for some reason, still mark setup true so
        // the fallback reflection logic in this class can attempt to read
        // selections directly.
        this.isSetup = true;
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
                plugin.getLogger().fine("WorldEdit: trying cached helper " + cachedHelperClass);
                java.lang.reflect.Method m = helper.getMethod("getSelectionLocations", EzAfk.class, Player.class);
                Object out = m.invoke(null, plugin, p);
                if (out instanceof Location[]) {
                    plugin.getLogger().fine("WorldEdit: cached helper returned selection");
                    return (Location[]) out;
                }
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
                plugin.getLogger().fine("WorldEdit: trying helper " + cand);
                java.lang.reflect.Method m = helper.getMethod("getSelectionLocations", EzAfk.class, Player.class);
                Object out = m.invoke(null, plugin, p);
                if (out instanceof Location[]) {
                    // cache in-memory for faster future lookups
                    cachedHelperClass = cand;
                    plugin.getLogger().fine("WorldEdit: helper " + cand + " returned selection");
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
