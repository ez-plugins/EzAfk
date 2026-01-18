package com.gyvex.ezafk.command;

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
            "reload", "gui", "toggle", "bypass", "info", "time", "top"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return MAIN_COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("toggle") || sub.equals("bypass") || sub.equals("info") || (sub.equals("time") && sender.hasPermission("ezafk.time.others"))) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .collect(Collectors.toList());
                Collections.sort(playerNames, String.CASE_INSENSITIVE_ORDER);
                return playerNames;
            }
        }
        return Collections.emptyList();
    }
}
