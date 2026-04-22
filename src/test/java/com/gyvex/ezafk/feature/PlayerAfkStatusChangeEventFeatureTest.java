package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.event.PlayerAfkStatusChangeEvent;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.state.ToggleResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerAfkStatusChangeEventFeatureTest {

    private ServerMock server;
    private EzAfk ezafk;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        ezafk = (EzAfk) TestHelpers.loadPlugin();
    }

    @AfterEach
    public void tearDown() {
        AfkState.afkPlayers.clear();
        AfkState.clearBypass();
        TestHelpers.stopServer();
    }

    @Test
    public void event_fires_on_afk_enable_with_isAfk_true() {
        Player p = server.addPlayer("EventEnable");
        AtomicBoolean fired = new AtomicBoolean(false);
        AtomicBoolean capturedIsAfk = new AtomicBoolean(false);

        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onAfkChange(PlayerAfkStatusChangeEvent event) {
                fired.set(true);
                capturedIsAfk.set(event.isAfk());
            }
        }, ezafk);

        AfkState.toggle(ezafk, p);

        assertTrue(fired.get(), "PlayerAfkStatusChangeEvent should fire on AFK enable");
        assertTrue(capturedIsAfk.get(), "isAfk should be true when enabling AFK");
    }

    @Test
    public void event_fires_on_afk_disable_with_isAfk_false() {
        Player p = server.addPlayer("EventDisable");
        AfkState.toggle(ezafk, p);

        AtomicBoolean firedOnDisable = new AtomicBoolean(false);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onAfkChange(PlayerAfkStatusChangeEvent event) {
                if (!event.isAfk()) {
                    firedOnDisable.set(true);
                }
            }
        }, ezafk);

        AfkState.toggle(ezafk, p);

        assertTrue(firedOnDisable.get(), "PlayerAfkStatusChangeEvent should fire on AFK disable with isAfk=false");
        assertFalse(AfkState.isAfk(p.getUniqueId()));
    }

    @Test
    public void cancelling_enable_event_prevents_afk_state() {
        Player p = server.addPlayer("CancelEnable");

        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onAfkChange(PlayerAfkStatusChangeEvent event) {
                if (event.isAfk()) {
                    event.setCancelled(true);
                }
            }
        }, ezafk);

        AfkState.toggle(ezafk, p);

        assertFalse(AfkState.isAfk(p.getUniqueId()), "Player should not be AFK when enable event is cancelled");
    }

    @Test
    public void cancelling_disable_event_keeps_player_afk() {
        Player p = server.addPlayer("CancelDisable");
        AfkState.toggle(ezafk, p);
        assertTrue(AfkState.isAfk(p.getUniqueId()));

        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onAfkChange(PlayerAfkStatusChangeEvent event) {
                if (!event.isAfk()) {
                    event.setCancelled(true);
                }
            }
        }, ezafk);

        AfkState.toggle(ezafk, p);

        assertTrue(AfkState.isAfk(p.getUniqueId()), "Player should remain AFK when disable event is cancelled");
    }

    @Test
    public void event_has_correct_player_reference() {
        Player p = server.addPlayer("EventPlayer");
        AtomicReference<Player> capturedPlayer = new AtomicReference<>();

        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onAfkChange(PlayerAfkStatusChangeEvent event) {
                if (event.isAfk()) {
                    capturedPlayer.set(event.getPlayer());
                }
            }
        }, ezafk);

        AfkState.toggle(ezafk, p);

        assertEquals(p, capturedPlayer.get(), "Event's getPlayer() should return the toggling player");
    }

    @Test
    public void event_reason_is_MANUAL_for_toggle() {
        Player p = server.addPlayer("ReasonPlayer");
        AtomicReference<AfkReason> capturedReason = new AtomicReference<>();

        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onAfkChange(PlayerAfkStatusChangeEvent event) {
                if (event.isAfk()) {
                    capturedReason.set(event.getReason());
                }
            }
        }, ezafk);

        AfkState.toggle(ezafk, p);

        assertEquals(AfkReason.MANUAL, capturedReason.get(), "Toggle reason should be AfkReason.MANUAL");
    }

    @Test
    public void toggle_returns_NOW_AFK_when_event_not_cancelled() {
        Player p = server.addPlayer("NotCancelledPlayer");
        ToggleResult result = AfkState.toggle(ezafk, p);
        assertEquals(ToggleResult.NOW_AFK, result);
    }
}
