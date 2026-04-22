package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.listener.PlayerQuitListener;
import com.gyvex.ezafk.state.AfkActivationMode;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.zone.ZoneCache;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerQuitAfkFlushTest {

    private ServerMock server;
    private EzAfk ezafk;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        ezafk = (EzAfk) TestHelpers.loadPlugin();
        server.getPluginManager().registerEvents(new PlayerQuitListener(), ezafk);
    }

    @AfterEach
    public void tearDown() {
        AfkState.afkPlayers.clear();
        TestHelpers.stopServer();
    }

    @Test
    public void afk_player_is_removed_from_afk_state_on_quit() {
        Player p = server.addPlayer("AfkQuitter");
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);
        assertTrue(AfkState.isAfk(p.getUniqueId()));

        server.getPluginManager().callEvent(new PlayerQuitEvent(p, "Bye"));

        assertFalse(AfkState.isAfk(p.getUniqueId()),
            "AFK player should be removed from AFK state on quit");
    }

    @Test
    public void quit_clears_zone_cache_positions() {
        Player p = server.addPlayer("ZoneQuitter");
        UUID id = p.getUniqueId();

        ZoneCache.zonePos1.put(id, p.getLocation());
        ZoneCache.zonePos2.put(id, p.getLocation());

        server.getPluginManager().callEvent(new PlayerQuitEvent(p, "Bye"));

        assertFalse(ZoneCache.zonePos1.containsKey(id), "zonePos1 should be cleared on quit");
        assertFalse(ZoneCache.zonePos2.containsKey(id), "zonePos2 should be cleared on quit");
    }

    @Test
    public void non_afk_player_quit_does_not_throw() {
        Player p = server.addPlayer("NonAfkQuitter");
        assertFalse(AfkState.isAfk(p.getUniqueId()));

        assertDoesNotThrow(() ->
            server.getPluginManager().callEvent(new PlayerQuitEvent(p, "Bye")),
            "Non-AFK player quit should not throw"
        );
    }
}
