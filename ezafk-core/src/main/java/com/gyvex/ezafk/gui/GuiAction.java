package com.gyvex.ezafk.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import java.util.List;

public class GuiAction {
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final ActionType type;
    private final String targetMessage;
    private final String feedbackMessage;
    private final String command;

    public GuiAction(Material material, String displayName, List<String> lore, ActionType type, String targetMessage, String feedbackMessage, String command) {
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.type = type;
        this.targetMessage = targetMessage;
        this.feedbackMessage = feedbackMessage;
        this.command = command;
    }

    public ItemStack createIcon() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            com.gyvex.ezafk.compatibility.LoreUtil.setDisplayName(meta, displayName, null, org.bukkit.Bukkit.getLogger());
            if (lore != null) {
                com.gyvex.ezafk.compatibility.LoreUtil.setLore(meta, lore, null, org.bukkit.Bukkit.getLogger());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void execute(Player executor, Player target) {
        switch (type) {
            case KICK:
                if (target == null) {
                    com.gyvex.ezafk.manager.MessageManager.sendMessage(executor, "gui.actions.error.player-not-found", "&cCould not find the selected player.");
                    return;
                }
                String kickMessage = com.gyvex.ezafk.util.PlaceholderUtil.colorize(com.gyvex.ezafk.util.PlaceholderUtil.replacePlaceholders(executor, target, targetMessage));
                String defaultKickMessage = com.gyvex.ezafk.manager.MessageManager.getMessage("gui.actions.default-kick-message", "&cYou were kicked for being AFK too long.");
                target.kickPlayer(kickMessage != null ? kickMessage : defaultKickMessage);
                break;
            case MESSAGE:
                if (target == null) {
                    com.gyvex.ezafk.manager.MessageManager.sendMessage(executor, "gui.actions.error.player-not-found", "&cCould not find the selected player.");
                    return;
                }
                String alertMessage = com.gyvex.ezafk.util.PlaceholderUtil.colorize(com.gyvex.ezafk.util.PlaceholderUtil.replacePlaceholders(executor, target, targetMessage));
                if (alertMessage != null) {
                    target.sendMessage(alertMessage);
                }
                break;
            case TELEPORT:
                if (target == null) {
                    com.gyvex.ezafk.manager.MessageManager.sendMessage(executor, "gui.actions.error.player-not-found", "&cCould not find the selected player.");
                    return;
                }
                executor.teleport(target);
                break;
            case COMMAND:
                String commandToRun = com.gyvex.ezafk.util.PlaceholderUtil.replacePlaceholders(executor, target, command);
                if (commandToRun == null || commandToRun.isEmpty()) {
                    com.gyvex.ezafk.manager.MessageManager.sendMessage(executor, "gui.actions.error.no-command", "&cNo command configured for this action.");
                    return;
                }
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), commandToRun);
                break;
            default:
                return;
        }

        String feedback = com.gyvex.ezafk.util.PlaceholderUtil.colorize(com.gyvex.ezafk.util.PlaceholderUtil.replacePlaceholders(executor, target, feedbackMessage));
        if (feedback != null && !feedback.isEmpty()) {
            executor.sendMessage(feedback);
        }

        executor.closeInventory();
    }

    public enum ActionType {
        KICK,
        MESSAGE,
        TELEPORT,
        COMMAND
    }
}
