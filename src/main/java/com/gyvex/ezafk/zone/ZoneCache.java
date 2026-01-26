package com.gyvex.ezafk.zone;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ZoneCache {
    public static final Map<UUID, Location> zonePos1 = new HashMap<>();
    public static final Map<UUID, Location> zonePos2 = new HashMap<>();
    public static final Map<UUID, Long> zonePos1Time = new HashMap<>();
    public static final Map<UUID, Long> zonePos2Time = new HashMap<>();

    public static void clearPositions(UUID playerId) {
        zonePos1.remove(playerId);
        zonePos2.remove(playerId);
        zonePos1Time.remove(playerId);
        zonePos2Time.remove(playerId);
    }

    public static void cleanupExpiredPositions(long expiryMillis) {
        long now = System.currentTimeMillis();
        List<UUID> toRemove = new java.util.ArrayList<>();

        for (Map.Entry<UUID, Long> e : zonePos1Time.entrySet()) {
            if (now - e.getValue() > expiryMillis) toRemove.add(e.getKey());
        }
        for (UUID id : toRemove) {
            zonePos1.remove(id);
            zonePos1Time.remove(id);
        }

        toRemove.clear();
        for (Map.Entry<UUID, Long> e : zonePos2Time.entrySet()) {
            if (now - e.getValue() > expiryMillis) toRemove.add(e.getKey());
        }
        for (UUID id : toRemove) {
            zonePos2.remove(id);
            zonePos2Time.remove(id);
        }
    }
}
