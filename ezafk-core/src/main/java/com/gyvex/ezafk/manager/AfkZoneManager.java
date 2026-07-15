package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import com.gyvex.ezafk.zone.Zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AfkZoneManager {
    private static final List<Zone> zones = new ArrayList<>();
    private AfkZoneManager() {}

    public static void load(EzAfk plugin) {
        zones.clear();
        if (plugin == null) return;

        FileConfiguration zonesConfig = Registry.get().getZonesConfig();
        if (zonesConfig == null) return;

        if (!zonesConfig.getBoolean("enabled", false)) return;

        // Read global defaults from the top-level "defaults.reward" and
        // "defaults.notification" sections so zone authors can opt-in to
        // sensible behaviour without repeating config per zone.
        Map<?, ?> defaultsRewardMap = null;
        Map<?, ?> defaultsNotifMap = null;
        Object defaultsObj = zonesConfig.get("defaults");
        if (defaultsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> defaultsMap = (Map<String, Object>) defaultsObj;
            Object dr = defaultsMap.get("reward");
            if (dr instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<?, ?> drm = (Map<?, ?>) dr;
                defaultsRewardMap = drm;
                Object dn = drm.get("notification");
                if (dn instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<?, ?> dnm = (Map<?, ?>) dn;
                    defaultsNotifMap = dnm;
                }
            }
        }

        // Default notification values (can be overridden by defaults.reward.notification or per-zone)
        boolean defaultNotifEnabled = true;
        List<String> defaultNotifDisplays = new ArrayList<>();
        defaultNotifDisplays.add("ACTION_BAR");
        String defaultNotifMessage = "&7Next reward in &e{seconds}s &7in &a%zone%";
        int defaultNotifDuration = 0; // 0 = use zone interval

        if (defaultsNotifMap != null) {
            Object en = defaultsNotifMap.get("enabled");
            if (en != null) defaultNotifEnabled = Boolean.parseBoolean(String.valueOf(en));
            Object disp = defaultsNotifMap.get("displays");
            if (disp instanceof List) {
                defaultNotifDisplays = new ArrayList<>();
                for (Object d : (List<?>) disp) defaultNotifDisplays.add(String.valueOf(d).toUpperCase());
            }
            Object msg = defaultsNotifMap.get("message");
            if (msg != null) defaultNotifMessage = String.valueOf(msg);
            Object dur = defaultsNotifMap.get("duration");
            if (dur != null) defaultNotifDuration = (int) toDouble(dur);
        }

        // Default reward values from "defaults.reward"
        boolean defaultRewardEnabled = false;
        long defaultRewardInterval = 300L;
        double defaultRewardAmount = 1.0;
        String defaultRewardType = "economy";

        if (defaultsRewardMap != null) {
            Object en = defaultsRewardMap.get("enabled");
            if (en != null) defaultRewardEnabled = Boolean.parseBoolean(String.valueOf(en));
            Object iv = defaultsRewardMap.get("interval-seconds");
            if (iv != null) defaultRewardInterval = (long) toDouble(iv);
            Object am = defaultsRewardMap.get("amount");
            if (am != null) defaultRewardAmount = toDouble(am);
            Object ty = defaultsRewardMap.get("type");
            if (ty != null) defaultRewardType = String.valueOf(ty);
        }

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

            boolean rewardEnabled = defaultRewardEnabled;
            long rewardInterval = defaultRewardInterval;
            int rewardMaxStack = 0;
            double rewardAmount = defaultRewardAmount;
            String rewardType = defaultRewardType;
            String rewardCommand = null;
            String rewardItemMaterial = null;
            int rewardItemAmount = 1;
            int rewardLimit = 0;
            long rewardLimitCooldown = 0L;

            boolean notifEnabled = defaultNotifEnabled;
            List<String> notifDisplays = new ArrayList<>(defaultNotifDisplays);
            String notifMessage = defaultNotifMessage;
            int notifDuration = defaultNotifDuration;

            if (map.containsKey("reward") && map.get("reward") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rewardMap = (Map<String, Object>) map.get("reward");
                rewardEnabled = Boolean.parseBoolean(String.valueOf(rewardMap.getOrDefault("enabled", rewardEnabled)));
                rewardInterval = (long) toDouble(rewardMap.getOrDefault("interval-seconds", rewardInterval));
                Object maxStackObj = rewardMap.getOrDefault("max-stack", 0);
                if (maxStackObj instanceof Number) {
                    rewardMaxStack = ((Number) maxStackObj).intValue();
                } else {
                    try { rewardMaxStack = Integer.parseInt(String.valueOf(maxStackObj)); } catch (Exception ignored) {}
                }
                rewardAmount = toDouble(rewardMap.getOrDefault("amount", rewardAmount));
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

                // Per-zone notification overrides
                if (rewardMap.containsKey("notification") && rewardMap.get("notification") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> notifMap = (Map<String, Object>) rewardMap.get("notification");
                    Object en = notifMap.get("enabled");
                    if (en != null) notifEnabled = Boolean.parseBoolean(String.valueOf(en));
                    Object disp = notifMap.get("displays");
                    if (disp instanceof List) {
                        notifDisplays = new ArrayList<>();
                        for (Object d : (List<?>) disp) notifDisplays.add(String.valueOf(d).toUpperCase());
                    }
                    Object msg = notifMap.get("message");
                    if (msg != null) notifMessage = String.valueOf(msg);
                    Object dur = notifMap.get("duration");
                    if (dur != null) notifDuration = (int) toDouble(dur);
                }
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            zones.add(new Zone(
                    name, world.getName(),
                    Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2),
                    rewardEnabled, rewardInterval, rewardMaxStack, rewardAmount,
                    rewardType, rewardCommand, rewardItemMaterial, rewardItemAmount,
                    rewardLimit, rewardLimitCooldown,
                    notifEnabled, notifDisplays, notifMessage, notifDuration
            ));
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

    /**
     * Returns a copy of the currently loaded zones.
     */
    public static java.util.List<Zone> getZones() {
        return new java.util.ArrayList<>(zones);
    }

    // Zone class moved to com.gyvex.ezafk.manager.zone.Zone
}
