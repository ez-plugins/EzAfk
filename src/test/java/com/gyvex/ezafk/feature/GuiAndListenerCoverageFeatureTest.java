package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.event.PlayerAfkStatusChangeEvent;
import com.gyvex.ezafk.integration.EconomyIntegration;
import com.gyvex.ezafk.listener.AfkPlayerActionsGUI;
import com.gyvex.ezafk.listener.AfkPlayerOverviewGUI;
import com.gyvex.ezafk.listener.EconomyServiceListener;
import com.gyvex.ezafk.listener.PlayerActivityListener;
import com.gyvex.ezafk.listener.SimpleVoiceChatAfkListener;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.state.AfkActivationMode;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.state.LastActiveState;
import com.gyvex.ezafk.command.AfkZoneCommand;
import com.gyvex.ezafk.compatibility.item.HeadCompat;
import com.gyvex.ezafk.compatibility.item.ItemMetaCompat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiAndListenerCoverageFeatureTest {

    private org.mockbukkit.mockbukkit.ServerMock server;
    private JavaPlugin plugin;
    private com.gyvex.ezafk.EzAfk ezafk;

    @BeforeEach
    void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
        ezafk = (com.gyvex.ezafk.EzAfk) plugin;
    }

    @AfterEach
    void tearDown() {
        LastActiveState.lastActive.clear();
        AfkState.afkPlayers.clear();
        AfkState.clearBypass();
        TestHelpers.stopServer();
    }

    @Test
    void overview_and_actions_gui_open_and_click_paths() {
        PlayerMock admin = (PlayerMock) server.addPlayer("GuiAdmin2");
        admin.setOp(true);
        Player target = server.addPlayer("GuiTarget");

        AfkState.markAfk(ezafk, target, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);

        AfkPlayerActionsGUI actionsGUI = new AfkPlayerActionsGUI();
        server.getPluginManager().registerEvents(actionsGUI, plugin);

        AfkPlayerOverviewGUI overviewGUI = new AfkPlayerOverviewGUI();
        server.getPluginManager().registerEvents(overviewGUI, plugin);

        overviewGUI.openGUI(admin, 1, AfkPlayerOverviewGUI.PlayerListType.AFK);
        assertNotNull(admin.getOpenInventory());

        // Click first slot (likely AFK player head)
        InventoryClickEvent firstClick = admin.simulateInventoryClick(0);
        assertNotNull(firstClick);

        // If actions GUI opened, click any arrow slot (back button) to exercise back path.
        int arrowSlot = -1;
        for (int i = 0; i < admin.getOpenInventory().getTopInventory().getSize(); i++) {
            if (admin.getOpenInventory().getTopInventory().getItem(i) != null
                    && admin.getOpenInventory().getTopInventory().getItem(i).getType() == Material.ARROW) {
                arrowSlot = i;
                break;
            }
        }
        if (arrowSlot >= 0) {
            InventoryClickEvent backClick = admin.simulateInventoryClick(arrowSlot);
            assertNotNull(backClick);
        }

        // Re-open overview and click list toggle slot to switch list type.
        overviewGUI.openGUI(admin, 1, AfkPlayerOverviewGUI.PlayerListType.AFK);
        InventoryClickEvent toggleClick = admin.simulateInventoryClick(49);
        assertNotNull(toggleClick);

        // Re-open overview and click a non-functional empty slot path.
        overviewGUI.openGUI(admin, 1, AfkPlayerOverviewGUI.PlayerListType.ACTIVE);
        InventoryClickEvent emptyClick = admin.simulateInventoryClick(52);
        assertNotNull(emptyClick);
    }

    @Test
    void playerActivityListener_updates_on_inventory_click_path() {
        PlayerActivityListener listener = new PlayerActivityListener();
        server.getPluginManager().registerEvents(listener, plugin);

        PlayerMock p = (PlayerMock) server.addPlayer("InvActive");
        LastActiveState.lastActive.put(p.getUniqueId(), 0L);

        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 9, "Test");
        inv.setItem(0, new org.bukkit.inventory.ItemStack(Material.STONE));
        p.openInventory(inv);

        InventoryClickEvent click = p.simulateInventoryClick(0);
        assertNotNull(click);
        assertTrue(LastActiveState.getLastActive(p.getUniqueId()) > 0L);

        // chat and command events still update
        server.getPluginManager().callEvent(new org.bukkit.event.player.AsyncPlayerChatEvent(false, p, "hello", new HashSet<>()));
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerCommandPreprocessEvent(p, "/afk"));
        assertTrue(LastActiveState.getLastActive(p.getUniqueId()) > 0L);
    }

    @Test
    void economyServiceListener_register_and_unregister_paths() {
        EconomyServiceListener listener = new EconomyServiceListener();

        class FakeEconomyIntegration extends EconomyIntegration {
            boolean loaded;
            boolean unloaded;

            @Override
            public void load() {
                loaded = true;
                this.isSetup = true;
            }

            @Override
            public void unload() {
                unloaded = true;
                this.isSetup = false;
            }
        }

        FakeEconomyIntegration fake = new FakeEconomyIntegration();
        IntegrationManager.addIntegration("economy", fake);

        Economy econProvider = Mockito.mock(Economy.class);
        RegisteredServiceProvider<Economy> provider = new RegisteredServiceProvider<>(
            Economy.class,
            econProvider,
            ServicePriority.Normal,
            plugin);

        ServiceRegisterEvent registerEvent = new ServiceRegisterEvent(provider);
        ServiceUnregisterEvent unregisterEvent = new ServiceUnregisterEvent(provider);

        listener.onServiceRegister(registerEvent);
        assertTrue(fake.loaded);

        listener.onServiceUnregister(unregisterEvent);
        assertTrue(fake.unloaded);

        // non-economy path should short-circuit without exception
        RegisteredServiceProvider<Object> other = new RegisteredServiceProvider<>(
            Object.class,
            new Object(),
            ServicePriority.Normal,
            plugin);
        assertDoesNotThrow(() -> listener.onServiceRegister(new ServiceRegisterEvent(other)));
        assertDoesNotThrow(() -> listener.onServiceUnregister(new ServiceUnregisterEvent(other)));
    }

    @Test
    void simpleVoiceChatListener_handles_afk_and_return_events() {
        SimpleVoiceChatAfkListener listener = new SimpleVoiceChatAfkListener(ezafk);
        Player p = server.addPlayer("VoiceUser");

        assertDoesNotThrow(() -> listener.onAfkStatusChange(
                new PlayerAfkStatusChangeEvent(p, true, AfkReason.MANUAL, "test")));
        assertDoesNotThrow(() -> listener.onAfkStatusChange(
                new PlayerAfkStatusChangeEvent(p, false, AfkReason.MANUAL, "test")));
    }

    @Test
    void moveListener_additional_branches_world_change_and_no_to_location() {
        com.gyvex.ezafk.listener.MoveListener listener = new com.gyvex.ezafk.listener.MoveListener(ezafk);
        server.getPluginManager().registerEvents(listener, plugin);

        PlayerMock p = (PlayerMock) server.addPlayer("MoveBranch");
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);

        // null 'to' location path
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation().clone(), null));
        assertTrue(AfkState.isAfk(p.getUniqueId()));

        // world change path clears AFK
        org.bukkit.World secondWorld = server.addSimpleWorld("world2");
        org.bukkit.Location from = p.getLocation().clone();
        org.bukkit.Location to = new org.bukkit.Location(secondWorld, from.getX(), from.getY(), from.getZ());
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, from, to));
        assertFalse(AfkState.isAfk(p.getUniqueId()));

        // Bubble-column environment setup should be harmless in move handling.
        UUID id = p.getUniqueId();
        assertNotNull(id);
        p.teleport(new org.bukkit.Location(secondWorld, 5, 65, 5));
        secondWorld.getBlockAt(5, 65, 5).setType(Material.BUBBLE_COLUMN);
        assertDoesNotThrow(() -> server.getPluginManager().callEvent(
            new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation().clone(), p.getLocation().clone().add(1, 0, 0))));
    }

    @Test
    void overview_gui_click_edge_cases_and_pagination_paths() throws Exception {
        AfkPlayerOverviewGUI overviewGUI = new AfkPlayerOverviewGUI();
        server.getPluginManager().registerEvents(overviewGUI, plugin);

        PlayerMock player = (PlayerMock) server.addPlayer("OverviewCases");
        player.setOp(false);

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "AFK Players - Page 2");

        ItemStack invalidToggle = new ItemStack(Material.NETHER_STAR);
        ItemMeta invalidToggleMeta = invalidToggle.getItemMeta();
        assertNotNull(invalidToggleMeta);
        ItemMetaCompat.setItemMetadata(invalidToggleMeta, "afk_overview_list_toggle", "NOT_A_REAL_TYPE");
        invalidToggle.setItemMeta(invalidToggleMeta);
        inv.setItem(0, invalidToggle);

        ItemStack activeToggle = new ItemStack(Material.NETHER_STAR);
        ItemMeta activeToggleMeta = activeToggle.getItemMeta();
        assertNotNull(activeToggleMeta);
        ItemMetaCompat.setItemMetadata(activeToggleMeta, "afk_overview_list_toggle", "ACTIVE");
        activeToggle.setItemMeta(activeToggleMeta);
        inv.setItem(1, activeToggle);

        ItemStack headNoUuid = HeadCompat.createPlayerHead();
        inv.setItem(2, headNoUuid);

        ItemStack headBadUuid = HeadCompat.createPlayerHead();
        ItemMeta headBadUuidMeta = headBadUuid.getItemMeta();
        assertNotNull(headBadUuidMeta);
        ItemMetaCompat.setItemMetadata(headBadUuidMeta, "afk_overview_uuid", "not-a-uuid");
        headBadUuid.setItemMeta(headBadUuidMeta);
        inv.setItem(3, headBadUuid);

        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prev.getItemMeta();
        assertNotNull(prevMeta);
        prevMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
        prev.setItemMeta(prevMeta);
        inv.setItem(4, prev);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        assertNotNull(nextMeta);
        nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
        next.setItemMeta(nextMeta);
        inv.setItem(5, next);

        // Force actions GUI singleton to null to hit null-guard branch.
        Field instanceField = AfkPlayerActionsGUI.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        player.openInventory(inv);

        assertNotNull(player.simulateInventoryClick(0));
        assertNotNull(player.simulateInventoryClick(1));
        // Head click without actions permission.
        assertNotNull(player.simulateInventoryClick(2));

        player.setOp(true);
        assertNotNull(player.simulateInventoryClick(2));
        assertNotNull(player.simulateInventoryClick(3));

        ItemStack headValidUuid = HeadCompat.createPlayerHead();
        ItemMeta headValidUuidMeta = headValidUuid.getItemMeta();
        assertNotNull(headValidUuidMeta);
        ItemMetaCompat.setItemMetadata(headValidUuidMeta, "afk_overview_uuid", player.getUniqueId().toString());
        headValidUuid.setItemMeta(headValidUuidMeta);
        inv.setItem(6, headValidUuid);
        player.openInventory(inv);
        assertNotNull(player.simulateInventoryClick(6));

        // Exercise previous and next page button handlers separately (click changes inventory view).
        player.openInventory(inv);
        assertNotNull(player.simulateInventoryClick(4));
        player.openInventory(inv);
        assertNotNull(player.simulateInventoryClick(5));
    }

    @Test
    void actions_gui_permission_empty_null_target_and_action_paths() throws Exception {
        AfkPlayerActionsGUI actionsGUI = new AfkPlayerActionsGUI();
        server.getPluginManager().registerEvents(actionsGUI, plugin);

        PlayerMock noPerm = (PlayerMock) server.addPlayer("ActionNoPerm");
        actionsGUI.openGUI(noPerm, null, 1, AfkPlayerOverviewGUI.PlayerListType.AFK);
        assertNotNull(noPerm.simulateInventoryClick(0));

        PlayerMock op = (PlayerMock) server.addPlayer("ActionOp");
        op.setOp(true);

        // Empty item branch.
        Inventory emptyInventory = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "Player Actions");
        op.openInventory(emptyInventory);
        assertNotNull(op.simulateInventoryClick(0));

        // Determine configured action slot and exercise null-target action execution.
        Field actionsField = AfkPlayerActionsGUI.class.getDeclaredField("actions");
        actionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<Integer, ?> actionMap = (java.util.Map<Integer, ?>) actionsField.get(actionsGUI);
        assertFalse(actionMap.isEmpty());
        int actionSlot = actionMap.keySet().iterator().next();

        actionsGUI.openGUI(op, null, 1, AfkPlayerOverviewGUI.PlayerListType.AFK);
        assertNotNull(op.simulateInventoryClick(actionSlot));

        Player target = server.addPlayer("ActionTarget");
        actionsGUI.openGUI(op, target, 1, AfkPlayerOverviewGUI.PlayerListType.AFK);
        assertNotNull(op.simulateInventoryClick(actionSlot));

        // Click a non-action filler slot to hit action-null path.
        actionsGUI.openGUI(op, target, 1, AfkPlayerOverviewGUI.PlayerListType.AFK);
        int nullActionSlot = -1;
        for (int i = 0; i < op.getOpenInventory().getTopInventory().getSize(); i++) {
            if (!actionMap.containsKey(i)) {
                nullActionSlot = i;
                break;
            }
        }
        assertTrue(nullActionSlot >= 0);
        assertNotNull(op.simulateInventoryClick(nullActionSlot));
    }

    @Test
    void move_listener_anti_vehicle_water_and_zone_transition_paths() {
        com.gyvex.ezafk.listener.MoveListener listener = new com.gyvex.ezafk.listener.MoveListener(ezafk);
        server.getPluginManager().registerEvents(listener, plugin);

        PlayerMock p = (PlayerMock) server.addPlayer("MoveDeep");
        PlayerMock vehicle = (PlayerMock) server.addPlayer("MoveVehicle");

        ezafk.getConfig().set("afk.anti.infinite-vehicle", true);
        ezafk.getConfig().set("afk.anti.infinite-waterflow", false);
        ezafk.getConfig().set("afk.anti.bubble-column", false);

        // Anti-vehicle + flag-only true.
        ezafk.getConfig().set("afk.anti.flag-only", true);
        vehicle.addPassenger(p);
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation().clone(), p.getLocation().clone().add(1, 0, 0)));

        // Anti-vehicle + flag-only false (leave vehicle path).
        ezafk.getConfig().set("afk.anti.flag-only", false);
        vehicle.addPassenger(p);
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation().clone(), p.getLocation().clone().add(1, 0, 0)));

        // Anti-water flow path (flag-only true/false).
        ezafk.getConfig().set("afk.anti.infinite-vehicle", false);
        ezafk.getConfig().set("afk.anti.infinite-waterflow", true);
        p.teleport(new org.bukkit.Location(p.getWorld(), 30, 70, 30));
        p.getWorld().getBlockAt(30, 70, 30).setType(Material.WATER);
        p.getWorld().getBlockAt(30, 70, 30).setBlockData(Bukkit.createBlockData("minecraft:water[level=8]"));
        p.getWorld().getBlockAt(30, 69, 30).setType(Material.AIR);

        ezafk.getConfig().set("afk.anti.flag-only", true);
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation().clone(), p.getLocation().clone().add(1, 0, 0)));

        ezafk.getConfig().set("afk.anti.flag-only", false);
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation().clone(), p.getLocation().clone().add(1, 0, 0)));

        // Zone transition: enter, same zone, exit.
        ezafk.getConfig().set("afk.anti.infinite-waterflow", false);
        AfkZoneCommand zoneCommand = new AfkZoneCommand(ezafk);
        p.setOp(true);
        zoneCommand.handleAfkZone(p, new String[]{"zone", "add", "move-zone", "100", "60", "100", "120", "80", "120"});

        p.teleport(new org.bukkit.Location(p.getWorld(), 110, 70, 110));
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation().clone().add(-1, 0, 0), p.getLocation().clone()));
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation().clone(), p.getLocation().clone().add(1, 0, 0)));

        p.teleport(new org.bukkit.Location(p.getWorld(), 140, 70, 140));
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation().clone().add(-1, 0, 0), p.getLocation().clone()));
    }
}
