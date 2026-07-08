package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.command.AfkZoneCommand;
import com.gyvex.ezafk.listener.MoveListener;
import com.gyvex.ezafk.manager.AfkZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveListenerInternalCoverageTest {

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
        TestHelpers.stopServer();
    }

    @Test
    void private_helpers_cover_flowing_water_and_distance_logic() throws Exception {
        MoveListener listener = new MoveListener(ezafk);

        Method formatLocation = MoveListener.class.getDeclaredMethod("formatLocation", Location.class);
        formatLocation.setAccessible(true);
        String s1 = (String) formatLocation.invoke(listener, new Location(null, 1, 2, 3));
        assertNotNull(s1);

        Method meaningful = MoveListener.class.getDeclaredMethod("hasMeaningfulPositionChange", PlayerMoveEvent.class);
        meaningful.setAccessible(true);

        Player p = server.addPlayer("MoveInternals");
        Location from = p.getLocation().clone();
        Location toSame = from.clone();
        Location toMove = from.clone().add(1, 0, 0);

        assertFalse((boolean) meaningful.invoke(listener, new PlayerMoveEvent(p, from, null)));
        assertFalse((boolean) meaningful.invoke(listener, new PlayerMoveEvent(p, from, toSame)));
        assertTrue((boolean) meaningful.invoke(listener, new PlayerMoveEvent(p, from, toMove)));

        Method flow = MoveListener.class.getDeclaredMethod("isFlowingWaterThatPushes", Block.class);
        flow.setAccessible(true);

        org.bukkit.World world = server.getWorlds().get(0);
        Block water = world.getBlockAt(10, 65, 10);
        water.setType(Material.WATER);

        // Source water should not count as pushing flow.
        water.setBlockData(Bukkit.createBlockData("minecraft:water[level=0]"));
        assertFalse((boolean) flow.invoke(listener, water));

        // Falling/high level water with passable block below should count.
        water.setBlockData(Bukkit.createBlockData("minecraft:water[level=8]"));
        world.getBlockAt(10, 64, 10).setType(Material.AIR);
        assertTrue((boolean) flow.invoke(listener, water));

        // Mid-level flowing water with side opening should also count.
        water.setBlockData(Bukkit.createBlockData("minecraft:water[level=3]"));
        world.getBlockAt(11, 65, 10).setType(Material.AIR);
        assertTrue((boolean) flow.invoke(listener, water));
    }

    @Test
    void onMove_tolerates_multiple_anti_configurations() {
        MoveListener listener = new MoveListener(ezafk);
        server.getPluginManager().registerEvents(listener, plugin);

        Player p = server.addPlayer("MoveConfig");
        Location from = p.getLocation().clone();
        Location to = from.clone().add(1, 0, 0);

        ezafk.getConfig().set("afk.anti.infinite-vehicle", false);
        ezafk.getConfig().set("afk.anti.flag-only", false);
        ezafk.getConfig().set("afk.anti.infinite-waterflow", false);
        ezafk.getConfig().set("afk.anti.bubble-column", false);

        assertDoesNotThrow(() -> server.getPluginManager().callEvent(new PlayerMoveEvent(p, from, to)));

        // Bubble-column branch setup
        p.getWorld().getBlockAt(from.getBlockX(), from.getBlockY(), from.getBlockZ()).setType(Material.BUBBLE_COLUMN);
        ezafk.getConfig().set("afk.anti.bubble-column", true);
        ezafk.getConfig().set("afk.anti.flag-only", true);
        assertDoesNotThrow(() -> server.getPluginManager().callEvent(new PlayerMoveEvent(p, from, to)));

        // Ensure a zone exists for transition checks in move handling.
        ((org.mockbukkit.mockbukkit.entity.PlayerMock) p).setOp(true);
        AfkZoneCommand zoneCommand = new AfkZoneCommand(ezafk);
        zoneCommand.handleAfkZone(p, new String[]{"zone", "add", "move-internal-zone", "200", "60", "200", "220", "80", "220"});
        List<com.gyvex.ezafk.zone.Zone> zones = AfkZoneManager.getZones();
        assertFalse(zones.isEmpty());

        p.teleport(new Location(p.getWorld(), 210, 70, 210));
        assertDoesNotThrow(() -> server.getPluginManager().callEvent(new PlayerMoveEvent(p, p.getLocation().clone().add(-1, 0, 0), p.getLocation().clone())));
        p.teleport(new Location(p.getWorld(), 240, 70, 240));
        assertDoesNotThrow(() -> server.getPluginManager().callEvent(new PlayerMoveEvent(p, p.getLocation().clone().add(-1, 0, 0), p.getLocation().clone())));
    }

    @Test
    void private_helpers_cover_infinite_water_and_bubble_body_below() throws Exception {
        MoveListener listener = new MoveListener(ezafk);
        Player p = server.addPlayer("MoveHelperBranches");

        Method infiniteFlow = MoveListener.class.getDeclaredMethod("isInInfiniteWaterFlow", Player.class);
        infiniteFlow.setAccessible(true);

        p.teleport(new Location(p.getWorld(), 300, 70, 300));
        p.getWorld().getBlockAt(300, 70, 300).setType(Material.WATER);
        p.getWorld().getBlockAt(300, 70, 300).setBlockData(Bukkit.createBlockData("minecraft:water[level=8]"));
        p.getWorld().getBlockAt(300, 69, 300).setType(Material.AIR);
        assertTrue((boolean) infiniteFlow.invoke(listener, p));

        p.teleport(new Location(p.getWorld(), 305, 70, 305));
        p.getWorld().getBlockAt(305, 70, 305).setType(Material.STONE);
        assertFalse((boolean) infiniteFlow.invoke(listener, p));

        Method bubble = MoveListener.class.getDeclaredMethod("isInBubbleColumn", Player.class);
        bubble.setAccessible(true);

        p.teleport(new Location(p.getWorld(), 310, 70, 310));
        p.getWorld().getBlockAt(310, 70, 310).setType(Material.AIR);
        p.getWorld().getBlockAt(310, 71, 310).setType(Material.BUBBLE_COLUMN);
        p.getWorld().getBlockAt(310, 69, 310).setType(Material.AIR);
        assertTrue((boolean) bubble.invoke(listener, p));

        p.getWorld().getBlockAt(310, 71, 310).setType(Material.AIR);
        p.getWorld().getBlockAt(310, 69, 310).setType(Material.BUBBLE_COLUMN);
        assertTrue((boolean) bubble.invoke(listener, p));

        Method flow = MoveListener.class.getDeclaredMethod("isFlowingWaterThatPushes", Block.class);
        flow.setAccessible(true);

        Block nonWater = p.getWorld().getBlockAt(315, 70, 315);
        nonWater.setType(Material.STONE);
        assertFalse((boolean) flow.invoke(listener, nonWater));

        Block bubbleBelow = p.getWorld().getBlockAt(316, 70, 316);
        bubbleBelow.setType(Material.WATER);
        bubbleBelow.setBlockData(Bukkit.createBlockData("minecraft:water[level=8]"));
        p.getWorld().getBlockAt(316, 69, 316).setType(Material.BUBBLE_COLUMN);
        assertFalse((boolean) flow.invoke(listener, bubbleBelow));

        Block maxLevel = p.getWorld().getBlockAt(317, 70, 317);
        maxLevel.setType(Material.WATER);
        maxLevel.setBlockData(Bukkit.createBlockData("minecraft:water[level=8]"));
        p.getWorld().getBlockAt(317, 69, 317).setType(Material.AIR);
        assertTrue((boolean) flow.invoke(listener, maxLevel));

        Block adjacentHigher = p.getWorld().getBlockAt(318, 70, 318);
        adjacentHigher.setType(Material.WATER);
        adjacentHigher.setBlockData(Bukkit.createBlockData("minecraft:water[level=2]"));
        p.getWorld().getBlockAt(319, 70, 318).setType(Material.WATER);
        p.getWorld().getBlockAt(319, 70, 318).setBlockData(Bukkit.createBlockData("minecraft:water[level=5]"));
        assertTrue((boolean) flow.invoke(listener, adjacentHigher));
    }

    @Test
    void onMove_anti_infinite_water_and_zone_cache_branches() throws Exception {
        MoveListener listener = new MoveListener(ezafk);
        server.getPluginManager().registerEvents(listener, plugin);
        Player p = server.addPlayer("MoveOnMoveBranches");

        ezafk.getConfig().set("afk.anti.infinite-vehicle", false);
        ezafk.getConfig().set("afk.anti.bubble-column", false);
        ezafk.getConfig().set("afk.anti.infinite-waterflow", true);

        p.teleport(new Location(p.getWorld(), 330, 70, 330));
        p.getWorld().getBlockAt(330, 70, 330).setType(Material.WATER);
        p.getWorld().getBlockAt(330, 70, 330).setBlockData(Bukkit.createBlockData("minecraft:water[level=8]"));
        p.getWorld().getBlockAt(330, 69, 330).setType(Material.AIR);

        Method infiniteFlow = MoveListener.class.getDeclaredMethod("isInInfiniteWaterFlow", Player.class);
        infiniteFlow.setAccessible(true);
        assertTrue((boolean) infiniteFlow.invoke(listener, p));

        ezafk.getConfig().set("afk.anti.flag-only", true);
        server.getPluginManager().callEvent(new PlayerMoveEvent(p, p.getLocation().clone(), p.getLocation().clone().add(1, 0, 0)));

        // Use another player so handled-set branches execute as fresh adds in non-flag mode.
        Player p2 = server.addPlayer("MoveOnMoveBranches2");
        p2.teleport(new Location(p2.getWorld(), 331, 70, 331));
        p2.getWorld().getBlockAt(331, 70, 331).setType(Material.WATER);
        p2.getWorld().getBlockAt(331, 70, 331).setBlockData(Bukkit.createBlockData("minecraft:water[level=8]"));
        p2.getWorld().getBlockAt(331, 69, 331).setType(Material.AIR);

        ezafk.getConfig().set("afk.anti.flag-only", false);
        server.getPluginManager().callEvent(new PlayerMoveEvent(p2, p2.getLocation().clone(), p2.getLocation().clone().add(1, 0, 0)));

        // Deterministic zone transition coverage by injecting one zone and driving enter/same/exit states.
        Field zonesField = AfkZoneManager.class.getDeclaredField("zones");
        zonesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<com.gyvex.ezafk.zone.Zone> zones = (List<com.gyvex.ezafk.zone.Zone>) zonesField.get(null);
        zones.clear();
        zones.add(new com.gyvex.ezafk.zone.Zone("Injected", p.getWorld().getName(), 340, 60, 340, 350, 80, 350,
                false, 0L, 0, 0D, "economy", null, null, 1, 0, 0L, false, List.of(), "", 0));

        Field cacheField = MoveListener.class.getDeclaredField("playerZoneCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, String> zoneCache = (Map<UUID, String>) cacheField.get(null);
        zoneCache.clear();

        Method checkTransition = MoveListener.class.getDeclaredMethod("checkZoneTransition", Player.class);
        checkTransition.setAccessible(true);

        p.teleport(new Location(p.getWorld(), 345, 70, 345));
        checkTransition.invoke(listener, p); // enter
        checkTransition.invoke(listener, p); // same-zone

        p.teleport(new Location(p.getWorld(), 360, 70, 360));
        checkTransition.invoke(listener, p); // exit
    }
}
