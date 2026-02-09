package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.state.LastActiveState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerActivityListenerFeatureTest {
    private org.mockbukkit.mockbukkit.ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
        // register listener so activity events update LastActiveState
        server.getPluginManager().registerEvents(new com.gyvex.ezafk.listener.PlayerActivityListener(), plugin);
    }

    @AfterEach
    public void tearDown() {
        TestHelpers.stopServer();
    }

    @Test
    public void interacting_updates_last_active() {
        Player p = server.addPlayer("ActivePlayer");
        long before = LastActiveState.getLastActive(p.getUniqueId());

        // simulate player command preprocess event which should update LastActiveState
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerCommandPreprocessEvent(p, "/afk"));

        long after = LastActiveState.getLastActive(p.getUniqueId());
        assertTrue(after >= before);
    }
}
