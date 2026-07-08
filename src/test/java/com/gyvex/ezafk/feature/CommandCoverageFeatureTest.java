package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.command.AfkZoneCommand;
import com.gyvex.ezafk.command.EzAfkCommand;
import com.gyvex.ezafk.command.EzAfkTabCompleter;
import com.gyvex.ezafk.manager.AfkTimeManager;
import com.gyvex.ezafk.state.AfkState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandCoverageFeatureTest {

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
        AfkState.clearBypass();
        TestHelpers.stopServer();
    }

    @Test
    void ezAfkCommand_exercises_error_and_usage_paths() {
        EzAfkCommand cmd = new EzAfkCommand(ezafk);
        CommandSender console = server.getConsoleSender();
        Player player = server.addPlayer("CmdUser");

        assertTrue(cmd.onCommand(console, null, "afk", new String[]{}));
        assertTrue(cmd.onCommand(player, null, "afk", new String[]{"unknown"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"gui"}));
        assertTrue(cmd.onCommand(player, null, "afk", new String[]{"toggle"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"toggle", "NoSuchPlayer"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"info"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"info", "NoSuchPlayer"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"time", "a", "b"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"zone", "list"}));
        assertTrue(cmd.onCommand(console, null, "afkzone", new String[]{"list"}));
    }

    @Test
    void ezAfkCommand_exercises_main_success_paths() {
        EzAfkCommand cmd = new EzAfkCommand(ezafk);
        CommandSender console = server.getConsoleSender();
        Player target = server.addPlayer("TargetOne");
        target.setOp(true);

        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"reload"}));
        assertTrue(cmd.onCommand(target, null, "afk", new String[]{}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"toggle", "TargetOne"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"time", "TargetOne"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"time", "reset", "TargetOne"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"top"}));
        assertTrue(cmd.onCommand(console, null, "afktime", new String[]{"TargetOne"}));
        assertTrue(cmd.onCommand(console, null, "afktop", new String[]{}));

        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass", "whitelist", "add", "TargetOne"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass", "whitelist", "list"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass", "whitelist", "remove", "TargetOne"}));

        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass", "blacklist", "add", "TargetOne"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass", "blacklist", "list"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass", "blacklist", "remove", "TargetOne"}));

        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass", "TargetOne"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"info", "TargetOne"}));

        // Exercise info AFK-detail path and top non-empty leaderboard path.
        AfkState.markAfk(ezafk, target, com.gyvex.ezafk.state.AfkReason.MANUAL, "via test", com.gyvex.ezafk.state.AfkActivationMode.STANDARD);
        AfkTimeManager.recordAfkSession(target.getUniqueId(), System.currentTimeMillis() - 15000L, System.currentTimeMillis());
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"info", "TargetOne"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"top"}));

        // Self-time path with a player sender.
        assertTrue(cmd.onCommand(target, null, "afk", new String[]{"time"}));
    }

    @Test
    void afkZoneCommand_exercises_main_paths() {
        AfkZoneCommand cmd = new AfkZoneCommand(ezafk);
        CommandSender console = server.getConsoleSender();
        Player admin = server.addPlayer("ZoneAdmin");
        admin.setOp(true);

        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone", "list"}));

        // seed a region to exercise list and remove branches
        Map<String, Object> region = new HashMap<>();
        region.put("name", "spawn-afk");
        region.put("world", admin.getWorld().getName());
        region.put("x1", 0);
        region.put("y1", 60);
        region.put("z1", 0);
        region.put("x2", 10);
        region.put("y2", 70);
        region.put("z2", 10);
        Registry.get().getZonesConfig().set("regions", List.of(region));

        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone", "list"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone", "players"}));

        // Force non-empty loaded zone path by creating and loading an in-memory region around admin.
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "add", "live-zone", "0", "60", "0", "20", "90", "20"}));
        admin.teleport(new org.bukkit.Location(admin.getWorld(), 10, 70, 10));
        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone", "players"}));

        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone", "add", "bad"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "add"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "add", "coordsBad", "a", "b", "c", "d", "e", "f"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "add", "coordsOk", "1", "60", "1", "4", "63", "4"}));

        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "remove"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "remove", "missing"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "remove", "coordsOk"}));

        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "pos1"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "pos2"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "clearpos"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "clearpos", "ZoneAdmin"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "clearpos", "NoSuchPlayer"}));

        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "reset"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "reset", "NoSuchPlayer"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "reset", "ZoneAdmin"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "reset", "ZoneAdmin", "spawn-afk"}));

        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone", "unknown"}));
    }

    @Test
    void ezAfkTabCompleter_exercises_all_argument_shapes() {
        EzAfkTabCompleter completer = new EzAfkTabCompleter();
        Player p = server.addPlayer("CompleterUser");
        p.setOp(true);
        server.addPlayer("Beta");

        // zone name suggestions for remove
        Map<String, Object> region = new HashMap<>();
        region.put("name", "alpha-zone");
        Registry.get().getZonesConfig().set("regions", List.of(region));

        assertNotNull(completer.onTabComplete(p, null, "afk", new String[]{"g"}));
        assertNotNull(completer.onTabComplete(p, null, "afkzone", new String[]{"l"}));
        assertNotNull(completer.onTabComplete(p, null, "afk", new String[]{"toggle", "B"}));
        assertNotNull(completer.onTabComplete(p, null, "afk", new String[]{"info", "B"}));
        assertNotNull(completer.onTabComplete(p, null, "afk", new String[]{"time", "B"}));
        assertNotNull(completer.onTabComplete(p, null, "afk", new String[]{"reset", "B"}));
        assertNotNull(completer.onTabComplete(p, null, "afk", new String[]{"remove", "a"}));
        assertNotNull(completer.onTabComplete(p, null, "afk", new String[]{"bypass", "w"}));
        assertNotNull(completer.onTabComplete(p, null, "afk", new String[]{"bypass", "whitelist", "a"}));
        assertNotNull(completer.onTabComplete(p, null, "afk", new String[]{"bypass", "whitelist", "add", "B"}));

        // no-permission path for /afk time <player> suggestions
        p.setOp(false);
        assertTrue(completer.onTabComplete(p, null, "afk", new String[]{"time", "B"}).isEmpty());

        assertFalse(completer.onTabComplete(p, null, "afk", new String[]{"bypass", "B"}).isEmpty());
    }
}
