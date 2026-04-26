package com.gyvex.ezafk.zone;

import com.gyvex.ezafk.TestHelpers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

public class ZoneContainsTest {

    private ServerMock server;
    private World world;
    private Zone zone;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        TestHelpers.loadPlugin();
        world = server.addSimpleWorld("world");
        // Zone bounds: x=[0,10], y=[60,80], z=[0,10]
        zone = new Zone(
            "TestZone", world.getName(),
            0, 60, 0, 10, 80, 10,
            false, 60, -1, 0, "economy", null, null, 1, -1, 0
        );
    }

    @AfterEach
    public void tearDown() {
        TestHelpers.stopServer();
    }

    @Test
    public void player_inside_bounds_returns_true() {
        Player p = server.addPlayer("InsidePlayer");
        p.teleport(new Location(world, 5, 70, 5));
        assertTrue(zone.contains(p), "Player at center of zone should be inside");
    }

    @Test
    public void player_on_min_boundary_returns_true() {
        Player p = server.addPlayer("MinBoundPlayer");
        p.teleport(new Location(world, 0, 60, 0));
        assertTrue(zone.contains(p), "Player on minimum boundary should be inside");
    }

    @Test
    public void player_on_max_boundary_returns_true() {
        Player p = server.addPlayer("MaxBoundPlayer");
        p.teleport(new Location(world, 10, 80, 10));
        assertTrue(zone.contains(p), "Player on maximum boundary should be inside");
    }

    @Test
    public void player_outside_x_returns_false() {
        Player p = server.addPlayer("OutsideXPlayer");
        p.teleport(new Location(world, 11, 70, 5));
        assertFalse(zone.contains(p), "Player outside X bound should not be inside");
    }

    @Test
    public void player_outside_y_returns_false() {
        Player p = server.addPlayer("OutsideYPlayer");
        p.teleport(new Location(world, 5, 55, 5));
        assertFalse(zone.contains(p), "Player outside Y bound (below) should not be inside");
    }

    @Test
    public void player_outside_z_returns_false() {
        Player p = server.addPlayer("OutsideZPlayer");
        p.teleport(new Location(world, 5, 70, 11));
        assertFalse(zone.contains(p), "Player outside Z bound should not be inside");
    }

    @Test
    public void player_in_different_world_returns_false() {
        World otherWorld = server.addSimpleWorld("nether");
        Zone worldZone = new Zone(
            "WorldZone", "overworld",
            0, 60, 0, 10, 80, 10,
            false, 60, -1, 0, "economy", null, null, 1, -1, 0
        );
        Player p = server.addPlayer("OtherWorldPlayer");
        p.teleport(new Location(otherWorld, 5, 70, 5));
        assertFalse(worldZone.contains(p), "Player in wrong world should not be inside zone");
    }
}
