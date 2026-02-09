package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AfkZoneMoreFeatureTest {
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
    public void pos1_pos2_and_clearpos_and_reset_do_not_throw() {
        Player admin = server.addPlayer("ZoneAdmin2");
        admin.setOp(true);
        com.gyvex.ezafk.command.AfkZoneCommand cmd = new com.gyvex.ezafk.command.AfkZoneCommand(ezafk);

        // set pos1/pos2
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "pos1"}));
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "pos2"}));

        // clear own pos
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "clearpos"}));

        // reset counts for a player (requires the player be online)
        assertDoesNotThrow(() -> cmd.handleAfkZone(admin, new String[]{"zone", "reset", "ZoneAdmin2"}));
    }
}
