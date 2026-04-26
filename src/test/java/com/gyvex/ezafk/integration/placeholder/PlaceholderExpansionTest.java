package com.gyvex.ezafk.integration.placeholder;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.state.AfkActivationMode;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkState;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

public class PlaceholderExpansionTest {

    private ServerMock server;
    private EzAfk ezafk;
    private EzAfkPlaceholderExpansion expansion;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        ezafk = (EzAfk) TestHelpers.loadPlugin();
        expansion = new EzAfkPlaceholderExpansion();
    }

    @AfterEach
    public void tearDown() {
        AfkState.afkPlayers.clear();
        TestHelpers.stopServer();
    }

    @Test
    public void identifier_is_ezafk() {
        assertEquals("ezafk", expansion.getIdentifier());
    }

    @Test
    public void empty_params_returns_empty_string() {
        Player p = server.addPlayer("EmptyParamPlayer");
        assertEquals("", expansion.onRequest(p, ""));
    }

    @Test
    public void null_params_returns_empty_string() {
        Player p = server.addPlayer("NullParamPlayer");
        assertEquals("", expansion.onRequest(p, null));
    }

    @Test
    public void status_returns_AFK_when_player_is_afk() {
        Player p = server.addPlayer("StatusAfkPlayer");
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);

        assertEquals("AFK", expansion.onRequest(p, "status"));
    }

    @Test
    public void status_returns_ACTIVE_when_player_is_not_afk() {
        Player p = server.addPlayer("StatusActivePlayer");
        assertFalse(AfkState.isAfk(p.getUniqueId()));

        assertEquals("ACTIVE", expansion.onRequest(p, "status"));
    }

    @Test
    public void status_colored_contains_afk_marker_when_afk() {
        Player p = server.addPlayer("ColoredAfkPlayer");
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);

        String result = expansion.onRequest(p, "status_colored");
        assertTrue(result.contains("AFK"), "status_colored should contain AFK for AFK player");
    }

    @Test
    public void status_colored_contains_active_marker_when_not_afk() {
        Player p = server.addPlayer("ColoredActivePlayer");

        String result = expansion.onRequest(p, "status_colored");
        assertTrue(result.contains("ACTIVE"), "status_colored should contain ACTIVE for active player");
    }

    @Test
    public void afk_count_reflects_number_of_afk_players() {
        Player p1 = server.addPlayer("AfkCountP1");
        Player p2 = server.addPlayer("AfkCountP2");
        AfkState.markAfk(ezafk, p1, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);
        AfkState.markAfk(ezafk, p2, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);

        assertEquals("2", expansion.onRequest(null, "afk_count"));
    }

    @Test
    public void afk_players_alias_equals_afk_count() {
        Player p = server.addPlayer("AfkPlayersP1");
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);

        assertEquals(
            expansion.onRequest(null, "afk_count"),
            expansion.onRequest(null, "afk_players"),
            "afk_players should be an alias for afk_count"
        );
    }

    @Test
    public void active_count_reflects_online_minus_afk() {
        Player p1 = server.addPlayer("ActiveCountP1");
        Player p2 = server.addPlayer("ActiveCountP2");

        int online = server.getOnlinePlayers().size();
        AfkState.markAfk(ezafk, p1, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);

        String result = expansion.onRequest(null, "active_count");
        assertEquals(String.valueOf(online - 1), result);
    }

    @Test
    public void active_players_alias_equals_active_count() {
        server.addPlayer("ActivePlayersP1");

        assertEquals(
            expansion.onRequest(null, "active_count"),
            expansion.onRequest(null, "active_players"),
            "active_players should be an alias for active_count"
        );
    }

    @Test
    public void since_returns_empty_when_player_not_afk() {
        Player p = server.addPlayer("SinceNotAfkPlayer");
        // getSecondsSinceAfk returns -1 when not AFK -> formatDurationSeconds returns ""
        assertEquals("", expansion.onRequest(p, "since"));
    }

    @Test
    public void since_returns_non_empty_when_player_is_afk() {
        Player p = server.addPlayer("SinceAfkPlayer");
        AfkState.markAfk(ezafk, p, AfkReason.MANUAL, null, AfkActivationMode.STANDARD);

        String result = expansion.onRequest(p, "since");
        assertFalse(result.isEmpty(), "since should be non-empty for AFK player");
    }

    @Test
    public void unknown_placeholder_returns_empty_string() {
        Player p = server.addPlayer("UnknownParamPlayer");
        assertEquals("", expansion.onRequest(p, "nonexistent_placeholder"));
    }
}
