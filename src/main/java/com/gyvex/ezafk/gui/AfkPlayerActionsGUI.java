package com.gyvex.ezafk.gui;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.compatibility.CompatibilityUtil;
import com.gyvex.ezafk.compatibility.LoreUtil;
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
import com.gyvex.ezafk.util.PlaceholderUtil;
import java.util.HashMap;
import java.util.List;
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

        // Filler item for empty slots
        FileConfiguration config = Registry.get().getConfigManager().getGuiConfig();
        ItemStack filler = null;
        if (config.getBoolean("empty-slot-filler.enabled", true)) {
            String matName = config.getString("empty-slot-filler.material", "GRAY_STAINED_GLASS_PANE");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;
            filler = new ItemStack(mat);
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                String display = config.getString("empty-slot-filler.display-name", " ");
                List<String> lore = config.getStringList("empty-slot-filler.lore");
                LoreUtil.setDisplayName(meta, display, null, org.bukkit.Bukkit.getLogger());
                LoreUtil.setLore(meta, lore, null, org.bukkit.Bukkit.getLogger());
                filler.setItemMeta(meta);
            }
        }

        // Place actions and filler
        for (int i = 0; i < inventorySize; i++) {
            if (actions.containsKey(i)) {
                inventory.setItem(i, actions.get(i).createIcon());
            } else if (backButtonSlot == i && backButtonItem != null) {
                inventory.setItem(i, backButtonItem.clone());
            } else if (filler != null) {
                inventory.setItem(i, filler.clone());
            }
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

        FileConfiguration config = Registry.get().getConfigManager().getGuiConfig();
        ConfigurationSection section = config.getConfigurationSection("actions");

        int configuredSize = config.getInt("inventory-size", config.getInt("actions.inventory-size", MIN_INVENTORY_SIZE));
        inventorySize = normalizeInventorySize(configuredSize);

        if (section == null) {
            Registry.get().getLogger().warning("No GUI actions configured. The player actions GUI will be empty.");
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
                    Registry.get().getLogger().warning("Slot " + slot + " for GUI action '" + key + "' exceeds the maximum supported size. Skipping...");
                    continue;
                }
                GuiAction action = GuiActionFactory.fromConfigSection(actionSection);
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
            Registry.get().getLogger().warning("Failed to allocate a slot for the back button in the player actions GUI.");
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
            Registry.get().getLogger().warning("Adjusted player actions GUI inventory size from " + configuredSize + " to " + size + " (must be a multiple of 9 between 9 and 54).");
        }

        return size;
    }

    private ItemStack createBackButtonItem() {
        FileConfiguration config = Registry.get().getConfigManager().getGuiConfig();
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String display = config.getString("back-button.display-name", "&6« Back to Overview");
            List<String> lore = config.getStringList("back-button.lore");
            if (lore == null || lore.isEmpty()) {
                lore = java.util.Collections.singletonList("&7Return to the player list.");
            }
            LoreUtil.setDisplayName(meta, display, null, org.bukkit.Bukkit.getLogger());
            LoreUtil.setLore(meta, lore, null, org.bukkit.Bukkit.getLogger());
            item.setItemMeta(meta);
        }
        return item;
    }
}
