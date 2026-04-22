package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.state.AfkActivationMode;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.state.ToggleResult;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

public class AfkStateFeatureTest {

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
    public void toggle_returns_NOW_AFK_on_first_call() {
        Player p = server.addPlayer("TogglePlayer");
        ToggleResult result = AfkState.toggle(ezafk, p);
        assertEquals(ToggleResult.NOW_AFK, result);
        assertTrue(AfkState.isAfk(p.getUniqueId()));
    }

    @Test
    public void toggle_returns_NO_LONGER_AFK_on_second_call() {
        Player p = server.addPlayer("TogglePlayer2");
        AfkState.toggle(ezafk, p);
        ToggleResult result = AfkState.toggle(ezafk, p);
        assertEquals(ToggleResult.NO_LONGER_AFK, result);
        assertFalse(AfkState.isAfk(p.getUniqueId()));
    }

    @Test
    public void afkPlayerCount_updates_on_toggle() {
        Player p1 = server.addPlayer("CountP1");
        Player p2 = server.addPlayer("CountP2");

        assertEquals(0, AfkState.getAfkPlayerCount());
        AfkState.toggle(ezafk, p1);
        assertEquals(1, AfkState.getAfkPlayerCount());
        AfkState.toggle(ezafk, p2);
        assertEquals(2, AfkState.getAfkPlayerCount());
        AfkState.toggle(ezafk, p1);
        assertEquals(1, AfkState.getAfkPlayerCount());
    }

    @Test
    public void activePlayerCount_returns_online_minus_afk() {
        Player p1 = server.addPlayer("ActiveP1");
        server.addPlayer("ActiveP2");

        int online = server.getOnlinePlayers().size();
        assertEquals(online, AfkState.getActivePlayerCount());

        AfkState.toggle(ezafk, p1);
        assertEquals(online - 1, AfkState.getActivePlayerCount());
    }

    @Test
    public void getAfkStartTime_is_zero_before_afk() {
        Player p = server.addPlayer("StartTimePlayer");
        assertEquals(0L, AfkState.getAfkStartTime(p.getUniqueId()));
    }

    @Test
    public void getAfkStartTime_is_positive_when_afk() {
        Player p = server.addPlayer("StartTimeAfkPlayer");
        AfkState.toggle(ezafk, p);
        assertTrue(AfkState.getAfkStartTime(p.getUniqueId()) > 0L);
    }

    @Test
    public void getSecondsSinceAfk_is_negative_when_not_afk() {
        Player p = server.addPlayer("SecondsSincePlayer");
        assertEquals(-1L, AfkState.getSecondsSinceAfk(p.getUniqueId()));
    }

    @Test
    public void getSecondsSinceAfk_is_non_negative_when_afk() {
        Player p = server.addPlayer("SecondsSinceAfkPlayer");
        AfkState.toggle(ezafk, p);
        assertTrue(AfkState.getSecondsSinceAfk(p.getUniqueId()) >= 0L);
    }

    @Test
    public void toggleBypass_adds_and_removes_bypass() {
        Player p = server.addPlayer("BypassPlayer");
        assertFalse(AfkState.isBypassed(p.getUniqueId()));

        AfkState.toggleBypass(p.getUniqueId());
        assertTrue(AfkState.isBypassed(p.getUniqueId()));

        AfkState.toggleBypass(p.getUniqueId());
        assertFalse(AfkState.isBypassed(p.getUniqueId()));
    }

    @Test
    public void clearBypass_removes_all_bypasses() {
        Player p1 = server.addPlayer("ClearByP1");
        Player p2 = server.addPlayer("ClearByP2");
        AfkState.toggleBypass(p1.getUniqueId());
        AfkState.toggleBypass(p2.getUniqueId());

        AfkState.clearBypass();

        assertFalse(AfkState.isBypassed(p1.getUniqueId()));
        assertFalse(AfkState.isBypassed(p2.getUniqueId()));
    }

    @Test
    public void markAfk_SILENT_marks_player_as_afk() {
        Player p = server.addPlayer("SilentAfkPlayer");
        AfkState.markAfk(ezafk, p, AfkReason.INACTIVITY, null, AfkActivationMode.SILENT);
        assertTrue(AfkState.isAfk(p.getUniqueId()));
    }

    @Test
    public void multiple_players_tracked_independently() {
        Player p1 = server.addPlayer("IndepP1");
        Player p2 = server.addPlayer("IndepP2");

        AfkState.toggle(ezafk, p1);
        assertTrue(AfkState.isAfk(p1.getUniqueId()));
        assertFalse(AfkState.isAfk(p2.getUniqueId()));

        AfkState.toggle(ezafk, p2);
        assertTrue(AfkState.isAfk(p1.getUniqueId()));
        assertTrue(AfkState.isAfk(p2.getUniqueId()));
    }

    @Test
    public void isAfk_returns_false_for_unknown_player() {
        assertFalse(AfkState.isAfk(java.util.UUID.randomUUID()));
    }
}
