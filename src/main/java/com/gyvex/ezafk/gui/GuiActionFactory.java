package com.gyvex.ezafk.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

public class GuiActionFactory {
    public static GuiAction fromConfigSection(ConfigurationSection section) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.matchMaterial(materialName.toUpperCase());
        }
        if (material == null) {
            material = Material.STONE;
        }
        String displayName = section.getString("display-name", "Action");
        List<String> lore = section.getStringList("lore");
        String typeName = section.getString("type", "MESSAGE");
        GuiAction.ActionType actionType;
        try {
            actionType = GuiAction.ActionType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            actionType = GuiAction.ActionType.MESSAGE;
        }
        String targetMessage = section.getString("target-message");
        String feedbackMessage = section.getString("feedback-message");
        String command = section.getString("command");
        return new GuiAction(material, displayName, lore, actionType, targetMessage, feedbackMessage, command);
    }
}
