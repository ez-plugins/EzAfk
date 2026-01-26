package com.gyvex.ezafk.gui;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.compatibility.CompatibilityUtil;
import com.gyvex.ezafk.manager.MessageManager;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.state.AfkStatusDetails;
import com.gyvex.ezafk.state.LastActiveState;
import com.gyvex.ezafk.util.DurationFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AfkPlayerOverviewGUI implements Listener {

    private static final DateTimeFormatter LAST_SEEN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final int INVENTORY_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int LIST_TOGGLE_SLOT = 49;

    public enum PlayerListType {
        AFK("AFK Players"),
        ACTIVE("Active Players");

        private final String friendlyName;

        PlayerListType(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public String getTitle() {
            return ChatColor.DARK_GRAY + friendlyName;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        public PlayerListType getOpposite() {
            return this == AFK ? ACTIVE : AFK;
        }

        public String getToggleDisplayName() {
            return ChatColor.AQUA + "View " + getOpposite().getFriendlyName();
        }

        public static PlayerListType fromTitle(String title) {
            for (PlayerListType type : values()) {
                if (title.startsWith(type.getTitle())) {
                    return type;
                }
            }
            return null;
        }
    }

    private static final String PLAYER_UUID_KEY = "afk_overview_uuid";
    private static final String LIST_TOGGLE_KEY = "afk_overview_list_toggle";

    public AfkPlayerOverviewGUI() {
    }

    public void openGUI(Player player, int page) {
        openGUI(player, page, PlayerListType.AFK);
    }

    public void openGUI(Player player, int page, PlayerListType listType) {
        List<OfflinePlayer> trackedPlayers = getPlayers(listType);

        int totalPages = Math.max(1, (int) Math.ceil((double) trackedPlayers.size() / PAGE_SIZE));

        if (page < 1) {
            page = 1;
        } else if (page > totalPages) {
            page = totalPages;
        }

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, listType.getTitle() + " - Page " + page);

        if (!trackedPlayers.isEmpty()) {
            int startIndex = (page - 1) * PAGE_SIZE;
            int endIndex = Math.min(startIndex + PAGE_SIZE, trackedPlayers.size());

            for (int i = startIndex; i < endIndex; i++) {
                OfflinePlayer offlinePlayer = trackedPlayers.get(i);
                ItemStack playerHead = createPlayerHead(offlinePlayer);
                inventory.addItem(playerHead);
            }
        }

        ItemStack prevPageItem = createButtonItem(ChatColor.GREEN + "Previous Page", Material.ARROW);
        ItemStack nextPageItem = createButtonItem(ChatColor.GREEN + "Next Page", Material.ARROW);

        if (page > 1) {
            inventory.setItem(PREVIOUS_PAGE_SLOT, prevPageItem);
        }
        if (page < totalPages) {
            inventory.setItem(NEXT_PAGE_SLOT, nextPageItem);
        }

        inventory.setItem(LIST_TOGGLE_SLOT, createToggleItem(listType));

        player.openInventory(inventory);
    }

    private List<OfflinePlayer> getPlayers(PlayerListType listType) {
        List<OfflinePlayer> players = new ArrayList<>();

        if (listType == PlayerListType.AFK) {
            List<UUID> afkPlayers = new ArrayList<>(AfkState.afkPlayers);
            for (UUID playerId : afkPlayers) {
                players.add(Bukkit.getOfflinePlayer(playerId));
            }
        } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!AfkState.isAfk(onlinePlayer.getUniqueId())) {
                    players.add(onlinePlayer);
                }
            }
        }

        return players;
    }

    private ItemStack createPlayerHead(OfflinePlayer offlinePlayer) {
        ItemStack head = CompatibilityUtil.createPlayerHead();
        ItemMeta meta = head.getItemMeta();

        if (!(meta instanceof SkullMeta)) {
            return head;
        }

        SkullMeta skullMeta = (SkullMeta) meta;
        CompatibilityUtil.setSkullOwner(skullMeta, offlinePlayer);

        String displayName = offlinePlayer.getName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = offlinePlayer.getUniqueId().toString();
        }
        skullMeta.setDisplayName(ChatColor.YELLOW + displayName);

        List<String> lore = buildPlayerLore(offlinePlayer);
        if (!lore.isEmpty()) {
            skullMeta.setLore(lore);
        }

        CompatibilityUtil.setItemMetadata(skullMeta, PLAYER_UUID_KEY, offlinePlayer.getUniqueId().toString());

        head.setItemMeta(skullMeta);
        return head;
    }

    private List<String> buildPlayerLore(OfflinePlayer offlinePlayer) {
        List<String> lore = new ArrayList<>();
        UUID playerId = offlinePlayer.getUniqueId();

        if (offlinePlayer.isOnline()) {
            lore.add(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Online");
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null) {
                lore.add(ChatColor.GRAY + "World: " + ChatColor.AQUA + onlinePlayer.getWorld().getName());
            }
            long lastActive = LastActiveState.getSecondsSinceLastActive(playerId);
            lore.add(ChatColor.GRAY + "Last Activity: "
                    + ChatColor.AQUA + DurationFormatter.formatDuration(lastActive));
        } else {
            lore.add(ChatColor.GRAY + "Status: " + ChatColor.RED + "Offline");
            long lastPlayed = offlinePlayer.getLastPlayed();
            if (lastPlayed > 0L) {
                Duration since = Duration.between(Instant.ofEpochMilli(lastPlayed), Instant.now());
                if (since.isNegative()) {
                    since = Duration.ZERO;
                }

                lore.add(ChatColor.GRAY + "Last Active: " + ChatColor.AQUA
                        + DurationFormatter.formatDuration(since.getSeconds()) + ChatColor.GRAY + " ago");
                lore.add(ChatColor.GRAY + "Last Seen: " + ChatColor.AQUA + LAST_SEEN_FORMATTER.format(Instant.ofEpochMilli(lastPlayed)));
            }
        }

        if (AfkState.isAfk(playerId)) {
            long afkSeconds = AfkState.getSecondsSinceAfk(playerId);
            if (afkSeconds >= 0) {
                lore.add(ChatColor.GRAY + "AFK for: " + ChatColor.AQUA
                        + DurationFormatter.formatDuration(afkSeconds));
            }
            AfkStatusDetails details = AfkState.getAfkStatusDetails(playerId);
            if (details != null) {
                lore.add(ChatColor.GRAY + "Reason: " + ChatColor.YELLOW + details.getReasonDisplayName());
                if (details.hasDetail()) {
                    lore.add(ChatColor.DARK_GRAY + details.detail());
                }
            }
        }

        return lore;
    }

    private ItemStack createButtonItem(String displayName, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createToggleItem(PlayerListType currentType) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(currentType.getToggleDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Switch to " + ChatColor.AQUA + currentType.getOpposite().getFriendlyName());
            meta.setLore(lore);

            CompatibilityUtil.setItemMetadata(meta, LIST_TOGGLE_KEY, currentType.getOpposite().name());

            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = CompatibilityUtil.getInventoryTitle(event);

        PlayerListType listType = PlayerListType.fromTitle(title);

        if (listType == null) {
            return;
        }

        if (event.getClickedInventory() == null || event.getView().getTopInventory() != event.getClickedInventory()) {
            return;
        }

        event.setCancelled(true); // Prevent players from taking items from the GUI

        ItemStack clickedItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        int currentPage = 1;

        int lastSpace = title.lastIndexOf(" ");
        if (lastSpace >= 0) {
            try {
                currentPage = Integer.parseInt(title.substring(lastSpace + 1));
            } catch (NumberFormatException ignored) {
                currentPage = 1;
            }
        }

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta itemMeta = clickedItem.getItemMeta();

        if (itemMeta != null) {
            String targetListTypeName = CompatibilityUtil.getItemMetadata(itemMeta, LIST_TOGGLE_KEY);

            if (targetListTypeName != null) {
                try {
                    PlayerListType targetType = PlayerListType.valueOf(targetListTypeName);
                    if (targetType == PlayerListType.ACTIVE
                            && !player.hasPermission("ezafk.gui.view-active")
                            && !player.isOp()) {
                        MessageManager.sendMessage(player, "gui.overview.active.no-permission",
                                "&cYou don't have permission to view active players.");
                        return;
                    }
                    openGUI(player, 1, targetType);
                } catch (IllegalArgumentException exception) {
                    Registry.get().getLogger().warning("Invalid overview list type metadata: " + targetListTypeName);
                }
                return;
            }
        }

        if (CompatibilityUtil.isPlayerHead(clickedItem)) {
            if (!player.hasPermission("ezafk.gui.actions") && !player.isOp()) {
                MessageManager.sendMessage(player, "gui.actions.no-permission",
                        "&cYou don't have permission to use player actions.");
                return;
            }
            if (itemMeta == null) {
                return;
            }

            String storedUuid = CompatibilityUtil.getItemMetadata(itemMeta, PLAYER_UUID_KEY);

            if (storedUuid == null) {
                Registry.get().getLogger().warning("Player head missing AFK UUID metadata");
                return;
            }

            UUID playerId;
            try {
                playerId = UUID.fromString(storedUuid);
            } catch (IllegalArgumentException exception) {
                Registry.get().getLogger().warning("Invalid AFK UUID metadata: " + storedUuid);
                return;
            }

            AfkPlayerActionsGUI actionsGUI = AfkPlayerActionsGUI.getInstance();
            if (actionsGUI == null) {
                Registry.get().getLogger().warning("Player actions GUI is not initialized. Unable to open player actions view.");
                return;
            }

            actionsGUI.openGUI(player, Bukkit.getPlayer(playerId), currentPage, listType);
        } else if (clickedItem.getType() == Material.ARROW) {
            // Handle pagination buttons
            if (itemMeta == null) {
                return;
            }

            String displayName = itemMeta.getDisplayName();

            if ((ChatColor.GREEN + "Previous Page").equals(displayName)) {
                openGUI(player, currentPage - 1, listType);
            } else if ((ChatColor.GREEN + "Next Page").equals(displayName)) {
                openGUI(player, currentPage + 1, listType);
            }
        }
    }
}
