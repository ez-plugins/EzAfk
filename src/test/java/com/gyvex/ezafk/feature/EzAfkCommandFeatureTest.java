package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.state.AfkState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EzAfkCommandFeatureTest {
    private org.mockbukkit.mockbukkit.ServerMock server;
    private JavaPlugin plugin;
    private com.gyvex.ezafk.EzAfk ezafk;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
        ezafk = (com.gyvex.ezafk.EzAfk) plugin;
    }

    @AfterEach
    public void tearDown() {
        TestHelpers.stopServer();
    }

    @Test
    public void reload_command_from_console() {
        CommandSender console = server.getConsoleSender();
        com.gyvex.ezafk.command.EzAfkCommand cmd = new com.gyvex.ezafk.command.EzAfkCommand(ezafk);
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"reload"}));
    }

    @Test
    public void toggle_self_toggles_afk_state() {
        Player player = server.addPlayer("Alice");
        assertFalse(AfkState.isAfk(player.getUniqueId()));
        com.gyvex.ezafk.command.EzAfkCommand cmd = new com.gyvex.ezafk.command.EzAfkCommand(ezafk);
        assertTrue(cmd.onCommand(player, null, "afk", new String[]{}));
        assertTrue(AfkState.isAfk(player.getUniqueId()));
        assertTrue(cmd.onCommand(player, null, "afk", new String[]{}));
        assertFalse(AfkState.isAfk(player.getUniqueId()));
    }

    @Test
    public void bypass_toggles_bypass_state() {
        Player player = server.addPlayer("BypassUser");
        assertFalse(AfkState.isBypassed(player.getUniqueId()));
        com.gyvex.ezafk.command.EzAfkCommand cmd = new com.gyvex.ezafk.command.EzAfkCommand(ezafk);
        // Use console as manager to have permission
        CommandSender console = server.getConsoleSender();
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass", "BypassUser"}));
        assertTrue(AfkState.isBypassed(player.getUniqueId()));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"bypass", "BypassUser"}));
        assertFalse(AfkState.isBypassed(player.getUniqueId()));
    }

    @Test
    public void info_and_time_commands_run() {
        Player player = server.addPlayer("InfoUser");
        com.gyvex.ezafk.command.EzAfkCommand cmd = new com.gyvex.ezafk.command.EzAfkCommand(ezafk);
        CommandSender console = server.getConsoleSender();
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"info", "InfoUser"}));
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"time", "InfoUser"}));
    }

    @Test
    public void top_command_runs() {
        CommandSender console = server.getConsoleSender();
        com.gyvex.ezafk.command.EzAfkCommand cmd = new com.gyvex.ezafk.command.EzAfkCommand(ezafk);
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"top"}));
    }
}
