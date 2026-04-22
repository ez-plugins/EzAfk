package com.gyvex.ezafk.zone;

import com.gyvex.ezafk.TestHelpers;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ZoneCacheTest {

    private ServerMock server;
    private World world;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        TestHelpers.loadPlugin();
        world = server.addSimpleWorld("world");
        ZoneCache.zonePos1.clear();
        ZoneCache.zonePos2.clear();
        ZoneCache.zonePos1Time.clear();
        ZoneCache.zonePos2Time.clear();
    }

    @AfterEach
    public void tearDown() {
        ZoneCache.zonePos1.clear();
        ZoneCache.zonePos2.clear();
        ZoneCache.zonePos1Time.clear();
        ZoneCache.zonePos2Time.clear();
        TestHelpers.stopServer();
    }

    @Test
    public void clearPositions_removes_all_four_maps_for_player() {
        UUID id = UUID.randomUUID();
        Location loc = new Location(world, 0, 64, 0);

        ZoneCache.zonePos1.put(id, loc);
        ZoneCache.zonePos2.put(id, loc);
        ZoneCache.zonePos1Time.put(id, System.currentTimeMillis());
        ZoneCache.zonePos2Time.put(id, System.currentTimeMillis());

        ZoneCache.clearPositions(id);

        assertFalse(ZoneCache.zonePos1.containsKey(id));
        assertFalse(ZoneCache.zonePos2.containsKey(id));
        assertFalse(ZoneCache.zonePos1Time.containsKey(id));
        assertFalse(ZoneCache.zonePos2Time.containsKey(id));
    }

    @Test
    public void clearPositions_only_removes_the_target_player() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Location loc = new Location(world, 0, 64, 0);

        ZoneCache.zonePos1.put(id1, loc);
        ZoneCache.zonePos1.put(id2, loc);

        ZoneCache.clearPositions(id1);

        assertFalse(ZoneCache.zonePos1.containsKey(id1));
        assertTrue(ZoneCache.zonePos1.containsKey(id2), "Other player's pos1 should remain");
    }

    @Test
    public void cleanupExpiredPositions_removes_expired_entries() {
        UUID expiredId = UUID.randomUUID();
        UUID freshId = UUID.randomUUID();
        Location loc = new Location(world, 0, 64, 0);

        long now = System.currentTimeMillis();
        long expiredTime = now - 10_000L; // 10 seconds ago
        long freshTime = now - 500L;       // 0.5 seconds ago

        ZoneCache.zonePos1.put(expiredId, loc);
        ZoneCache.zonePos1Time.put(expiredId, expiredTime);

        ZoneCache.zonePos1.put(freshId, loc);
        ZoneCache.zonePos1Time.put(freshId, freshTime);

        // Expire anything older than 5 seconds
        ZoneCache.cleanupExpiredPositions(5_000L);

        assertFalse(ZoneCache.zonePos1.containsKey(expiredId), "Expired pos1 should be removed");
        assertTrue(ZoneCache.zonePos1.containsKey(freshId), "Fresh pos1 should remain");
    }

    @Test
    public void cleanupExpiredPositions_removes_expired_pos2_entries() {
        UUID expiredId = UUID.randomUUID();
        UUID freshId = UUID.randomUUID();
        Location loc = new Location(world, 0, 64, 0);

        long now = System.currentTimeMillis();
        ZoneCache.zonePos2.put(expiredId, loc);
        ZoneCache.zonePos2Time.put(expiredId, now - 10_000L);

        ZoneCache.zonePos2.put(freshId, loc);
        ZoneCache.zonePos2Time.put(freshId, now - 500L);

        ZoneCache.cleanupExpiredPositions(5_000L);

        assertFalse(ZoneCache.zonePos2.containsKey(expiredId), "Expired pos2 should be removed");
        assertTrue(ZoneCache.zonePos2.containsKey(freshId), "Fresh pos2 should remain");
    }
}
