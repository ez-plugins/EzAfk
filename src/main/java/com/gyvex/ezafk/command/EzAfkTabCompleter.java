package com.gyvex.ezafk.command;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EzAfkTabCompleter implements TabCompleter {
        private static final List<String> MAIN_COMMANDS = Arrays.asList(
            "reload", "gui", "toggle", "bypass", "info", "time", "top", "zone"
        );

        private static final List<String> ZONE_COMMANDS = Arrays.asList(
            "list", "add", "remove", "pos1", "pos2", "reset"
        );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            String labelLower = alias.toLowerCase(Locale.ROOT);
            if ("afkzone".equals(labelLower)) {
                return ZONE_COMMANDS.stream().filter(cmd -> cmd.startsWith(prefix)).collect(Collectors.toList());
            }
            return MAIN_COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String prefix = args[1].toLowerCase(Locale.ROOT);
            // If user used /afkzone <tab>
            if ("afkzone".equals(alias.toLowerCase(Locale.ROOT))) {
                sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
            }

            if (sub.equals("toggle") || sub.equals("bypass") || sub.equals("info") || (sub.equals("time") && sender.hasPermission("ezafk.time.others"))) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .collect(Collectors.toList());
                Collections.sort(playerNames, String.CASE_INSENSITIVE_ORDER);
                return playerNames;
            }

            if (sub.equals("reset")) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .collect(Collectors.toList());
                Collections.sort(playerNames, String.CASE_INSENSITIVE_ORDER);
                return playerNames;
            }

            if (sub.equals("remove")) {
                // suggest zone names from zones.yml
                try {
                    EzAfk plugin = EzAfk.getInstance();
                    if (plugin != null) {
                        java.util.List<?> raw = plugin.getZonesConfig().getMapList("regions");
                        if (raw != null) {
                            List<String> zones = new java.util.ArrayList<>();
                            for (Object o : raw) {
                                if (!(o instanceof java.util.Map)) continue;
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> m = (java.util.Map<String, Object>) o;
                                String name = String.valueOf(m.getOrDefault("name", ""));
                                if (!name.isEmpty() && name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                                    zones.add(name);
                                }
                            }
                            java.util.Collections.sort(zones, String.CASE_INSENSITIVE_ORDER);
                            return zones;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return Collections.emptyList();
    }
}
