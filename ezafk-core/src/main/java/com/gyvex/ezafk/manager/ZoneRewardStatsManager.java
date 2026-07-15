package com.gyvex.ezafk.manager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player, per-zone reward statistics for the current server session.
 *
 * <p>Stats are kept in-memory only; they reset when the server restarts. All
 * mutation methods are intentionally unsynchronised because they are only ever
 * called from the main thread via {@link AfkZoneRewardManager}.
 *
 * <p>Retrieve stats via the typed {@link ZoneRewardStat} snapshot returned by
 * {@link #getStat(UUID, String)}.
 */
public final class ZoneRewardStatsManager {

    private ZoneRewardStatsManager() {}

    /** zone-name → accumulated stats */
    private static final Map<UUID, Map<String, MutableStat>> stats = new HashMap<>();

    /**
     * Immutable snapshot of a player's rewards in one zone for the current
     * session.
     */
    public static final class ZoneRewardStat {
        /** Total number of individual reward grants received. */
        public final long totalGrants;
        /** Total currency deposited (only meaningful for the {@code economy} reward type). */
        public final double totalAmount;

        ZoneRewardStat(long totalGrants, double totalAmount) {
            this.totalGrants = totalGrants;
            this.totalAmount = totalAmount;
        }
    }

    /** Mutable stat bucket used internally. */
    private static final class MutableStat {
        long totalGrants;
        double totalAmount;
    }

    /**
     * Records a reward grant event.
     *
     * @param playerId UUID of the rewarded player.
     * @param zoneName Name of the zone.
     * @param grantCount Number of reward intervals that fired at once.
     * @param amount Total amount deposited (0 for non-economy rewards).
     */
    public static void recordGrant(UUID playerId, String zoneName, int grantCount, double amount) {
        if (playerId == null || zoneName == null) return;
        Map<String, MutableStat> playerMap = stats.computeIfAbsent(playerId, k -> new HashMap<>());
        MutableStat s = playerMap.computeIfAbsent(zoneName, k -> new MutableStat());
        s.totalGrants += grantCount;
        s.totalAmount += amount;
    }

    /**
     * Returns a stat snapshot for the given player and zone, or a zeroed
     * snapshot if no data exists yet.
     */
    public static ZoneRewardStat getStat(UUID playerId, String zoneName) {
        if (playerId == null || zoneName == null) return new ZoneRewardStat(0, 0);
        Map<String, MutableStat> playerMap = stats.get(playerId);
        if (playerMap == null) return new ZoneRewardStat(0, 0);
        MutableStat s = playerMap.get(zoneName);
        if (s == null) return new ZoneRewardStat(0, 0);
        return new ZoneRewardStat(s.totalGrants, s.totalAmount);
    }

    /**
     * Returns an unmodifiable view of all zone stats for the given player.
     * Keys are zone names; values are mutable snapshots at call time.
     */
    public static Map<String, ZoneRewardStat> getAllStats(UUID playerId) {
        if (playerId == null) return Collections.emptyMap();
        Map<String, MutableStat> playerMap = stats.get(playerId);
        if (playerMap == null) return Collections.emptyMap();
        Map<String, ZoneRewardStat> result = new HashMap<>();
        for (Map.Entry<String, MutableStat> e : playerMap.entrySet()) {
            result.put(e.getKey(), new ZoneRewardStat(e.getValue().totalGrants, e.getValue().totalAmount));
        }
        return Collections.unmodifiableMap(result);
    }

    /** Clears all session stats for the given player (e.g. on quit). */
    public static void clearPlayer(UUID playerId) {
        stats.remove(playerId);
    }

    /** Clears ALL session stats (e.g. on plugin reload/disable). */
    public static void clearAll() {
        stats.clear();
    }

    /**
     * Returns the total number of reward grants a player has received across
     * ALL zones in the current session.
     */
    public static long getTotalGrantsAllZones(UUID playerId) {
        if (playerId == null) return 0;
        Map<String, MutableStat> playerMap = stats.get(playerId);
        if (playerMap == null) return 0;
        long total = 0;
        for (MutableStat s : playerMap.values()) total += s.totalGrants;
        return total;
    }

    /**
     * Returns the total amount earned by a player across all zones in the
     * current session.
     */
    public static double getTotalAmountAllZones(UUID playerId) {
        if (playerId == null) return 0;
        Map<String, MutableStat> playerMap = stats.get(playerId);
        if (playerMap == null) return 0;
        double total = 0;
        for (MutableStat s : playerMap.values()) total += s.totalAmount;
        return total;
    }
}
