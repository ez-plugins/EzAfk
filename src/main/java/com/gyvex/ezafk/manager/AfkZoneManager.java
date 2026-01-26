package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Map;
import org.bukkit.entity.Player;
import com.gyvex.ezafk.zone.Zone;

import java.util.ArrayList;
import java.util.List;

public final class AfkZoneManager {
    private static final List<Zone> zones = new ArrayList<>();
    private AfkZoneManager() {}

    public static void load(EzAfk plugin) {
        zones.clear();
        if (plugin == null) return;

        FileConfiguration zonesConfig = plugin.getZonesConfig();
        if (zonesConfig == null) return;

        if (!zonesConfig.getBoolean("enabled", false)) return;

        List<?> list = zonesConfig.getList("regions");
        if (list == null) return;

        for (Object o : list) {
            if (!(o instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) o;
            String name = (String) map.getOrDefault("name", "");
            String worldName = (String) map.getOrDefault("world", "world");
            double x1 = toDouble(map.getOrDefault("x1", 0));
            double y1 = toDouble(map.getOrDefault("y1", 0));
            double z1 = toDouble(map.getOrDefault("z1", 0));
            double x2 = toDouble(map.getOrDefault("x2", 0));
            double y2 = toDouble(map.getOrDefault("y2", 0));
            double z2 = toDouble(map.getOrDefault("z2", 0));

            boolean rewardEnabled = false;
            long rewardInterval = 0L;
            int rewardMaxStack = 0;
            double rewardAmount = 0.0;
            String rewardType = "economy";
            String rewardCommand = null;
            String rewardItemMaterial = null;
            int rewardItemAmount = 1;
            int rewardLimit = 0;
            long rewardLimitCooldown = 0L;

            if (map.containsKey("reward") && map.get("reward") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rewardMap = (Map<String, Object>) map.get("reward");
                rewardEnabled = Boolean.parseBoolean(String.valueOf(rewardMap.getOrDefault("enabled", false)));
                rewardInterval = (long) toDouble(rewardMap.getOrDefault("interval-seconds", 0));
                Object maxStackObj = rewardMap.getOrDefault("max-stack", 0);
                if (maxStackObj instanceof Number) {
                    rewardMaxStack = ((Number) maxStackObj).intValue();
                } else {
                    try { rewardMaxStack = Integer.parseInt(String.valueOf(maxStackObj)); } catch (Exception ignored) {}
                }
                rewardAmount = toDouble(rewardMap.getOrDefault("amount", 0.0));
                rewardType = String.valueOf(rewardMap.getOrDefault("type", rewardType));
                rewardCommand = rewardMap.getOrDefault("command", null) != null ? String.valueOf(rewardMap.get("command")) : null;

                if (rewardMap.containsKey("item") && rewardMap.get("item") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) rewardMap.get("item");
                    rewardItemMaterial = String.valueOf(itemMap.getOrDefault("material", ""));
                    Object amt = itemMap.getOrDefault("amount", 1);
                    if (amt instanceof Number) rewardItemAmount = ((Number) amt).intValue();
                    else try { rewardItemAmount = Integer.parseInt(String.valueOf(amt)); } catch (Exception ignored) {}
                }
                Object limitObj = rewardMap.getOrDefault("limit", 0);
                if (limitObj instanceof Number) rewardLimit = ((Number) limitObj).intValue();
                else try { rewardLimit = Integer.parseInt(String.valueOf(limitObj)); } catch (Exception ignored) {}

                rewardLimitCooldown = (long) toDouble(rewardMap.getOrDefault("limit-cooldown-seconds", 0));
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            zones.add(new Zone(name, world.getName(), Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), rewardEnabled, rewardInterval, rewardMaxStack, rewardAmount, rewardType, rewardCommand, rewardItemMaterial, rewardItemAmount, rewardLimit, rewardLimitCooldown));
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

    public static Zone getZoneForPlayer(Player player) {
        if (player == null || zones.isEmpty()) return null;

        for (Zone z : zones) {
            if (z.contains(player)) return z;
        }

        return null;
    }

    // Zone class moved to com.gyvex.ezafk.manager.zone.Zone
}
