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

public class EzAfkMoreCommandsFeatureTest {
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
    public void gui_op_player_can_open() {
        Player p = server.addPlayer("GuiOp");
        p.setOp(true);
        com.gyvex.ezafk.command.EzAfkCommand cmd = new com.gyvex.ezafk.command.EzAfkCommand(ezafk);
        assertTrue(cmd.onCommand(p, null, "afk", new String[]{"gui"}));
    }

    @Test
    public void toggle_other_by_console() {
        Player target = server.addPlayer("TargetUser");
        CommandSender console = server.getConsoleSender();
        com.gyvex.ezafk.command.EzAfkCommand cmd = new com.gyvex.ezafk.command.EzAfkCommand(ezafk);
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"toggle", "TargetUser"}));
        assertTrue(AfkState.isAfk(target.getUniqueId()));
    }

    @Test
    public void time_reset_and_aliases() {
        Player target = server.addPlayer("TimeTarget");
        CommandSender console = server.getConsoleSender();
        com.gyvex.ezafk.command.EzAfkCommand cmd = new com.gyvex.ezafk.command.EzAfkCommand(ezafk);
        // reset via /afk time reset <player>
        assertTrue(cmd.onCommand(console, null, "afk", new String[]{"time", "reset", "TimeTarget"}));
        // afktime alias (label)
        assertTrue(cmd.onCommand(console, null, "afktime", new String[]{"TimeTarget"}));
        // afktop alias
        assertTrue(cmd.onCommand(console, null, "afktop", new String[]{}));
    }
}
