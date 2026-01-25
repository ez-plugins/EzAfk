package com.gyvex.ezafk.command;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.gui.AfkPlayerActionsGUI;
import com.gyvex.ezafk.gui.AfkPlayerOverviewGUI;
import com.gyvex.ezafk.integration.TabIntegration;
import com.gyvex.ezafk.manager.AfkTimeManager;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.manager.MessageManager;
import com.gyvex.ezafk.manager.MySQLManager;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.state.AfkStatusDetails;
import com.gyvex.ezafk.state.LastActiveState;
import com.gyvex.ezafk.state.ToggleResult;
import com.gyvex.ezafk.util.CommandUtil;
import com.gyvex.ezafk.util.DurationFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EzAfkCommand implements CommandExecutor {
    private static final int TOP_LEADERBOARD_SIZE = 10;
    private final EzAfk plugin;
    private static final java.util.Map<java.util.UUID, org.bukkit.Location> zonePos1 = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, org.bukkit.Location> zonePos2 = new java.util.HashMap<>();

    public EzAfkCommand(EzAfk plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String lowerLabel = label.toLowerCase(Locale.ROOT);
        if ("afktime".equals(lowerLabel)) {
            handleTime(sender, args, 0);
            return true;
        }

        if ("afktop".equals(lowerLabel)) {
            handleTop(sender);
            return true;
        }

        if ("afkzone".equals(lowerLabel)) {
            // /afkzone add|list|remove|pos1|pos2|reset ... -> map to /afk zone <action> ...
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "zone";
            System.arraycopy(args, 0, newArgs, 1, args.length);
            handleAfkZone(sender, newArgs);
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                MessageManager.sendMessage(sender, "command.toggle.self.only-players", "&cOnly players can toggle their own AFK state.");
                return true;
            }

            Player player = (Player) sender;
            ToggleResult result = AfkState.toggle(this.plugin, player);

            if (result == ToggleResult.FAILED) {
                MessageManager.sendMessage(player, "command.toggle.self.failed", "&cYour AFK status could not be updated.");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                return true;
            case "gui":
                handleGui(sender);
                return true;
            case "toggle":
                handleToggle(sender, args);
                return true;
            case "bypass":
                handleBypass(sender, args);
                return true;
            case "info":
                handleInfo(sender, args);
                return true;
            case "time":
                handleTime(sender, args, 1);
                return true;
            case "top":
                handleTop(sender);
                return true;
            case "zone":
                handleAfkZone(sender, args);
                return true;
            default:
                MessageManager.sendMessage(sender, "command.usage", getUsageFallback());
                return true;
        }
    }

    private void handleAfkZone(CommandSender sender, String[] args) {
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
                org.bukkit.plugin.Plugin wePlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
                if (wePlugin != null) {
                    try {
                        Class<?> weClass = Class.forName("com.sk89q.worldedit.bukkit.WorldEditPlugin");
                        if (weClass.isInstance(wePlugin)) {
                            java.lang.reflect.Method getSelection = weClass.getMethod("getSelection", org.bukkit.entity.Player.class);
                            Object selection = getSelection.invoke(wePlugin, p);
                            if (selection != null) {
                                Class<?> selClass = selection.getClass();
                                java.lang.reflect.Method getMinimumPoint = selClass.getMethod("getMinimumPoint");
                                java.lang.reflect.Method getMaximumPoint = selClass.getMethod("getMaximumPoint");
                                Object minObj = getMinimumPoint.invoke(selection);
                                Object maxObj = getMaximumPoint.invoke(selection);
                                if (minObj instanceof org.bukkit.Location && maxObj instanceof org.bukkit.Location) {
                                    minLoc = (org.bukkit.Location) minObj;
                                    maxLoc = (org.bukkit.Location) maxObj;
                                    usedSelection = true;
                                }
                            }
                        }
                    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {
                        // ignore
                    }
                    if (!usedSelection) {
                        // Check for stored pos1/pos2
                        java.util.UUID pu = p.getUniqueId();
                        org.bukkit.Location p1 = zonePos1.get(pu);
                        org.bukkit.Location p2 = zonePos2.get(pu);
                        if (p1 != null && p2 != null) {
                            minLoc = new org.bukkit.Location(p1.getWorld(), Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
                            maxLoc = new org.bukkit.Location(p1.getWorld(), Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));
                        } else {
                            MessageManager.sendMessage(sender, "afkzone.add.selection-missing", "&cNo WorldEdit selection found - please select a region or provide explicit coordinates, or set pos1/pos2.");
                            return;
                        }
                    }
                } else {
                    // No WorldEdit installed - allow stored pos1/pos2 or require explicit coordinates
                    java.util.UUID pu = p.getUniqueId();
                    org.bukkit.Location p1 = zonePos1.get(pu);
                    org.bukkit.Location p2 = zonePos2.get(pu);
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
                        MessageManager.sendMessage(sender, "afkzone.add.coords-required", "&cWorldEdit not available. Provide coordinates: /afk zone add <name> <x1> <y1> <z1> <x2> <y2> <z2> or set pos1/pos2.");
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
                zonePos1.remove(p.getUniqueId());
                zonePos2.remove(p.getUniqueId());
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
                zonePos1.put(pp1.getUniqueId(), pp1.getLocation());
                MessageManager.sendMessage(sender, "afkzone.add.pos1.set", "&aPosition 1 set.");
                return;
            case "pos2":
                if (!CommandUtil.checkPermission(sender, "ezafk.zone.manage", "command.usage", "&cYou don't have permission.")) return;
                if (!(sender instanceof Player)) {
                    MessageManager.sendMessage(sender, "afkzone.add.player-only", "&cOnly players can set positions.");
                    return;
                }
                Player pp2 = (Player) sender;
                zonePos2.put(pp2.getUniqueId(), pp2.getLocation());
                MessageManager.sendMessage(sender, "afkzone.add.pos2.set", "&aPosition 2 set.");
                return;
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

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("ezafk.reload")) {
            MessageManager.sendMessage(sender, "command.reload.no-permission", "&cYou don't have permission to use this command.");
            return;
        }

        plugin.loadConfig();
        IntegrationManager.unload();
        IntegrationManager.load();
        TabIntegration tabIntegration = (TabIntegration) IntegrationManager.getIntegration("tab");
        if (tabIntegration != null) {
            tabIntegration.reloadFromConfig();
            tabIntegration.update();
        }

        MySQLManager.shutdown();
        MySQLManager.setup();
        AfkPlayerActionsGUI.reloadConfiguredActions();
        MessageManager.sendMessage(sender, "command.reload.success", "&aConfig reloaded.");
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageManager.sendMessage(sender, "command.gui.player-only", "&cOnly players can use this command.");
            return;
        }
        if (!player.hasPermission("ezafk.gui") && !player.isOp()) {
            MessageManager.sendMessage(sender, "command.gui.no-permission", "&cYou don't have permission to use this command.");
            return;
        }
        AfkPlayerOverviewGUI gui = new AfkPlayerOverviewGUI();
        gui.openGUI(player, 1);
    }

    private void handleToggle(CommandSender sender, String[] args) {
        if (!CommandUtil.checkPermission(sender, "ezafk.toggle", "command.toggle.no-permission", "&cYou don't have permission to use this command.")) return;

        if (args.length < 2) {
            MessageManager.sendMessage(sender, "command.toggle.usage", "&cUsage: /afk toggle <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageManager.sendMessage(sender, "command.player-not-found", "&cPlayer not found.");
            return;
        }

        boolean initiatedByTarget = sender instanceof Player && sender.equals(target);
        AfkReason reason = initiatedByTarget ? AfkReason.MANUAL : AfkReason.COMMAND_FORCED;
        // Sanitize sender name for detail
        String senderName = sender.getName().replaceAll("[^a-zA-Z0-9_\\-]", "");
        String detail = initiatedByTarget ? null : "Triggered by " + senderName;

        ToggleResult result = AfkState.toggle(this.plugin, target, initiatedByTarget, reason, detail);

        switch (result) {
            case NOW_AFK:
                MessageManager.sendMessage(sender, "command.toggle.now-afk", "&a%player% is now AFK.", Map.of("player", target.getName()));
                if (initiatedByTarget) {
                    MessageManager.sendMessage(target, "command.toggle.self-now-afk", "&aYou are now AFK.");
                }
                break;
            case NO_LONGER_AFK:
                MessageManager.sendMessage(sender, "command.toggle.no-longer-afk", "&a%player% is no longer AFK.", Map.of("player", target.getName()));
                if (initiatedByTarget) {
                    MessageManager.sendMessage(target, "command.toggle.self-no-longer-afk", "&aYou are no longer AFK.");
                }
                break;
            case FAILED:
                MessageManager.sendMessage(sender, "command.toggle.failed", "&cFailed to toggle AFK status for %player%.", Map.of("player", target.getName()));
                if (initiatedByTarget) {
                    MessageManager.sendMessage(target, "command.toggle.self.failed", "&cYour AFK status could not be updated.");
                }
                break;
        }
    }

    private void handleBypass(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezafk.bypass.manage")) {
            MessageManager.sendMessage(sender, "command.bypass.no-permission", "&cYou don't have permission to change AFK bypass states.");
            return;
        }

        if (args.length < 2) {
            MessageManager.sendMessage(sender, "command.usage", getUsageFallback());
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageManager.sendMessage(sender, "command.player-not-found", "&cPlayer not found.");
            return;
        }

        boolean bypassEnabled = AfkState.toggleBypass(target.getUniqueId());
        if (bypassEnabled) {
            MessageManager.sendMessage(sender, "command.bypass.enabled", "&a%target% will now bypass AFK detection.", Map.of("target", target.getName()));
            if (sender != target) {
                MessageManager.sendMessage(target, "command.bypass.enabled-target", "&aYou will now bypass AFK detection.");
            }
        } else {
            MessageManager.sendMessage(sender, "command.bypass.disabled", "&a%target% will no longer bypass AFK detection.", Map.of("target", target.getName()));
            if (sender != target) {
                MessageManager.sendMessage(target, "command.bypass.disabled-target", "&cYou will no longer bypass AFK detection.");
            }
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!CommandUtil.checkPermission(sender, "ezafk.info", "command.info.no-permission", "&cYou don't have permission to use this command.")) return;

        if (args.length < 2) {
            MessageManager.sendMessage(sender, "command.info.usage", "&cUsage: /afk info <player>");
            return;
        }

        String nameArg = args[1];
        OfflinePlayer target = CommandUtil.findPlayer(nameArg);
        if (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline()) {
            MessageManager.sendMessage(sender, "command.player-not-found", "&cPlayer not found.");
            return;
        }

        UUID targetId = target.getUniqueId();
        if (!AfkState.isAfk(targetId)) {
            MessageManager.sendMessage(sender, "command.info.not-afk", "&a%player% is not currently AFK.", Map.of("player", target.getName() != null ? target.getName() : targetId.toString()));
            return;
        }

        String targetName = target.getName() != null ? target.getName() : targetId.toString();
        AfkStatusDetails details = AfkState.getAfkStatusDetails(targetId);
        String reasonText = details != null ? details.getReasonDisplayName() : AfkReason.OTHER.getDisplayName();
        long afkSeconds = AfkState.getSecondsSinceAfk(targetId);
        long lastActivity = LastActiveState.getSecondsSinceLastActive(targetId);

        MessageManager.sendMessage(sender, "command.info.header", "&6AFK report for &e%player%&6:", Map.of("player", targetName));
        MessageManager.sendMessage(sender, "command.info.reason", "&7Reason: &f%reason%", Map.of("player", targetName, "reason", reasonText));

        if (details != null && details.hasDetail()) {
            MessageManager.sendMessage(sender, "command.info.detail", "&7Detail: &f%detail%", Map.of("player", targetName, "detail", details.getDetail()));
        } else {
            MessageManager.sendMessage(sender, "command.info.no-detail", "&7No additional details.", Map.of("player", targetName));
        }

        MessageManager.sendMessage(sender, "command.info.duration", "&7AFK for: &f%duration%", Map.of("player", targetName, "duration", DurationFormatter.formatDuration(afkSeconds)));
        MessageManager.sendMessage(sender, "command.info.last-activity", "&7Last activity: &f%last% ago", Map.of("player", targetName, "last", DurationFormatter.formatDuration(lastActivity)));
    }

    private void handleTime(CommandSender sender, String[] args, int startIndex) {
        int remainingArguments = Math.max(0, args.length - startIndex);

        if (remainingArguments > 1) {
            MessageManager.sendMessage(sender, "command.usage", getUsageFallback());
            return;
        }

        if (remainingArguments == 0) {
            if (!(sender instanceof Player)) {
                MessageManager.sendMessage(sender, "command.time.players-only", "&cOnly players can view their own AFK time.");
                return;
            }

            if (!sender.hasPermission("ezafk.time")) {
                MessageManager.sendMessage(sender, "command.time.no-permission", "&cYou don't have permission to view your AFK time.");
                return;
            }

            Player player = (Player) sender;
            long totalSeconds = AfkState.getTotalAfkSeconds(player.getUniqueId());
            MessageManager.sendMessage(sender, "command.time.self", "&aYou have been AFK for %duration% in total.",
                    Map.of("duration", DurationFormatter.formatDuration(totalSeconds)));
            return;
        }

        if (!sender.hasPermission("ezafk.time.others")) {
            MessageManager.sendMessage(sender, "command.time.others-no-permission", "&cYou don't have permission to view other players' AFK time.");
            return;
        }

        String targetNameArg = args[startIndex];
        Player onlineTarget = Bukkit.getPlayer(targetNameArg);
        OfflinePlayer target;
        if (onlineTarget != null) {
            target = onlineTarget;
        } else {
            // Fallback: unavoidable for legacy, but safe
            target = Bukkit.getOfflinePlayer(targetNameArg);
        }
        if (target == null || (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline())) {
            MessageManager.sendMessage(sender, "command.player-not-found", "&cPlayer not found.");
            return;
        }

        UUID targetId = target.getUniqueId();
        long totalSeconds = AfkState.getTotalAfkSeconds(targetId);
        String targetName = target.getName() != null ? target.getName() : targetId.toString();
        MessageManager.sendMessage(sender, "command.time.other", "&a%player% has been AFK for %duration% in total.",
                Map.of("player", targetName, "duration", DurationFormatter.formatDuration(totalSeconds)));
    }

    private void handleTop(CommandSender sender) {
        if (!CommandUtil.checkPermission(sender, "ezafk.top", "command.top.no-permission", "&cYou don't have permission to use this command.")) return;

        List<Map.Entry<UUID, Long>> topEntries = AfkTimeManager.getTopPlayers(TOP_LEADERBOARD_SIZE);
        if (topEntries.isEmpty()) {
            MessageManager.sendMessage(sender, "command.top.empty", "&cNo AFK data available.");
            return;
        }

        MessageManager.sendMessage(sender, "command.top.header", "&6Top AFK players:");

        int position = 1;
        for (Map.Entry<UUID, Long> entry : topEntries) {
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            String name = playerName != null ? playerName : entry.getKey().toString();
            String duration = DurationFormatter.formatDuration(entry.getValue());
            MessageManager.sendMessage(sender, "command.top.entry", "&e#%pos% &f%player%: &b%duration%", Map.of("pos", String.valueOf(position), "player", name, "duration", duration));
            position++;
        }
    }

    private String getUsageFallback() {
        return "&cUsage: /afk [reload|gui|toggle <player>|bypass <player>|info <player>|time [player]|top|zone <list|add|remove>]";
    }

}
