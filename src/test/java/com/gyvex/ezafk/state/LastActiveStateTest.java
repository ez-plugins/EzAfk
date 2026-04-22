package com.gyvex.ezafk.state;

import com.gyvex.ezafk.TestHelpers;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LastActiveStateTest {

    private ServerMock server;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        TestHelpers.loadPlugin();
    }

    @AfterEach
    public void tearDown() {
        LastActiveState.lastActive.clear();
        TestHelpers.stopServer();
    }

    @Test
    public void getLastActive_returns_current_time_for_unknown_player() {
        UUID unknown = UUID.randomUUID();
        long before = System.currentTimeMillis();
        long result = LastActiveState.getLastActive(unknown);
        long after = System.currentTimeMillis();

        assertTrue(result >= before && result <= after,
            "getLastActive for unknown player should return approximately current time");
    }

    @Test
    public void update_stores_current_time_for_player() {
        Player p = server.addPlayer("UpdatePlayer");
        long before = System.currentTimeMillis();
        LastActiveState.update(p);
        long after = System.currentTimeMillis();

        long stored = LastActiveState.getLastActive(p.getUniqueId());
        assertTrue(stored >= before && stored <= after,
            "update should store approximately current time");
    }

    @Test
    public void getSecondsSinceLastActive_returns_zero_or_more_after_update() {
        Player p = server.addPlayer("SecondsSincePlayer");
        LastActiveState.update(p);

        long seconds = LastActiveState.getSecondsSinceLastActive(p.getUniqueId());
        assertTrue(seconds >= 0L, "getSecondsSinceLastActive should return 0 or more after update");
    }

    @Test
    public void getSecondsSinceLastActive_returns_zero_for_unknown_player() {
        // Unknown player gets current time as default, so diff ≈ 0
        UUID unknown = UUID.randomUUID();
        long seconds = LastActiveState.getSecondsSinceLastActive(unknown);
        assertTrue(seconds >= 0L, "getSecondsSinceLastActive should be non-negative");
    }

    @Test
    public void update_overwrites_previous_timestamp() throws InterruptedException {
        Player p = server.addPlayer("OverwritePlayer");
        LastActiveState.lastActive.put(p.getUniqueId(), 1000L);

        LastActiveState.update(p);

        assertTrue(LastActiveState.getLastActive(p.getUniqueId()) > 1000L,
            "update should overwrite the old timestamp with a newer one");
    }

    @Test
    public void getLastActive_with_player_overload_matches_uuid_overload() {
        Player p = server.addPlayer("OverloadPlayer");
        LastActiveState.update(p);

        assertEquals(
            LastActiveState.getLastActive(p),
            LastActiveState.getLastActive(p.getUniqueId()),
            "getLastActive(Player) and getLastActive(UUID) should return the same value"
        );
    }
}
