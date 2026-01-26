package com.gyvex.ezafk.command;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.manager.MessageManager;
import com.gyvex.ezafk.util.CommandUtil;
import org.bukkit.Bukkit;
import com.gyvex.ezafk.zone.ZoneCache;
import com.gyvex.ezafk.integration.WorldEditIntegration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AfkZoneCommand {
    private final EzAfk plugin;
    // zone position cache moved to ZoneCache

    public AfkZoneCommand(EzAfk plugin) {
        this.plugin = plugin;
    }

    // helper methods available on ZoneCache

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

    public void handleAfkZone(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageManager.sendMessage(sender, "command.usage", "&cUsage: /afkzone <list|add|remove> [name]");
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);

        switch (action) {
            case "list":
                if (!CommandUtil.checkPermission(sender, "ezafk.zone.list", "command.usage", "&cYou don't have permission.")) return;
                List<?> rawList = plugin.getZonesConfig().getMapList("regions");
                if (rawList == null || rawList.isEmpty()) {
                    MessageManager.sendMessage(sender, "afkzone.list.empty", "&eNo AFK zones configured.");
                    return;
                }
                MessageManager.sendMessage(sender, "afkzone.list.header", "&6AFK Zones:");
                for (Object o : rawList) {
                    if (!(o instanceof java.util.Map)) continue;
                    @SuppressWarnings("unchecked")
                    java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) o;
                    String name = String.valueOf(map.getOrDefault("name", "<unnamed>"));
                    String world = String.valueOf(map.getOrDefault("world", ""));
                    String coords = String.format("%s,%s,%s - %s,%s,%s",
                            map.getOrDefault("x1", ""), map.getOrDefault("y1", ""), map.getOrDefault("z1", ""),
                            map.getOrDefault("x2", ""), map.getOrDefault("y2", ""), map.getOrDefault("z2", "")
                    );
                    MessageManager.sendMessage(sender, "afkzone.list.entry", "&e%name% &7(%world%): &f%coords%", Map.of("name", name, "world", world, "coords", coords));
                }
                return;
            case "add":
                if (!CommandUtil.checkPermission(sender, "ezafk.zone.manage", "command.usage", "&cYou don't have permission.")) return;
                if (!(sender instanceof Player)) {
                    MessageManager.sendMessage(sender, "afkzone.add.player-only", "&cOnly players can create AFK zones.");
                    return;
                }
                if (args.length < 3) {
                    MessageManager.sendMessage(sender, "command.usage", "&cUsage: /afkzone add <name>");
                    return;
                }
                String name = args[2];
                Player p = (Player) sender;
                java.util.Map<String, Object> region = new java.util.HashMap<>();
                region.put("name", name);

                // Attempt to use WorldEdit region selection if available, else fall back to stored pos1/pos2, else require explicit coordinates
                org.bukkit.Location minLoc = null;
                org.bukkit.Location maxLoc = null;
                boolean usedSelection = false;
                org.bukkit.Location[] sel = WorldEditIntegration.getSelectionLocations(plugin, p);
                if (sel != null && sel.length == 2) {
                    minLoc = sel[0];
                    maxLoc = sel[1];
                    usedSelection = true;
                }
                if (!usedSelection) {
                    // Check for stored pos1/pos2
                    java.util.UUID pu = p.getUniqueId();
                    org.bukkit.Location p1 = ZoneCache.zonePos1.get(pu);
                    org.bukkit.Location p2 = ZoneCache.zonePos2.get(pu);
                    if (p1 != null && p2 != null) {
                        minLoc = new org.bukkit.Location(p1.getWorld(), Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
                        maxLoc = new org.bukkit.Location(p1.getWorld(), Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));
                    } else if (args.length >= 9) {
                        try {
                            double ax1 = Double.parseDouble(args[3]);
                            double ay1 = Double.parseDouble(args[4]);
                            double az1 = Double.parseDouble(args[5]);
                            double ax2 = Double.parseDouble(args[6]);
                            double ay2 = Double.parseDouble(args[7]);
                            double az2 = Double.parseDouble(args[8]);
                            minLoc = new org.bukkit.Location(p.getWorld(), Math.min(ax1, ax2), Math.min(ay1, ay2), Math.min(az1, az2));
                            maxLoc = new org.bukkit.Location(p.getWorld(), Math.max(ax1, ax2), Math.max(ay1, ay2), Math.max(az1, az2));
                        } catch (NumberFormatException ex) {
                            MessageManager.sendMessage(sender, "afkzone.add.coords-invalid", "&cInvalid coordinates provided. Use numbers.");
                            return;
                        }
                    } else {
                        MessageManager.sendMessage(sender, "afkzone.add.selection-missing", "&cNo WorldEdit selection found - please select a region or provide explicit coordinates, or set pos1/pos2.");
                        return;
                    }
                }

                region.put("world", minLoc.getWorld().getName());
                region.put("x1", minLoc.getX());
                region.put("y1", minLoc.getY());
                region.put("z1", minLoc.getZ());
                region.put("x2", maxLoc.getX());
                region.put("y2", maxLoc.getY());
                region.put("z2", maxLoc.getZ());

                List<?> rawRegions = plugin.getZonesConfig().getMapList("regions");
                java.util.List<java.util.Map<String, Object>> regions;
                if (rawRegions == null) {
                    regions = new java.util.ArrayList<>();
                } else {
                    regions = new java.util.ArrayList<>();
                    for (Object o : rawRegions) {
                        if (!(o instanceof java.util.Map)) continue;
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> m = (java.util.Map<String, Object>) o;
                        regions.add(m);
                    }
                }
                regions.add(region);
                plugin.getZonesConfig().set("regions", regions);
                plugin.getZonesConfig().set("enabled", true);
                plugin.saveZonesConfig();
                plugin.reloadZonesConfig();
                com.gyvex.ezafk.manager.AfkZoneManager.load(plugin);
                // clear stored pos1/pos2 for this player if present
                ZoneCache.zonePos1.remove(p.getUniqueId());
                ZoneCache.zonePos2.remove(p.getUniqueId());
                MessageManager.sendMessage(sender, "afkzone.add.success", "&aAFK zone %name% added at your location.", Map.of("name", name));
                return;
            case "remove":
                if (!CommandUtil.checkPermission(sender, "ezafk.zone.manage", "command.usage", "&cYou don't have permission.")) return;
                if (args.length < 3) {
                    MessageManager.sendMessage(sender, "command.usage", "&cUsage: /afkzone remove <name>");
                    return;
                }
                String removeName = args[2];
                List<?> rawCurrent = plugin.getZonesConfig().getMapList("regions");
                java.util.List<java.util.Map<String, Object>> current = new java.util.ArrayList<>();
                if (rawCurrent != null) {
                    for (Object o : rawCurrent) {
                        if (!(o instanceof java.util.Map)) continue;
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> m = (java.util.Map<String, Object>) o;
                        current.add(m);
                    }
                }
                boolean removed = false;
                java.util.Iterator<java.util.Map<String, Object>> it = current.iterator();
                while (it.hasNext()) {
                    java.util.Map<String, Object> m = it.next();
                    Object n = m.get("name");
                    if (n != null && removeName.equalsIgnoreCase(n.toString())) {
                        it.remove();
                        removed = true;
                        break;
                    }
                }
                if (!removed) {
                    MessageManager.sendMessage(sender, "afkzone.remove.notfound", "&cAFK zone '%name%' not found.", Map.of("name", removeName));
                    return;
                }
                plugin.getZonesConfig().set("regions", current);
                plugin.saveZonesConfig();
                plugin.reloadZonesConfig();
                com.gyvex.ezafk.manager.AfkZoneManager.load(plugin);
                MessageManager.sendMessage(sender, "afkzone.remove.success", "&aAFK zone '%name%' removed.", Map.of("name", removeName));
                return;
            case "pos1":
                if (!CommandUtil.checkPermission(sender, "ezafk.zone.manage", "command.usage", "&cYou don't have permission.")) return;
                if (!(sender instanceof Player)) {
                    MessageManager.sendMessage(sender, "afkzone.add.player-only", "&cOnly players can set positions.");
                    return;
                }
                Player pp1 = (Player) sender;
                ZoneCache.zonePos1.put(pp1.getUniqueId(), pp1.getLocation());
                ZoneCache.zonePos1Time.put(pp1.getUniqueId(), System.currentTimeMillis());
                MessageManager.sendMessage(sender, "afkzone.add.pos1.set", "&aPosition 1 set.");
                return;
            case "pos2":
                if (!CommandUtil.checkPermission(sender, "ezafk.zone.manage", "command.usage", "&cYou don't have permission.")) return;
                if (!(sender instanceof Player)) {
                    MessageManager.sendMessage(sender, "afkzone.add.player-only", "&cOnly players can set positions.");
                    return;
                }
                Player pp2 = (Player) sender;
                ZoneCache.zonePos2.put(pp2.getUniqueId(), pp2.getLocation());
                ZoneCache.zonePos2Time.put(pp2.getUniqueId(), System.currentTimeMillis());
                MessageManager.sendMessage(sender, "afkzone.add.pos2.set", "&aPosition 2 set.");
                return;
            case "clearpos":
                // /afk zone clearpos [player]
                if (args.length >= 3) {
                    if (!CommandUtil.checkPermission(sender, "ezafk.zone.manage", "command.usage", "&cYou don't have permission.")) return;
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        MessageManager.sendMessage(sender, "command.player-not-found", "&cPlayer not found.");
                        return;
                    }
                    ZoneCache.clearPositions(target.getUniqueId());
                    MessageManager.sendMessage(sender, "afkzone.add.pos-cleared-target", "&aCleared stored positions for %player%.", Map.of("player", target.getName()));
                    return;
                } else {
                    if (!(sender instanceof Player)) {
                        MessageManager.sendMessage(sender, "afkzone.add.player-only", "&cOnly players can clear their own stored positions.");
                        return;
                    }
                    Player self = (Player) sender;
                    ZoneCache.clearPositions(self.getUniqueId());
                    MessageManager.sendMessage(sender, "afkzone.add.pos-cleared-self", "&aCleared your stored positions.");
                    return;
                }
            case "reset":
                if (!CommandUtil.checkPermission(sender, "ezafk.zone.manage", "command.usage", "&cYou don't have permission.")) return;
                if (args.length < 3) {
                    MessageManager.sendMessage(sender, "command.usage", "&cUsage: /afkzone reset <player> [zone]");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    MessageManager.sendMessage(sender, "command.player-not-found", "&cPlayer not found.");
                    return;
                }
                if (args.length >= 4) {
                    String zoneName = args[3];
                    com.gyvex.ezafk.manager.AfkZoneRewardManager.resetPlayerCountForZone(target.getUniqueId(), zoneName);
                    MessageManager.sendMessage(sender, "afkzone.reset.zone.success", "&aReset reward counts for %player% in zone %zone%.", Map.of("player", target.getName(), "zone", zoneName));
                } else {
                    com.gyvex.ezafk.manager.AfkZoneRewardManager.resetPlayerCounts(target.getUniqueId());
                    MessageManager.sendMessage(sender, "afkzone.reset.success", "&aReset reward counts for %player%.", Map.of("player", target.getName()));
                }
                return;
            default:
                MessageManager.sendMessage(sender, "command.usage", "&cUsage: /afkzone <list|add|remove> [name]");
                return;
        }
    }
}
