package com.gyvex.ezafk.command;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.listener.AfkPlayerActionsGUI;
import com.gyvex.ezafk.listener.AfkPlayerOverviewGUI;
import com.gyvex.ezafk.integration.TabIntegration;
import com.gyvex.ezafk.manager.AfkTimeManager;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.manager.MessageManager;
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
    private final AfkZoneCommand afkZoneCommand;

    // Zone position storage and helpers moved to AfkZoneCommand

    public EzAfkCommand(EzAfk plugin) {
        this.plugin = plugin;
        this.afkZoneCommand = new AfkZoneCommand(plugin);
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
            afkZoneCommand.handleAfkZone(sender, newArgs);
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
                afkZoneCommand.handleAfkZone(sender, args);
                return true;
            default:
                MessageManager.sendMessage(sender, "command.usage", getUsageFallback());
                return true;
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("ezafk.reload")) {
            MessageManager.sendMessage(sender, "command.reload.no-permission", "&cYou don't have permission to use this command.");
            return;
        }

        Registry.get().getConfigManager().loadConfig();
        IntegrationManager.unload();
        IntegrationManager.load();
        TabIntegration tabIntegration = (TabIntegration) IntegrationManager.getIntegration("tab");
        if (tabIntegration != null) {
            tabIntegration.reloadFromConfig();
            tabIntegration.update();
        }
        // Reload storage repository according to new configuration
        try {
            Registry.get().reloadStorageRepository();
        } catch (Exception ignored) {}
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

        // Support: /afk time reset <player>
        if (remainingArguments >= 2 && args[startIndex].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("ezafk.time.reset")) {
                MessageManager.sendMessage(sender, "command.time.reset.no-permission", "&cYou don't have permission to reset player AFK time.");
                return;
            }

            String targetName = args[startIndex + 1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline())) {
                MessageManager.sendMessage(sender, "command.player-not-found", "&cPlayer not found.");
                return;
            }

            boolean ok = AfkTimeManager.resetPlayer(target.getUniqueId());
            if (ok) {
                MessageManager.sendMessage(sender, "command.time.reset.success", "&aReset AFK time for %player%.", Map.of("player", target.getName() != null ? target.getName() : target.getUniqueId().toString()));
            } else {
                MessageManager.sendMessage(sender, "command.time.reset.failed", "&cFailed to reset AFK time for %player%.", Map.of("player", target.getName() != null ? target.getName() : target.getUniqueId().toString()));
            }
            return;
        }

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
