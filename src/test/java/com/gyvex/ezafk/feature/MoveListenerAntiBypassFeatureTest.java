package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.listener.MoveListener;
import com.gyvex.ezafk.state.AfkActivationMode;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

public class MoveListenerAntiBypassFeatureTest {

    private ServerMock server;
    private EzAfk ezafk;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        ezafk = (EzAfk) TestHelpers.loadPlugin();
        server.getPluginManager().registerEvents(new MoveListener(ezafk), ezafk);
    }

    @AfterEach
    public void tearDown() {
        AfkState.afkPlayers.clear();
        AfkState.clearBypass();
        TestHelpers.stopServer();
    }

    @Test
    public void head_only_rotation_does_not_clear_afk() {
        Player p = server.addPlayer("HeadRotPlayer");
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);
        assertTrue(AfkState.isAfk(p.getUniqueId()));

        // Same XYZ position, only yaw/pitch differs — distanceSquared = 0, below threshold
        Location from = p.getLocation().clone();
        Location to = from.clone();
        to.setYaw(from.getYaw() + 45f);
        to.setPitch(from.getPitch() + 15f);

        server.getPluginManager().callEvent(new PlayerMoveEvent(p, from, to));

        assertTrue(AfkState.isAfk(p.getUniqueId()), "Head rotation should NOT clear AFK status");
    }

    @Test
    public void movement_below_threshold_does_not_clear_afk() {
        Player p = server.addPlayer("TinyMovePlayer");
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);
        assertTrue(AfkState.isAfk(p.getUniqueId()));

        // distanceSquared = 0.001^2 + 0.001^2 = 2e-6 < 1e-4 threshold
        Location from = p.getLocation().clone();
        Location to = from.clone().add(0.001, 0.0, 0.001);

        server.getPluginManager().callEvent(new PlayerMoveEvent(p, from, to));

        assertTrue(AfkState.isAfk(p.getUniqueId()), "Tiny movement below threshold should NOT clear AFK");
    }

    @Test
    public void significant_movement_clears_afk() {
        Player p = server.addPlayer("SigMovePlayer");
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);
        assertTrue(AfkState.isAfk(p.getUniqueId()));

        // distanceSquared = 1.0 >> 1e-4 threshold
        Location from = p.getLocation().clone();
        Location to = from.clone().add(1.0, 0.0, 0.0);

        server.getPluginManager().callEvent(new PlayerMoveEvent(p, from, to));

        assertFalse(AfkState.isAfk(p.getUniqueId()), "Significant movement should clear AFK");
    }

    @Test
    public void movement_does_not_affect_non_afk_player() {
        Player p = server.addPlayer("NotAfkMover");
        assertFalse(AfkState.isAfk(p.getUniqueId()));

        Location from = p.getLocation().clone();
        Location to = from.clone().add(1.0, 0.0, 0.0);

        assertDoesNotThrow(() ->
            server.getPluginManager().callEvent(new PlayerMoveEvent(p, from, to)),
            "Moving while not AFK should not throw"
        );

        assertFalse(AfkState.isAfk(p.getUniqueId()));
    }
}
