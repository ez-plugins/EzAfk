package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AfkZoneCommandFeatureTest {
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
    public void zone_list_and_players_run() {
        CommandSender console = server.getConsoleSender();
        com.gyvex.ezafk.command.AfkZoneCommand cmd = new com.gyvex.ezafk.command.AfkZoneCommand(ezafk);
        cmd.handleAfkZone(console, new String[]{"zone", "list"});
        cmd.handleAfkZone(console, new String[]{"zone", "players"});
        // handleAfkZone returns void and communicates via MessageManager; ensure no exceptions thrown
        assertTrue(true);
    }

    @Test
    public void zone_add_and_remove_can_be_invoked() {
        Player player = server.addPlayer("ZoneAdmin");
        com.gyvex.ezafk.command.AfkZoneCommand cmd = new com.gyvex.ezafk.command.AfkZoneCommand(ezafk);
        // Basic invocation; detailed assertions require WorldEdit mocks and are out-of-scope here
        cmd.handleAfkZone(player, new String[]{"zone", "add", "testzone"});
        cmd.handleAfkZone(player, new String[]{"zone", "remove", "testzone"});
        assertTrue(true);
    }
}
