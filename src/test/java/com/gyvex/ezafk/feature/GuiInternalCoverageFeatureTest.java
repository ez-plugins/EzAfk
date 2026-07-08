package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.listener.AfkPlayerActionsGUI;
import com.gyvex.ezafk.listener.AfkPlayerOverviewGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GuiInternalCoverageFeatureTest {

    private org.mockbukkit.mockbukkit.ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
    }

    @AfterEach
    void tearDown() {
        TestHelpers.stopServer();
    }

    @Test
    void actionsGui_private_helpers_are_invokable() throws Exception {
        AfkPlayerActionsGUI gui = new AfkPlayerActionsGUI();

        Method normalize = AfkPlayerActionsGUI.class.getDeclaredMethod("normalizeInventorySize", int.class);
        normalize.setAccessible(true);
        normalize.invoke(gui, 1);
        normalize.invoke(gui, 999);
        normalize.invoke(gui, 10);

        Method createBack = AfkPlayerActionsGUI.class.getDeclaredMethod("createBackButtonItem");
        createBack.setAccessible(true);
        Object back = createBack.invoke(gui);
        assertNotNull(back);

        Method ensureBack = AfkPlayerActionsGUI.class.getDeclaredMethod("ensureBackButtonSlot");
        ensureBack.setAccessible(true);
        ensureBack.invoke(gui);

        PlayerMock p = (PlayerMock) server.addPlayer("ActionsOpen");
        Player target = server.addPlayer("ActionsTarget");
        gui.openGUI(p, target, 1, AfkPlayerOverviewGUI.PlayerListType.AFK);
        assertNotNull(p.getOpenInventory());
    }

    @Test
    void overviewGui_private_helpers_are_invokable() throws Exception {
        AfkPlayerOverviewGUI gui = new AfkPlayerOverviewGUI();

        Method createButton = AfkPlayerOverviewGUI.class.getDeclaredMethod("createButtonItem", String.class, Material.class);
        createButton.setAccessible(true);
        ItemStack button = (ItemStack) createButton.invoke(gui, "Name", Material.ARROW);
        assertNotNull(button);

        Method createToggle = AfkPlayerOverviewGUI.class.getDeclaredMethod("createToggleItem", AfkPlayerOverviewGUI.PlayerListType.class);
        createToggle.setAccessible(true);
        Object toggle = createToggle.invoke(gui, AfkPlayerOverviewGUI.PlayerListType.AFK);
        assertNotNull(toggle);

        Method buildLore = AfkPlayerOverviewGUI.class.getDeclaredMethod("buildPlayerLore", OfflinePlayer.class);
        buildLore.setAccessible(true);
        OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.randomUUID());
        buildLore.invoke(gui, offline);

        Method createHead = AfkPlayerOverviewGUI.class.getDeclaredMethod("createPlayerHead", OfflinePlayer.class);
        createHead.setAccessible(true);
        Object head = createHead.invoke(gui, offline);
        assertNotNull(head);

        PlayerMock p = (PlayerMock) server.addPlayer("OverviewOpen");
        gui.openGUI(p, 1, AfkPlayerOverviewGUI.PlayerListType.ACTIVE);
        assertDoesNotThrow(() -> p.simulateInventoryClick(45));
        assertDoesNotThrow(() -> p.simulateInventoryClick(53));
        assertDoesNotThrow(() -> p.simulateInventoryClick(49));
    }
}
