package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.command.AfkZoneCommand;
import com.gyvex.ezafk.command.EzAfkCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPermissionCoverageFeatureTest {

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
    void ezAfkCommand_permission_denied_paths() {
        EzAfkCommand cmd = new EzAfkCommand(ezafk);
        Player user = server.addPlayer("NoPermUser");
        org.bukkit.command.CommandSender console = server.getConsoleSender();

        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"reload"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"gui"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"toggle"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"bypass", "NoPermUser"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"info", "NoPermUser"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"time", "OtherUser"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"time", "reset", "OtherUser"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"top"}));
        assertTrue(cmd.onCommand(user, null, "afktime", new String[]{}));

        // afktime players-only branch and unknown-player fallback path.
        assertTrue(cmd.onCommand(console, null, "afktime", new String[]{}));
        assertTrue(cmd.onCommand(console, null, "afktime", new String[]{"DefinitelyUnknownPlayer"}));

        // Self-initiated toggle path.
        user.setOp(true);
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"toggle"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"toggle", user.getName()}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"toggle", user.getName()}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"bypass", "DefinitelyUnknown"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"bypass", "whitelist"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"bypass", "whitelist", "add"}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"bypass", "whitelist", "noop", user.getName()}));
        assertTrue(cmd.onCommand(user, null, "afk", new String[]{"info", "DefinitelyUnknown"}));
    }

    @Test
    void afkZoneCommand_permission_and_player_only_paths() {
        AfkZoneCommand cmd = new AfkZoneCommand(ezafk);
        Player user = server.addPlayer("NoZonePerm");
        CommandSender console = server.getConsoleSender();

        assertDoesNotThrow(() -> cmd.handleAfkZone(user, new String[]{"zone", "list"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(user, new String[]{"zone", "players"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(user, new String[]{"zone", "add", "z1"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone", "pos1"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone", "pos2"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(console, new String[]{"zone", "clearpos"}));
    }
}
