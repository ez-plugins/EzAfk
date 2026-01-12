package com.gyvex.ezafk.gui;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.compatibility.CompatibilityUtil;
import com.gyvex.ezafk.gui.AfkPlayerOverviewGUI.PlayerListType;
import com.gyvex.ezafk.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AfkPlayerActionsGUI implements Listener {

    private static final String GUI_TITLE = ChatColor.DARK_GRAY + "Player Actions";
    private static final int MIN_INVENTORY_SIZE = 9;
    private static final int MAX_INVENTORY_SIZE = 54;
    private static final HashMap<UUID, UUID> targetPlayers = new HashMap<>();
    private static final Map<UUID, Integer> returnPages = new HashMap<>();
    private static final Map<UUID, PlayerListType> returnListTypes = new HashMap<>();
    private static AfkPlayerActionsGUI instance;

    private final Map<Integer, GuiAction> actions = new HashMap<>();
    private ItemStack backButtonItem;
    private int backButtonSlot = -1;
    private int inventorySize = MIN_INVENTORY_SIZE;

    public AfkPlayerActionsGUI() {
        instance = this;
        reloadActions();
    }

    public static void reloadConfiguredActions() {
        if (instance != null) {
            instance.reloadActions();
        }
    }

    public static AfkPlayerActionsGUI getInstance() {
        return instance;
    }

    public void openGUI(Player opener, Player targetPlayer) {
        openGUI(opener, targetPlayer, 1, PlayerListType.AFK);
    }

    public void openGUI(Player opener, Player targetPlayer, int overviewPage) {
        openGUI(opener, targetPlayer, overviewPage, PlayerListType.AFK);
    }

    public void openGUI(Player opener, Player targetPlayer, int overviewPage, PlayerListType listType) {
        UUID openerId = opener.getUniqueId();
        targetPlayers.put(openerId, targetPlayer != null ? targetPlayer.getUniqueId() : null);
        returnPages.put(openerId, Math.max(1, overviewPage));
        returnListTypes.put(openerId, listType != null ? listType : PlayerListType.AFK);

        // Create inventory using compatibility utility for cross-version support
        Inventory inventory = CompatibilityUtil.createInventory(null, inventorySize, GUI_TITLE);

        for (Map.Entry<Integer, GuiAction> entry : actions.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().createIcon());
        }

        if (backButtonSlot >= 0 && backButtonItem != null) {
            inventory.setItem(backButtonSlot, backButtonItem.clone());
        }

        // Open GUI for the opener
        opener.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = CompatibilityUtil.getInventoryTitle(event);

        if (!title.equals(GUI_TITLE)) {
            return;
        }

        if (event.getClickedInventory() == null || event.getView().getTopInventory() != event.getClickedInventory()) {
            return;
        }

        event.setCancelled(true); // Prevent players from taking items from the GUI

        ItemStack clickedItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        if (!player.hasPermission("ezafk.gui.actions") && !player.isOp()) {
            MessageManager.sendMessage(player, "gui.actions.no-permission",
                    "&cYou don't have permission to use player actions.");
            player.closeInventory();
            return;
        }

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (backButtonSlot >= 0 && event.getSlot() == backButtonSlot) {
            handleBackButtonClick(player);
            return;
        }

        GuiAction action = actions.get(event.getSlot());

        if (action == null) {
            return;
        }

        UUID targetPlayerId = targetPlayers.get(player.getUniqueId());
        Player targetPlayer = targetPlayerId != null ? Bukkit.getPlayer(targetPlayerId) : null;

        action.execute(player, targetPlayer);
        UUID playerId = player.getUniqueId();
        targetPlayers.remove(playerId);
        returnPages.remove(playerId);
        returnListTypes.remove(playerId);
    }

    private void reloadActions() {
        actions.clear();
        inventorySize = MIN_INVENTORY_SIZE;
        backButtonSlot = -1;
        backButtonItem = createBackButtonItem();

        FileConfiguration config = EzAfk.getInstance().getGuiConfig();
        ConfigurationSection section = config.getConfigurationSection("actions");

        int configuredSize = config.getInt("inventory-size", config.getInt("actions.inventory-size", MIN_INVENTORY_SIZE));
        inventorySize = normalizeInventorySize(configuredSize);

        if (section == null) {
            EzAfk.getInstance().getLogger().warning("No GUI actions configured. The player actions GUI will be empty.");
        } else {
            for (String key : section.getKeys(false)) {
                ConfigurationSection actionSection = section.getConfigurationSection(key);
                if (actionSection == null) {
                    continue;
                }

                int slot = actionSection.getInt("slot", actions.size());
                if (slot < 0) {
                    continue;
                }

                if (slot >= MAX_INVENTORY_SIZE) {
                    EzAfk.getInstance().getLogger().warning("Slot " + slot + " for GUI action '" + key + "' exceeds the maximum supported size. Skipping...");
                    continue;
                }

                String materialName = actionSection.getString("material", "STONE");
                Material material = null;

                if (materialName != null) {
                    material = Material.matchMaterial(materialName);

                    if (material == null) {
                        material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
                    }
                }

                if (material == null) {
                    EzAfk.getInstance().getLogger().warning("Invalid material for GUI action '" + key + "'. Skipping...");
                    continue;
                }

                String displayName = actionSection.getString("display-name", key);
                String typeName = actionSection.getString("type", "MESSAGE");

                GuiAction.ActionType actionType;
                try {
                    actionType = GuiAction.ActionType.valueOf(typeName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    EzAfk.getInstance().getLogger().warning("Invalid action type '" + typeName + "' for GUI action '" + key + "'. Skipping...");
                    continue;
                }

                String targetMessage = actionSection.getString("target-message");
                String feedbackMessage = actionSection.getString("feedback-message");
                String command = actionSection.getString("command");

                GuiAction action = new GuiAction(material, displayName, actionType, targetMessage, feedbackMessage, command);
                actions.put(slot, action);

                int requiredSize = ((slot / 9) + 1) * 9;
                if (requiredSize > MAX_INVENTORY_SIZE) {
                    requiredSize = MAX_INVENTORY_SIZE;
                }
                inventorySize = Math.max(inventorySize, requiredSize);
            }
        }

        ensureBackButtonSlot();
    }

    private void handleBackButtonClick(Player player) {
        UUID playerId = player.getUniqueId();
        int page = returnPages.getOrDefault(playerId, 1);
        PlayerListType listType = returnListTypes.getOrDefault(playerId, PlayerListType.AFK);

        targetPlayers.remove(playerId);
        returnPages.remove(playerId);
        returnListTypes.remove(playerId);

        AfkPlayerOverviewGUI overviewGUI = new AfkPlayerOverviewGUI();
        overviewGUI.openGUI(player, page, listType);
    }

    private void ensureBackButtonSlot() {
        if (backButtonItem == null) {
            return;
        }

        backButtonSlot = findAvailableBackSlot();

        while (backButtonSlot < 0 && inventorySize < MAX_INVENTORY_SIZE) {
            inventorySize += 9;
            backButtonSlot = findAvailableBackSlot();
        }

        if (backButtonSlot < 0) {
            EzAfk.getInstance().getLogger().warning("Failed to allocate a slot for the back button in the player actions GUI.");
        }
    }

    private int findAvailableBackSlot() {
        for (int slot = inventorySize - 1; slot >= 0; slot--) {
            if (!actions.containsKey(slot)) {
                return slot;
            }
        }

        return -1;
    }

    private int normalizeInventorySize(int configuredSize) {
        int size = configuredSize;

        if (size < MIN_INVENTORY_SIZE) {
            size = MIN_INVENTORY_SIZE;
        } else if (size > MAX_INVENTORY_SIZE) {
            size = MAX_INVENTORY_SIZE;
        }

        if (size % 9 != 0) {
            size = ((size + 8) / 9) * 9;
        }

        if (size != configuredSize) {
            EzAfk.getInstance().getLogger().warning("Adjusted player actions GUI inventory size from " + configuredSize + " to " + size + " (must be a multiple of 9 between 9 and 54).");
        }

        return size;
    }

    private ItemStack createBackButtonItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Back");
            item.setItemMeta(meta);
        }

        return item;
    }

    private static String replacePlaceholders(Player executor, Player target, String message) {
        if (message == null) {
            return null;
        }

        String result = message;

        result = result.replace("%executor%", executor != null ? executor.getName() : "");
        result = result.replace("%player%", target != null ? target.getName() : "");

        return result;
    }

    private static String colorize(String message) {
        if (message == null) {
            return null;
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static class GuiAction {
        private final Material material;
        private final String displayName;
        private final ActionType type;
        private final String targetMessage;
        private final String feedbackMessage;
        private final String command;

        GuiAction(Material material, String displayName, ActionType type, String targetMessage, String feedbackMessage, String command) {
            this.material = material;
            this.displayName = colorize(displayName);
            this.type = type;
            this.targetMessage = targetMessage;
            this.feedbackMessage = feedbackMessage;
            this.command = command;
        }

        ItemStack createIcon() {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(displayName);
                item.setItemMeta(meta);
            }

            return item;
        }

        void execute(Player executor, Player target) {
            switch (type) {
                case KICK:
                    if (target == null) {
                        MessageManager.sendMessage(executor, "gui.actions.error.player-not-found", "&cCould not find the selected player.");
                        return;
                    }
                    String kickMessage = colorize(replacePlaceholders(executor, target, targetMessage));
                    String defaultKickMessage = MessageManager.getMessage("gui.actions.default-kick-message", "&cYou were kicked for being AFK too long.");
                    target.kickPlayer(kickMessage != null ? kickMessage : defaultKickMessage);
                    break;
                case MESSAGE:
                    if (target == null) {
                        MessageManager.sendMessage(executor, "gui.actions.error.player-not-found", "&cCould not find the selected player.");
                        return;
                    }
                    String alertMessage = colorize(replacePlaceholders(executor, target, targetMessage));
                    if (alertMessage != null) {
                        target.sendMessage(alertMessage);
                    }
                    break;
                case TELEPORT:
                    if (target == null) {
                        MessageManager.sendMessage(executor, "gui.actions.error.player-not-found", "&cCould not find the selected player.");
                        return;
                    }
                    executor.teleport(target);
                    break;
                case COMMAND:
                    String commandToRun = replacePlaceholders(executor, target, command);
                    if (commandToRun == null || commandToRun.isEmpty()) {
                        MessageManager.sendMessage(executor, "gui.actions.error.no-command", "&cNo command configured for this action.");
                        return;
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                    break;
                default:
                    return;
            }

            String feedback = colorize(replacePlaceholders(executor, target, feedbackMessage));
            if (feedback != null && !feedback.isEmpty()) {
                executor.sendMessage(feedback);
            }

            executor.closeInventory();
        }

        enum ActionType {
            KICK,
            MESSAGE,
            TELEPORT,
            COMMAND
        }
    }
}
