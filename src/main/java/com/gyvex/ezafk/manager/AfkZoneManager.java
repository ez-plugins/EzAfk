package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class AfkZoneManager {
    private static final List<Zone> zones = new ArrayList<>();
    private AfkZoneManager() {}

    public static void load(EzAfk plugin) {
        zones.clear();
        if (plugin == null) return;

        org.bukkit.configuration.file.FileConfiguration zonesConfig = plugin.getZonesConfig();
        if (zonesConfig == null) return;

        if (!zonesConfig.getBoolean("enabled", false)) return;

        List<?> list = zonesConfig.getList("regions");
        if (list == null) return;

        for (Object o : list) {
            if (!(o instanceof java.util.Map)) continue;
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) o;

            String worldName = (String) map.getOrDefault("world", "world");
            double x1 = toDouble(map.getOrDefault("x1", 0));
            double y1 = toDouble(map.getOrDefault("y1", 0));
            double z1 = toDouble(map.getOrDefault("z1", 0));
            double x2 = toDouble(map.getOrDefault("x2", 0));
            double y2 = toDouble(map.getOrDefault("y2", 0));
            double z2 = toDouble(map.getOrDefault("z2", 0));

            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            zones.add(new Zone(world.getName(), Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)));
        }
    }

    private static double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0d;
        }
    }

    public static boolean isInAfkZone(Player player) {
        if (player == null) return false;
        if (zones.isEmpty()) return false;

        for (Zone z : zones) {
            if (z.contains(player)) return true;
        }

        return false;
    }

    private static final class Zone {
        final String world;
        final double minX, minY, minZ, maxX, maxY, maxZ;

        Zone(String world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.world = world;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        boolean contains(Player player) {
            if (!player.getWorld().getName().equals(this.world)) return false;
            double x = player.getLocation().getX();
            double y = player.getLocation().getY();
            double z = player.getLocation().getZ();

            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
    }
}
