package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.zone.ZoneCache;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerQuitListenerFeatureTest {
    private org.mockbukkit.mockbukkit.ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
        // ensure PlayerQuitListener is registered so quit events are handled
        server.getPluginManager().registerEvents(new com.gyvex.ezafk.listener.PlayerQuitListener(), plugin);
    }

    @AfterEach
    public void tearDown() {
        TestHelpers.stopServer();
    }

    @Test
    public void quitting_clears_zone_positions() {
        Player p = server.addPlayer("Quitter");
        UUID id = p.getUniqueId();

        // populate zone cache
        ZoneCache.zonePos1.put(id, p.getLocation());
        ZoneCache.zonePos2.put(id, p.getLocation());
        assertTrue(ZoneCache.zonePos1.containsKey(id));

        // fire quit event
        server.getPluginManager().callEvent(new PlayerQuitEvent(p, "Goodbye"));

        assertFalse(ZoneCache.zonePos1.containsKey(id));
        assertFalse(ZoneCache.zonePos2.containsKey(id));
    }
}
