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
    private final EzAfk plugin;

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
        if (!(sender instanceof Player)) {
            MessageManager.sendMessage(sender, "command.gui.players-only", "&cOnly players can use the GUI.");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("ezafk.gui") && !player.isOp()) {
            MessageManager.sendMessage(player, "command.gui.no-permission", "&cYou don't have permission to use this command.");
            return;
        }

        AfkPlayerOverviewGUI gui = new AfkPlayerOverviewGUI();
        gui.openGUI(player, 1);
    }

    private void handleToggle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezafk.toggle")) {
            MessageManager.sendMessage(sender, "command.toggle.no-permission", "&cYou don't have permission to toggle other players.");
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

        boolean initiatedByTarget = sender instanceof Player && sender.equals(target);
        AfkReason reason = initiatedByTarget ? AfkReason.MANUAL : AfkReason.COMMAND_FORCED;
        String detail = initiatedByTarget ? null : "Triggered by " + sender.getName();

        ToggleResult result = AfkState.toggle(this.plugin, target, initiatedByTarget, reason, detail);

        switch (result) {
            case NOW_AFK:
                MessageManager.sendMessage(sender, "command.toggle.now-afk", "&a%target% is now AFK.", Map.of("target", target.getName()));
                break;
            case NO_LONGER_AFK:
                MessageManager.sendMessage(sender, "command.toggle.no-longer-afk", "&a%target% is no longer AFK.", Map.of("target", target.getName()));
                break;
            case FAILED:
                MessageManager.sendMessage(sender, "command.toggle.failed", "&c%target%'s AFK status could not be changed.", Map.of("target", target.getName()));
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
        if (!sender.hasPermission("ezafk.info")) {
            MessageManager.sendMessage(sender, "command.info.no-permission", "&cYou don't have permission to view AFK reports.");
            return;
        }

        if (args.length < 2) {
            MessageManager.sendMessage(sender, "command.usage", getUsageFallback());
            return;
        }

        String nameArg = args[1];
        Player onlineTarget = Bukkit.getPlayer(nameArg);
        OfflinePlayer target;
        if (onlineTarget != null) {
            target = onlineTarget;
        } else {
            // Fallback: unavoidable for legacy, but safe
            target = Bukkit.getOfflinePlayer(nameArg);
        }
        if (target == null || (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline())) {
            MessageManager.sendMessage(sender, "command.player-not-found", "&cPlayer not found.");
            return;
        }

        UUID targetId = target.getUniqueId();
        if (!AfkState.isAfk(targetId)) {
            String notAfkName = target.getName() != null ? target.getName() : targetId.toString();
            MessageManager.sendMessage(sender, "command.info.not-afk", "&e%target% is not currently marked as AFK.",
                    Map.of("target", notAfkName));
            return;
        }

        String targetName = target.getName() != null ? target.getName() : targetId.toString();
        AfkStatusDetails details = AfkState.getAfkStatusDetails(targetId);
        String reasonText = details != null ? details.getReasonDisplayName() : AfkReason.OTHER.getDisplayName();
        long afkSeconds = AfkState.getSecondsSinceAfk(targetId);
        long lastActivity = LastActiveState.getSecondsSinceLastActive(targetId);

        MessageManager.sendMessage(sender, "command.info.header", "&6AFK report for &e%player%&6:",
                Map.of("player", targetName));
        MessageManager.sendMessage(sender, "command.info.reason", "&7Reason: &f%reason%",
                Map.of("player", targetName, "reason", reasonText));

        if (details != null && details.hasDetail()) {
            MessageManager.sendMessage(sender, "command.info.detail", "&7Details: &f%detail%",
                    Map.of("player", targetName, "detail", details.detail()));
        } else {
            MessageManager.sendMessage(sender, "command.info.no-detail", "&7Details: &fNo additional details were recorded.",
                    Map.of("player", targetName));
        }

        MessageManager.sendMessage(sender, "command.info.duration", "&7AFK for: &f%duration%",
                Map.of("player", targetName, "duration", DurationFormatter.formatDuration(afkSeconds)));
        MessageManager.sendMessage(sender, "command.info.last-activity", "&7Last activity: &f%last% ago",
                Map.of("player", targetName, "last", DurationFormatter.formatDuration(lastActivity)));
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
        if (!sender.hasPermission("ezafk.top")) {
            MessageManager.sendMessage(sender, "command.top.no-permission", "&cYou don't have permission to view the AFK leaderboard.");
            return;
        }

        List<Map.Entry<UUID, Long>> topEntries = AfkTimeManager.getTopPlayers(10);
        if (topEntries.isEmpty()) {
            MessageManager.sendMessage(sender, "command.top.empty", "&eNo AFK time has been recorded yet.");
            return;
        }

        MessageManager.sendMessage(sender, "command.top.header", "&6Top AFK players:");

        int position = 1;
        for (Map.Entry<UUID, Long> entry : topEntries) {
            UUID playerId = entry.getKey();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            String playerName = offlinePlayer != null && offlinePlayer.getName() != null
                    ? offlinePlayer.getName()
                    : playerId.toString();
            String duration = DurationFormatter.formatDuration(entry.getValue());

            MessageManager.sendMessage(sender, "command.top.entry", "&e#%position% &7%player% - &f%duration%",
                    Map.of("position", String.valueOf(position), "player", playerName, "duration", duration));
            position++;
        }
    }

    private String getUsageFallback() {
        return "&cUsage: /afk [reload|gui|toggle <player>|bypass <player>|info <player>|time [player]|top]";
    }
}
