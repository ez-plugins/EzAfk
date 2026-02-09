package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.listener.MoveListener;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkActivationMode;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MoveListenerFeatureTest {
    private org.mockbukkit.mockbukkit.ServerMock server;
    private JavaPlugin plugin;
    private com.gyvex.ezafk.EzAfk ezafk;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
        ezafk = (com.gyvex.ezafk.EzAfk) plugin;
        // ensure listener is registered in the test environment
        server.getPluginManager().registerEvents(new MoveListener(ezafk), ezafk);
        // ensure movement detection thresholds are default
    }

    @AfterEach
    public void tearDown() {
        TestHelpers.stopServer();
        AfkState.clearBypass();
    }

    @Test
    public void moving_disables_afk_state() {
        Player p = server.addPlayer("Mover");

        // mark player as AFK
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);
        assertTrue(AfkState.isAfk(p.getUniqueId()));

        // fire a PlayerMoveEvent with a meaningful position change
        Location from = p.getLocation();
        Location to = from.clone().add(1.0, 0.0, 0.0);
        server.getPluginManager().callEvent(new PlayerMoveEvent(p, from, to));

        // After the move event, AFK should be disabled
        assertFalse(AfkState.isAfk(p.getUniqueId()));
    }
}
